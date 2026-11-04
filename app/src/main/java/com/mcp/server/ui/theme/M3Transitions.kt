package com.mcp.server.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mcp.server.R

/**
 * 跨 Activity 的 Shared Axis X 转场：进入页从右侧滑入淡入，退出页左移淡出。
 *
 * ⚠️ 平台限制（如实标注）：这是 **系统窗口级补间动画**，不是 MotionScheme 的
 * spring token。Compose 的 spring 只作用于进程内的组合，Activity 切换由系统
 * WindowManager 执行，无法读取 MaterialTheme.motionScheme。
 * 这里用 fast_out_slow_in 近似 expressive scheme 的 ease-in-out 收尾手感；
 * 页内转场（AnimatedContent）才是真正走 spring token 的部分。
 *
 * 若要 100% 由 spring token 驱动，需把四个子页面收进单 Activity 的 Compose 导航图，
 * 属于架构级改造，本轮未做。
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
