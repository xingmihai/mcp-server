package com.mcp.server.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * M3 Motion 规范的统一入口。
 *
 * 应用里不再手写 tween(300) / spring(dampingRatio=...)，所有动画规格都从
 * MaterialTheme.motionScheme 读取（主题里已设为 MotionScheme.expressive()）：
 * - effects spec：颜色 / alpha 这类不改变边界的动画
 * - spatial spec：位移 / 尺寸 / 形状这类改变边界的动画
 * - fast / default / slow：按 M3 的空间/时间语义分级
 */
object McpMotion

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3DefaultEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3DefaultSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultSpatialSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3FastEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastEffectsSpec()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> m3SlowSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.slowSpatialSpec()
