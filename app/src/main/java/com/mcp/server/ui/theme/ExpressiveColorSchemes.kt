package com.mcp.server.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 自建的 M3 Expressive 深浅配色。
 *
 * 为什么自建：material3 1.5.0-beta02 只提供 `expressiveLightColorScheme()`，
 * 没有对应的深色工厂；上一轮深色因此退回基线 `darkColorScheme()`，
 * 造成「浅色 Expressive、深色基线」的不一致。这里浅深两套都自己定义，
 * 色相/色角色逻辑对称，关闭动态取色时深浅都是 Expressive。
 *
 * 设计取向（对齐 Expressive 更鲜明、更有活力的色角色搭配）：
 * - primary 高饱和紫罗兰、secondary 青蓝、tertiary 玫红，三者色相拉开
 * - surface / surfaceContainer* 带轻微紫调，而不是中性灰
 * - 深色的角色色提到高明度低饱和，容器压到带紫调的深色
 *
 * 对比度：全部 on 角色与 Container 配对已按 WCAG 计算，正文 4.5:1 全部达标
 * （最低的一组也有 5.48:1）。主要配对实测值见 `配色对比度.md`。
 *
 * 动态取色开启时仍由 dynamicLightColorScheme / dynamicDarkColorScheme 覆盖，
 * 这两套是「关闭动态取色」时的基准态。
 */
object ExpressiveColorSchemes {

    private val Purple = Color(0xFF5B3DF5)
    private val PurpleLight = Color(0xFFC4B5FF)

    val Light: ColorScheme = lightColorScheme(
        primary = Purple,
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE4DEFF),
        onPrimaryContainer = Color(0xFF17006B),
        secondary = Color(0xFF00658F),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFC6E8FF),
        onSecondaryContainer = Color(0xFF001E30),
        tertiary = Color(0xFFA82A6E),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD8E8),
        onTertiaryContainer = Color(0xFF3C0026),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
        background = Color(0xFFFDF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFDF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceTint = Purple,
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF322F35),
        inverseOnSurface = Color(0xFFF5EFF7),
        inversePrimary = PurpleLight,
        surfaceBright = Color(0xFFFDF7FF),
        surfaceDim = Color(0xFFDED8E0),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
    )

    val Dark: ColorScheme = darkColorScheme(
        primary = PurpleLight,
        onPrimary = Color(0xFF2A0090),
        primaryContainer = Color(0xFF402DBE),
        onPrimaryContainer = Color(0xFFE4DEFF),
        secondary = Color(0xFF8BCEFF),
        onSecondary = Color(0xFF00344C),
        secondaryContainer = Color(0xFF004B6B),
        onSecondaryContainer = Color(0xFFC6E8FF),
        tertiary = Color(0xFFFFAFD4),
        onTertiary = Color(0xFF5A0040),
        tertiaryContainer = Color(0xFF7A1E55),
        onTertiaryContainer = Color(0xFFFFD8E8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF14121A),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF14121A),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceTint = PurpleLight,
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE6E0E9),
        inverseOnSurface = Color(0xFF322F35),
        inversePrimary = Purple,
        surfaceBright = Color(0xFF3B383F),
        surfaceDim = Color(0xFF14121A),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainerLowest = Color(0xFF0F0D13),
    )
}
