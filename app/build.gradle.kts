import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 版本号 / 版本名不再硬编码：默认从 Git 历史自动推导，
// CI 可用 -PappVersionCode=... -PappVersionName=... 覆盖（见 .github/scripts/version.sh）。
fun gitValue(vararg args: String): String = runCatching {
    val proc = ProcessBuilder("git", *args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val out = proc.inputStream.bufferedReader().use { it.readText() }.trim()
    if (proc.waitFor() == 0) out else ""
}.getOrDefault("")

val commitCount = gitValue("rev-list", "--count", "HEAD").toIntOrNull() ?: 1
val latestReleaseTag = gitValue("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
val autoVersionName = latestReleaseTag.removePrefix("v").ifEmpty { "0.${commitCount}.0" }
val appVersionCode = project.findProperty("appVersionCode")?.toString()?.toIntOrNull() ?: commitCount
val appVersionName = project.findProperty("appVersionName")?.toString()?.takeIf { it.isNotBlank() } ?: autoVersionName

android {
    namespace = "com.mcp.server"
    // SDK 37 基线：aapt 会据此在合并清单写入 compileSdkVersion=37 /
    // platformBuildVersionCode=37，构建脚本里有 aapt dump badging 的硬断言校验
    compileSdk = 37

    defaultConfig {
        applicationId = "com.mcp.server"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        release {
            // R8 代码压缩/优化/混淆 + 移除未使用资源，减小 Release APK 体积
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // AGP 9 移除了 android.kotlinOptions，改用 kotlin{} 扩展设置 JVM 目标
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    // 显式覆盖 BOM 的 material3 1.4.0：M3 Expressive 全套组件在 1.5.0 线
    implementation("androidx.compose.material3:material3:1.5.0-beta02")
    implementation("androidx.activity:activity-compose:1.9.1")
    // 单 Activity 导航：跨页转场走 MotionScheme 的 spring token
    implementation("androidx.navigation:navigation-compose:2.10.2")
    // 预测性返回走 navigationevent 通道；锁定探针验证过的 1.1.2（Gradle 实际解析版本）
    implementation("androidx.navigationevent:navigationevent-compose:1.1.2")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    // 悬浮窗 ComposeView 脱离 Activity 运行所需的 owner 支持
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.11.0")
    implementation("androidx.savedstate:savedstate:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.code.gson:gson:2.11.0")
    // Shizuku：以 shell 权限执行系统命令
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    // QuickJS：mcp_javascript 内置 JavaScript 引擎（ES2020）
    implementation("io.github.taoweiji.quickjs:quickjs-android:1.4.6")
}
