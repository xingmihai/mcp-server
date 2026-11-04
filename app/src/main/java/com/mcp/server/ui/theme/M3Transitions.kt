package com.mcp.server.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.mcp.server.R

private const val CLOSE_ENTER = R.anim.m3_shared_axis_close_enter
private const val CLOSE_EXIT = R.anim.m3_shared_axis_close_exit

/**
 * 跨 Activity 的 Shared Axis X 转场（进入）：右侧滑入淡入 / 左移淡出。
 *
 * ⚠️ 平台限制：这是**系统窗口级补间动画**，不是 MotionScheme 的 spring token。
 * Compose 的 spring 只作用于进程内组合，Activity 切换由 WindowManager 执行，
 * 读不到 MaterialTheme.motionScheme。这里用 fast_out_slow_in 近似 expressive 的收尾手感；
 * 页内转场（AnimatedContent）才是真正走 spring token 的部分。
 */
fun Context.m3StartActivity(intent: Intent) {
    startActivity(intent)
    if (this is Activity) {
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_OPEN,
                R.anim.m3_shared_axis_enter,
                R.anim.m3_shared_axis_exit,
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.m3_shared_axis_enter, R.anim.m3_shared_axis_exit)
        }
    }
}

/**
 * 统一的返回入口：TopAppBar 返回按钮、三键返回键、预测性返回手势提交都走这里。
 *
 * 补上了上一轮只做 OPEN 没做 CLOSE 的缺口——现在返回是 OPEN 的严格镜像：
 * 上一个页面从左 25% 回到 0 并淡入，当前页右移 100% 并淡出。
 */
fun Activity.m3Finish() {
    if (Build.VERSION.SDK_INT >= 34) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_CLOSE,
            CLOSE_ENTER,
            CLOSE_EXIT,
        )
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(CLOSE_ENTER, CLOSE_EXIT)
    }
    finish()
}

/**
 * 为**子 Activity** 接入预测性返回（Predictive Back）+ 对称 CLOSE 转场。
 *
 * 必须在 `setContent` **之前**调用：Compose 的 BackHandler 是后注册的，
 * 而 OnBackPressedDispatcher 后注册优先，所以抽屉 / BottomSheet / Dialog
 * 的返回消费不会被这个回调抢走。
 *
 * 两条路径都覆盖：
 * - API 34+ 侧滑手势：用 Progress API 驱动当前页跟手预览（右移 / 缩小 / 淡出），
 *   取消时回弹复位，提交时复位并调 [m3Finish]
 * - 三键导航 / 返回按钮 / API 33：走预注册的 CLOSE override 转场
 *
 * ⚠️ 如实说明：注册了 enabled=true 的回调后，系统会认为应用接管了返回，
 * **跳过系统默认的跨 Activity 预览动画**，改由这里的 progress 动画代替——
 * 这正是官方「自定义跨 Activity 动画」的做法，不是回退。
 * 根 Activity（MainActivity）不要调用本函数，以保留系统的 back-to-home 预览。
 */
fun ComponentActivity.m3SetupExpressiveBack() {
    // 预注册 CLOSE 转场，覆盖所有非手势路径
    if (Build.VERSION.SDK_INT >= 34) {
        overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_CLOSE,
            CLOSE_ENTER,
            CLOSE_EXIT,
        )
    }

    val decor = window.decorView
    var inGesture = false

    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            // 框架只在 API 34+ 派发 Progress 回调
            if (Build.VERSION.SDK_INT < 34) return
            inGesture = true
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) {
            if (!inGesture || Build.VERSION.SDK_INT < 34) return
            val p = backEvent.progress.coerceIn(0f, 1f)
            decor.translationX = p * decor.width * 0.25f
            val scale = 1f - p * 0.06f
            decor.scaleX = scale
            decor.scaleY = scale
            decor.alpha = 1f - p * 0.22f
        }

        override fun handleOnBackCancelled() {
            if (!inGesture) return
            inGesture = false
            decor.animate()
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(180L)
                .start()
        }

        override fun handleOnBackPressed() {
            inGesture = false
            decor.animate().cancel()
            decor.translationX = 0f
            decor.scaleX = 1f
            decor.scaleY = 1f
            decor.alpha = 1f
            m3Finish()
        }
    })
}
