package com.mcp.server.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mcp.server.R

/**
 * M3 Shared Axis X（水平共享轴）转场：进入页从右侧滑入并淡入，退出页左移并淡出。
 * 用于「主页 → 设置 / 工具 / 桥接 / 关于」这一组同级页面跳转。
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
