package com.mcp.server.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/**
 * M3 Motion physics system 的 spring token 入口。
 *
 * 原文：每个 scheme 有 3 个速度（fast / default / slow）× 2 类运动（spatial / effects）
 * = 6 个 token，token 名形如 `md.sys.motion.spring.fast.spatial`。
 * scheme 本身不在 token 名里，由产品级主题切换，因此换 scheme 不用改调用点。
 *
 * - spatial：位移 / 旋转 / 尺寸 / 圆角（rounded corners）
 * - effects：颜色 / opacity（不应有 overshoot）
 *
 * 全局 scheme 由 [McpTheme] 统一设为 MotionScheme.expressive()（按 M3 首页的
 * Expressive 方向，带 overshoot）；这里只负责读 token，不硬编码 stiffness / damping / duration。
 */
object McpMotionTokens {
    const val FAST_SPATIAL = "md.sys.motion.spring.fast.spatial"
    const val DEFAULT_SPATIAL = "md.sys.motion.spring.default.spatial"
    const val SLOW_SPATIAL = "md.sys.motion.spring.slow.spatial"
    const val FAST_EFFECTS = "md.sys.motion.spring.fast.effects"
    const val DEFAULT_EFFECTS = "md.sys.motion.spring.default.effects"
    const val SLOW_EFFECTS = "md.sys.motion.spring.slow.effects"
}

// ===== 6 个 spring token（全部从主题 motionScheme 读取）=====

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3FastSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3DefaultSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3SlowSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.slowSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3FastEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3DefaultEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3SlowEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.slowEffectsSpec()

/**
 * 自定义 MotionScheme（原文：scheme 不够用时可以自己创建，且仍能享受主题化）。
 *
 * 用 spring 的三属性（stiffness / damping / initial velocity）参数化 6 个 token；
 * 默认 dampingRatio = 1f（无回弹）；若要做 hero 时刻可传 0.8f 之类带回弹的值。
 * 当前全局用内置 [MotionScheme.expressive]，此工厂供局部元素做进阶定制。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun mcpMotionScheme(
    fastSpatialStiffness: Float = 700f,
    defaultSpatialStiffness: Float = 400f,
    slowSpatialStiffness: Float = 200f,
    fastEffectsStiffness: Float = 700f,
    defaultEffectsStiffness: Float = 400f,
    slowEffectsStiffness: Float = 200f,
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
): MotionScheme = object : MotionScheme {
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, fastSpatialStiffness)

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, defaultSpatialStiffness)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, slowSpatialStiffness)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, fastEffectsStiffness)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, defaultEffectsStiffness)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio, slowEffectsStiffness)
}

/**
 * 局部切换 scheme（原文：多数页面用一个 scheme，某些元素可换另一个）。
 *
 * 通过嵌套 [MaterialExpressiveTheme] 覆盖 motionScheme，显式回填当前配色/形状/排版，
 * 等价于 override 该 CompositionLocal，但不依赖内部类名。
 * 本项目全局用 expressive，默认不在任何页面局部切换；保留能力备用。
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProvideMcpMotionScheme(
    scheme: MotionScheme,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = MaterialTheme.colorScheme,
        motionScheme = scheme,
        shapes = MaterialTheme.shapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
