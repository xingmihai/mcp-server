package com.mcp.server.ui.theme

import android.graphics.Matrix
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toPath

/**
 * M3 Expressive 形状系统。
 *
 * 官方 `MaterialShapes` 共提供 **35 个** RoundedPolygon 形状；本文件把它们全部登记为令牌，
 * 再加 2 个自定义 Morph（形变）形状令牌，合计 **37 个形状令牌**。
 * 两个自定义令牌不是「官方第 36/37 个形状」，而是从官方形状组合出的可形变形状，
 * 用于展示 shape morph motion（首页点名的能力）。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object McpShapeTokens {

    data class Token(
        val name: String,
        val polygon: RoundedPolygon,
        val morphTarget: RoundedPolygon? = null,
        val custom: Boolean = false,
    )

    /** 35 个官方形状，按官方 Companion 顺序全量登记 */
    private val official: List<Token> = listOf(
        Token("Circle", MaterialShapes.Circle),
        Token("Square", MaterialShapes.Square),
        Token("Slanted", MaterialShapes.Slanted),
        Token("Arch", MaterialShapes.Arch),
        Token("Fan", MaterialShapes.Fan),
        Token("Arrow", MaterialShapes.Arrow),
        Token("SemiCircle", MaterialShapes.SemiCircle),
        Token("Oval", MaterialShapes.Oval),
        Token("Pill", MaterialShapes.Pill),
        Token("Triangle", MaterialShapes.Triangle),
        Token("Diamond", MaterialShapes.Diamond),
        Token("ClamShell", MaterialShapes.ClamShell),
        Token("Pentagon", MaterialShapes.Pentagon),
        Token("Gem", MaterialShapes.Gem),
        Token("Sunny", MaterialShapes.Sunny),
        Token("VerySunny", MaterialShapes.VerySunny),
        Token("Cookie4Sided", MaterialShapes.Cookie4Sided),
        Token("Cookie6Sided", MaterialShapes.Cookie6Sided),
        Token("Cookie7Sided", MaterialShapes.Cookie7Sided),
        Token("Cookie9Sided", MaterialShapes.Cookie9Sided),
        Token("Cookie12Sided", MaterialShapes.Cookie12Sided),
        Token("Ghostish", MaterialShapes.Ghostish),
        Token("Clover4Leaf", MaterialShapes.Clover4Leaf),
        Token("Clover8Leaf", MaterialShapes.Clover8Leaf),
        Token("Burst", MaterialShapes.Burst),
        Token("SoftBurst", MaterialShapes.SoftBurst),
        Token("Boom", MaterialShapes.Boom),
        Token("SoftBoom", MaterialShapes.SoftBoom),
        Token("Flower", MaterialShapes.Flower),
        Token("Puffy", MaterialShapes.Puffy),
        Token("PuffyDiamond", MaterialShapes.PuffyDiamond),
        Token("PixelCircle", MaterialShapes.PixelCircle),
        Token("PixelTriangle", MaterialShapes.PixelTriangle),
        Token("Bun", MaterialShapes.Bun),
        Token("Heart", MaterialShapes.Heart),
    )

    /** 2 个自定义形变令牌（官方形状两两组合成 Morph） */
    private val custom: List<Token> = listOf(
        Token(
            "CookieToSunny",
            MaterialShapes.Cookie4Sided,
            morphTarget = MaterialShapes.Sunny,
            custom = true,
        ),
        Token(
            "PuffyToClover",
            MaterialShapes.Puffy,
            morphTarget = MaterialShapes.Clover8Leaf,
            custom = true,
        ),
    )

    /** 37 个形状令牌：35 官方 + 2 自定义形变 */
    val ALL: List<Token> = official + custom

    val OFFICIAL_COUNT: Int get() = official.size
    val CUSTOM_COUNT: Int get() = custom.size
    val TOTAL_COUNT: Int get() = ALL.size
}

/**
 * 把 [Morph] 渲染成 Compose [Shape] 的适配器。
 *
 * `toPath(morph, progress)` 会给出归一化的 Path；这里再按目标尺寸缩放平移，
 * 生成 `Outline.Generic`，因此可以直接用在 `Modifier.clip` / `background(shape=...)`。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphShape(
    private val start: RoundedPolygon,
    private val end: RoundedPolygon,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
        }
        val path: Path = Morph(start, end).toPath(progress.coerceIn(0f, 1f))
        val bounds = path.getBounds()
        val sx = if (bounds.width > 0f) size.width / bounds.width else 1f
        val sy = if (bounds.height > 0f) size.height / bounds.height else 1f
        val matrix = Matrix().apply {
            setScale(sx, sy)
            postTranslate(-bounds.left * sx, -bounds.top * sy)
        }
        path.asAndroidPath().transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * 用 spring token 驱动的形变形状：progress 变化时形状在 start ↔ end 之间形变。
 */
@Composable
fun rememberMorphShape(
    start: RoundedPolygon,
    end: RoundedPolygon,
    targetProgress: Float,
    label: String = "shapeMorph",
): Shape {
    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = m3SlowSpatialSpec<Float>(),
        label = label,
    )
    return remember(start, end, progress) { MorphShape(start, end, progress) }
}
