package com.mcp.server.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mcp.server.R

/**
 * Material Symbols（Rounded）图标名 → Unicode codepoint。
 *
 * 字体来自 Google Material Symbols 变量字体，已用 fonttools 裁剪到本项目实际用到的
 * 24 个字形：变量版 58 KB（保留 FILL / GRAD / opsz / wght 四轴），
 * Android O 以下用的静态回退版 6 KB。远小于完整字体的 15 MB。
 */
object MaterialSymbols {
    const val Add = "\uE145"
    const val ArrowBack = "\uE5C4"
    const val CheckCircle = "\uF0BE"
    const val Circle = "\uEF4A"
    const val Code = "\uE86F"
    const val ContentCopy = "\uE14D"
    const val Delete = "\uE92E"
    const val Dns = "\uE875"
    const val Edit = "\uF097"
    const val Folder = "\uE2C7"
    const val Gavel = "\uE90E"
    const val Home = "\uE9B2"
    const val Info = "\uE88E"
    const val Link = "\uE250"
    const val Menu = "\uE5D2"
    const val PlayArrow = "\uE037"
    const val Power = "\uE63C"
    const val Public = "\uE80B"
    const val RadioButtonUnchecked = "\uE836"
    const val Refresh = "\uE5D5"
    const val Settings = "\uE8B8"
    const val Stop = "\uE047"
    const val Tune = "\uE429"
    const val Wallpaper = "\uE1BC"
}

/**
 * 渲染一个 Material Symbol。
 *
 * 通过变量轴表达状态，而不是换一张位图：
 * - [filled] 控制 FILL 轴（0=描边 / 1=填充），用于选中态
 * - [weight] 控制 wght 轴（100~700）
 * - [grade] 控制 GRAD 轴（-50~200），可在不改排版的前提下强调/弱化
 * - opsz 固定 24，与 24dp 视觉尺寸对齐
 *
 * 图标尺寸按 dp 固定（对系统字体缩放做反向补偿），不会随「字体大小」设置变大。
 * RTL 下 ArrowBack 自动镜像，对齐原 AutoMirrored 语义。
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun MaterialSymbol(
    symbol: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    filled: Boolean = false,
    weight: Int = 400,
    grade: Int = 0,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // API 26 以下不支持可变字体，回退到静态实例
    val fontFamily = remember(filled, weight, grade) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            FontFamily(
                Font(
                    R.font.material_symbols_rounded,
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("FILL", if (filled) 1f else 0f),
                        FontVariation.Setting("wght", weight.toFloat().coerceIn(100f, 700f)),
                        FontVariation.Setting("GRAD", grade.toFloat().coerceIn(-50f, 200f)),
                        FontVariation.Setting("opsz", 24f),
                    ),
                )
            )
        } else {
            FontFamily(Font(R.font.material_symbols_rounded_static))
        }
    }

    // 反向补偿 fontScale，保证图标是固定 dp 而不是 sp
    val iconSize = (size.value / density.fontScale).sp
    val mirror = rtl && symbol == MaterialSymbols.ArrowBack

    BasicText(
        text = symbol,
        modifier = modifier
            .size(size)
            .graphicsLayer { if (mirror) scaleX = -1f }
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics { }
                }
            ),
        style = TextStyle(
            color = tint,
            fontFamily = fontFamily,
            fontSize = iconSize,
            lineHeight = iconSize,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
        softWrap = false,
    )
}
