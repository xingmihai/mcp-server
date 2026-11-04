#!/usr/bin/env bash
# 构建 Release APK 并使用环境密钥中的 JKS 签名；两个发行工作流共用
set -euo pipefail

: "${KEYSTORE_BASE64:?缺少环境密钥 KEYSTORE_BASE64}"
: "${KEYSTORE_PASSWORD:?缺少环境密钥 KEYSTORE_PASSWORD}"
: "${KEY_ALIAS:?缺少环境密钥 KEY_ALIAS}"
: "${KEY_PASSWORD:?缺少环境密钥 KEY_PASSWORD}"
: "${OUT_APK:?缺少 OUT_APK}"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
test -n "$SDK"

KS="$RUNNER_TEMP/signing.jks"
KS_PASS="$RUNNER_TEMP/signing.ks.pass"
KEY_PASS="$RUNNER_TEMP/signing.key.pass"
ALIGNED="$RUNNER_TEMP/app-aligned.apk"
cleanup() { rm -f "$KS" "$KS_PASS" "$KEY_PASS" "$ALIGNED"; }
trap cleanup EXIT

printf '%s' "$KEYSTORE_BASE64" | base64 -d > "$KS"
printf '%s' "$KEYSTORE_PASSWORD" > "$KS_PASS"
printf '%s' "$KEY_PASSWORD" > "$KEY_PASS"
chmod 600 "$KS" "$KS_PASS" "$KEY_PASS"

# 先校验密钥库与别名，避免构建完成后才发现签名失败
keytool -list -keystore "$KS" -storepass:file "$KS_PASS" -alias "$KEY_ALIAS" > /dev/null

# 仓库中的 local.properties 指向本机 SDK，CI 上覆盖（不提交）
printf 'sdk.dir=%s\n' "$SDK" > local.properties
chmod +x gradlew
# GRADLE_ARGS 由工作流传入，如 -PappVersionCode=123 -PappVersionName=1.2.4-pre.7
# 失败时把关键错误写成一条 workflow annotation，方便从 API 快速定位
LOG="$RUNNER_TEMP/gradle-build.log"
set +e
# shellcheck disable=SC2086
./gradlew assembleRelease --no-daemon --stacktrace ${GRADLE_ARGS:-} > "$LOG" 2>&1
RC=$?
set -e
cat "$LOG"
if [ "$RC" -ne 0 ]; then
  SUMMARY=$(grep -nE '(^e: |^FAILURE:|^\* What went wrong:|^Caused by:|^Could not |^Execution failed|^\* Try:|FAILED$|requires Android Gradle|not found|Unsupported|error:|What went wrong)' "$LOG" \
    | head -120 | sed 's/%/%25/g; s/\r/%0D/g' \
    | awk 'BEGIN{s=""} {printf "%s%s", s, $0; s="%0A"} END{print ""}')
  echo "::error title=Gradle 构建失败::${SUMMARY:-请查看构建日志}"
  exit "$RC"
fi

BT=$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)
test -n "$BT"
UNSIGNED=$(find app/build/outputs/apk/release -name '*-unsigned.apk' -print -quit)
if [[ -z "$UNSIGNED" ]]; then
  UNSIGNED=$(find app/build/outputs/apk/release -name '*.apk' -print -quit)
fi
test -n "$UNSIGNED"

"$BT/zipalign" -p -f 4 "$UNSIGNED" "$ALIGNED"
mkdir -p "$(dirname "$OUT_APK")"
"$BT/apksigner" sign \
  --ks "$KS" --ks-key-alias "$KEY_ALIAS" \
  --ks-pass "file:$KS_PASS" --key-pass "file:$KEY_PASS" \
  --v1-signing-enabled true --v2-signing-enabled true \
  --out "$OUT_APK" "$ALIGNED"
"$BT/apksigner" verify --verbose "$OUT_APK"
echo "已生成：$OUT_APK"
