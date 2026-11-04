package com.mcp.server.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.mcp.server.server.Settings

/**
 * M3 Expressive 形状系统：比基线 M3 更圆润，并启用 largeIncreased 尺寸。
 * 所有页面共用这一套 shape，卡片/按钮/对话框会自动继承。
 */
private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * 强调排版：M3 Expressive 的 Typography 除了 15 个基线角色，
 * 还为每个角色提供 `*Emphasized` 变体（更重、更紧的字重与字距）。
 * 这里把标题类角色换成强调变体，正文/标签仍用基线。
 */
private val ExpressiveTypography: Typography
    @Composable
    get() {
        val base = Typography()
        return Typography(
            displayLarge = base.displayLargeEmphasized,
            displayMedium = base.displayMediumEmphasized,
            displaySmall = base.displaySmallEmphasized,
            headlineLarge = base.headlineLargeEmphasized,
            headlineMedium = base.headlineMediumEmphasized,
            headlineSmall = base.headlineSmallEmphasized,
            titleLarge = base.titleLargeEmphasized,
            titleMedium = base.titleMediumEmphasized,
            titleSmall = base.titleSmallEmphasized,
        )
    }

/**
 * 全局 M3 Expressive 主题。
 *
 * - motionScheme = expressive()：按 M3 首页的 Expressive 方向，带 overshoot 的 spring
 * - 配色：动态取色优先；关闭时走 [ExpressiveColorSchemes] 自建的深浅两套，
 *   两者色相对称、都是 Expressive，不再出现「浅色 Expressive / 深色基线」的割裂
 * - 排版：标题类角色用 *Emphasized 强调变体
 * - 形状：8 档 Expressive 圆角
 *
 * @param dynamicColor 是否启用 Material You 动态取色；传 null 时读取用户在设置页的开关。
 *   Android 12（API 31）以下自动回退到应用自带配色。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun McpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDynamic = dynamicColor ?: Settings.dynamicColorEnabled(context)

    val colorScheme = when {
        useDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ExpressiveColorSchemes.Dark
        else -> ExpressiveColorSchemes.Light
    }

    // 统一设置状态栏 / 导航栏颜色与图标明暗，兼容深色模式
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = findActivity(view.context)?.window ?: return@SideEffect
            val bg = colorScheme.surface
            val darkIcons = bg.luminance() > 0.5f
            // API 35+ 强制 edge-to-edge：状态栏透明，显示的是 decorView 背景色，因此统一设置
            window.decorView.setBackgroundColor(bg.toArgb())
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = bg.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = darkIcons
                isAppearanceLightNavigationBars = darkIcons
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = ExpressiveTypography,
        content = content,
    )
}

/** 从可能被包装的 context 中向上查找 Activity */
private tailrec fun findActivity(context: android.content.Context): android.app.Activity? =
    when (context) {
        is android.app.Activity -> context
        is android.content.ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
