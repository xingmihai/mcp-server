#!/usr/bin/env bash
# 从 Git 历史推导版本号，输出到 $GITHUB_OUTPUT，仓库中不写死任何版本数字
set -euo pipefail

# versionCode：提交总数，随每次提交自然递增（checkout 需要 fetch-depth: 0）
CODE=$(git rev-list --count HEAD)

# 最近一个正式版 tag（形如 v1.2.3）
LAST_TAG=$(git describe --tags --abbrev=0 --match 'v[0-9]*' 2>/dev/null || true)
BASE=${LAST_TAG#v}
if [[ -z "$BASE" ]]; then
  # 仓库还没有正式版 tag 时，用提交数生成一个可用版本
  BASE="0.${CODE}.0"
fi

# 下一个补丁版本，用于预发行与「留空版本号」的手动正式发行
IFS='.' read -r MAJOR MINOR PATCH <<< "${BASE%%-*}"
MAJOR=$(tr -cd '0-9' <<< "${MAJOR:-0}"); MAJOR=${MAJOR:-0}
MINOR=$(tr -cd '0-9' <<< "${MINOR:-0}"); MINOR=${MINOR:-0}
PATCH=$(tr -cd '0-9' <<< "${PATCH:-0}"); PATCH=${PATCH:-0}
NEXT="${MAJOR}.${MINOR}.$((PATCH + 1))"

{
  echo "VERSION_CODE=$CODE"
  echo "BASE_VERSION=$BASE"
  echo "NEXT_VERSION=$NEXT"
} >> "$GITHUB_OUTPUT"

echo "versionCode=$CODE  base=$BASE  next=$NEXT"
