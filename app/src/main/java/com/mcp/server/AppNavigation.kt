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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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

/**
 * 单个目的地的预测性返回帧。
 *
 * 刻意把 [PredictiveBackHandler] 放在 content **之前**组合：Compose 的 effect 按组合
 * 顺序注册，而 OnBackPressedDispatcher 后注册优先。于是优先级变成
 * 「NavHost 回调 < 页面级返回 < 页面内 Dialog / BackHandler」——
 * 工具页详情弹窗、桥接编辑/删除弹窗的返回仍能先关弹窗，不会被页面级返回抢走。
 *
 * 主页不使用本帧，保留系统的 back-to-home 预览。
 */
@Composable
private fun ExpressiveBackFrame(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    var backProgress by remember { mutableFloatStateOf(0f) }

    PredictiveBackHandler(enabled = true) { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { event ->
                // 跟手预览：右移 25% / 缩小 6% / 淡出 22%
                backProgress = event.progress.coerceIn(0f, 1f)
            }
            // 手势提交：复位并出栈
            backProgress = 0f
            onBack()
        } catch (_: CancellationException) {
            // 手势取消：回弹复位，栈不动
            backProgress = 0f
        }
    }

    Box(
        modifier = Modifier.graphicsLayer {
            translationX = backProgress * size.width * 0.25f
            val scale = 1f - backProgress * 0.06f
            scaleX = scale
            scaleY = scale
            alpha = 1f - backProgress * 0.22f
        },
    ) {
        content()
    }
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

    val popBack: () -> Unit = { navController.popBackStack() }

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
            ExpressiveBackFrame(onBack = popBack) { ToolsScreen(onBack = popBack) }
        }
        composable(Routes.SETTINGS) {
            ExpressiveBackFrame(onBack = popBack) { SettingsRoute(onBack = popBack) }
        }
        composable(Routes.ABOUT) {
            ExpressiveBackFrame(onBack = popBack) { AboutScreen(onBack = popBack) }
        }
        composable(Routes.BRIDGE) {
            ExpressiveBackFrame(onBack = popBack) {
                BridgeScreen(
                    onBack = popBack,
                    onManageTools = { id -> navController.navigate(Routes.bridgeTools(id)) },
                )
            }
        }
        composable(
            route = Routes.BRIDGE_TOOLS,
            arguments = listOf(navArgument("bridgeId") { type = NavType.StringType }),
        ) { backStackEntry ->
            // Uri.encode 写入路径；显式 decode 兜底一次（对字母数字 id 幂等）
            val raw = backStackEntry.arguments?.getString("bridgeId").orEmpty()
            val id = Uri.decode(raw)
            ExpressiveBackFrame(onBack = popBack) {
                BridgeToolsScreen(bridgeId = id, onBack = popBack)
            }
        }
    }
}
