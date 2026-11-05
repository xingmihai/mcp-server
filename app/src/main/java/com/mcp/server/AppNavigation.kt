package com.mcp.server

import android.net.Uri
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.mcp.server.ui.theme.m3DefaultEffectsSpec
import com.mcp.server.ui.theme.m3DefaultSpatialSpec

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
 * ⚠️ 为什么不用 `PredictiveBackHandler`：Navigation 2.10.x 已经迁到
 * `androidx.navigationevent` 的 `NavigationEventDispatcher` 通道；`PredictiveBackHandler`
 * 走的是旧的 `OnBackPressedDispatcher`，两条链并存时 progress 派发不到我们的回调。
 * 更糟的是：注册 enabled=true 的返回回调会让系统跳过预测性返回动画，
 * 结果就是「手势一松直接 pop」，即用户反馈的「不跟手」。
 *
 * 这里改用同通道的 [NavigationBackHandler] + [rememberNavigationEventState]：
 * - `state.transitionState` 是 Compose snapshot state，由 dispatcher 直接写入，
 *   重组即刷新，不再依赖协程 Flow 投递
 * - `onBackCompleted` 只执行一次 [onBack]，不会重复 pop
 * - progress 用 `InProgress.latestEvent.progress`，手势中用 `snap()` 瞬时跟随（100% 跟手），
 *   取消 / 提交后用 spring token 平滑回弹
 *
 * [NavigationBackHandler] 放在 content **之前**组合，保证页面内 Dialog / BackHandler
 * （后注册）仍优先消费返回；主页不使用本帧，保留系统 back-to-home 预览。
 */
@Composable
private fun ExpressiveBackFrame(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val navEventState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None,
    )
    val transitionState = navEventState.transitionState
    val inProgress = transitionState is NavigationEventTransitionState.InProgress
    val rawProgress = (transitionState as? NavigationEventTransitionState.InProgress)
        ?.latestEvent?.progress?.coerceIn(0f, 1f) ?: 0f

    // 跟手阶段瞬时映射，松手/取消后走 spring token 回弹
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (inProgress) snap() else m3DefaultEffectsSpec<Float>(),
        label = "predictiveBackProgress",
    )

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = true,
        onBackCancelled = { /* transitionState 自动回 Idle，progress 由 spring 归零 */ },
        onBackCompleted = onBack,
    )

    Box(
        modifier = Modifier.graphicsLayer {
            // 跟手预览：右移 25% / 缩小 6% / 淡出 22%
            translationX = progress * size.width * 0.25f
            val scale = 1f - progress * 0.06f
            scaleX = scale
            scaleY = scale
            alpha = 1f - progress * 0.22f
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
