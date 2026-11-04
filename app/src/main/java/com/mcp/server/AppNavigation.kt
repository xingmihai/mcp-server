package com.mcp.server

import android.net.Uri
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mcp.server.ui.theme.m3DefaultEffectsSpec
import com.mcp.server.ui.theme.m3DefaultSpatialSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * 单 Activity 导航图：把原来的 5 个子 Activity 收进同一个 Compose 导航图。
 *
 * 这是「跨 Activity 转场走 spring token」的唯一可行路径：
 * spring token 只存在于进程内的 Compose 组合树，Activity 窗口切换由 WindowManager
 * 执行、读不到 MaterialTheme。收进导航图后，所有转场都由 NavHost 的
 * enter/exit/popEnter/popExit 驱动，规格直接取 MotionScheme 的 spring token。
 *
 * 用字符串路由而非 @Serializable route，避免为此引入 kotlinx-serialization 插件。
 */
object Routes {
    const val HOME = "home"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
    const val BRIDGE = "bridge"
    const val BRIDGE_TOOLS = "bridgeTools/{bridgeId}"

    /** bridgeId 走路径参数，编码后拼接，避免特殊字符破坏路由 */
    fun bridgeTools(id: String): String = "bridgeTools/${Uri.encode(id)}"
}

@Composable
fun AppNavHost(
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onClearLogs: () -> Unit,
    showStorageDialog: Boolean,
    onStorageDialogDismiss: () -> Unit,
    onRequestStorage: () -> Unit,
) {
    val navController = rememberNavController()

    // transition lambda 不是 Composable 作用域，先在组合里取好 spring token 再捕获。
    // 这是真 spring token（读的是 MotionScheme.expressive()），不是窗口级补间。
    val spatialSpec = m3DefaultSpatialSpec<IntOffset>()
    val effectsSpec = m3DefaultEffectsSpec<Float>()

    val entry by navController.currentBackStackEntryAsState()
    val canGoBack = entry?.destination?.route != Routes.HOME
    var backProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier.graphicsLayer {
            // 预测性返回跟手预览：右移 25% / 缩小 6% / 淡出 22%
            translationX = backProgress * size.width * 0.25f
            val scale = 1f - backProgress * 0.06f
            scaleX = scale
            scaleY = scale
            alpha = 1f - backProgress * 0.22f
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Shared Axis X 进入：新页右侧全宽滑入 + 淡入
            enterTransition = {
                slideInHorizontally(animationSpec = spatialSpec) { fullWidth -> fullWidth } +
                    fadeIn(animationSpec = effectsSpec)
            },
            // 旧页左移 1/4 + 淡出
            exitTransition = {
                slideOutHorizontally(animationSpec = spatialSpec) { fullWidth -> -fullWidth / 4 } +
                    fadeOut(animationSpec = effectsSpec)
            },
            // 返回：上一页从左 1/4 回到 0 + 淡入
            popEnterTransition = {
                slideInHorizontally(animationSpec = spatialSpec) { fullWidth -> -fullWidth / 4 } +
                    fadeIn(animationSpec = effectsSpec)
            },
            // 当前页右移全宽 + 淡出
            popExitTransition = {
                slideOutHorizontally(animationSpec = spatialSpec) { fullWidth -> fullWidth } +
                    fadeOut(animationSpec = effectsSpec)
            },
        ) {
            composable(Routes.HOME) {
                MainScreen(
                    onStartServer = onStartServer,
                    onStopServer = onStopServer,
                    onOpenTools = { navController.navigate(Routes.TOOLS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onOpenBridge = { navController.navigate(Routes.BRIDGE) },
                    onClearLogs = onClearLogs,
                    showStorageDialog = showStorageDialog,
                    onStorageDialogDismiss = onStorageDialogDismiss,
                    onRequestStorage = onRequestStorage,
                )
            }
            composable(Routes.TOOLS) {
                ToolsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(onBack = { navController.popBackStack() })
            }
            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BRIDGE) {
                BridgeScreen(
                    onBack = { navController.popBackStack() },
                    onManageTools = { id -> navController.navigate(Routes.bridgeTools(id)) },
                )
            }
            composable(
                route = Routes.BRIDGE_TOOLS,
                arguments = listOf(navArgument("bridgeId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("bridgeId").orEmpty()
                BridgeToolsScreen(
                    bridgeId = id,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }

    // 放在 NavHost 之后组合 → 后注册优先，由这里统一消费返回。
    // enabled=false（主页）时交回系统，保留 back-to-home 预览。
    // 提交时显式 popBackStack，不依赖 NavHost 内部回调，避免双重消费。
    PredictiveBackHandler(enabled = canGoBack) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { event ->
                backProgress = event.progress.coerceIn(0f, 1f)
            }
            // 手势提交：复位预览并出栈
            backProgress = 0f
            navController.popBackStack()
        } catch (_: CancellationException) {
            // 手势取消：回弹复位，栈不动
            backProgress = 0f
        }
    }
}
