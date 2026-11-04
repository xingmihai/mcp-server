package com.mcp.server.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.ViewTreeSavedStateRegistryOwner
import com.mcp.server.MainActivity
import com.mcp.server.ui.theme.McpTheme
import kotlinx.coroutines.delay

/**
 * 悬浮窗：用 ComposeView 渲染，直接复用全局 M3 Expressive 主题（配色 / 形状 / 动效）。
 * 胶囊小窗只显示状态呼吸灯 + 运行中/已停止，可拖动，点击回到主界面。
 * 作为前台服务运行（低优先级通知），需要 SYSTEM_ALERT_WINDOW 权限。
 *
 * ComposeView 在 WindowManager 中需要 Lifecycle / ViewModelStore / SavedStateRegistry 三个 owner，
 * 由本 Service 一并实现。
 */
class FloatWindowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val ACTION_START = "com.mcp.server.FLOAT_START"
        const val ACTION_STOP = "com.mcp.server.FLOAT_STOP"
        const val CHANNEL_ID = "mcp_float"
        private const val NOTIFICATION_ID = 2

        @Volatile
        var running = false
            private set

        fun start(context: Context) {
            val i = Intent(context, FloatWindowService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, FloatWindowService::class.java).setAction(ACTION_STOP))
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        savedStateController.performRestore(null)
        // ComposeView 需要生命周期至少进入 STARTED 才会开始组合
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                if (composeView == null) showFloat()
                START_STICKY
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "悬浮窗", NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "悬浮窗服务运行状态"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("MCP 悬浮窗运行中")
            .setContentText("悬浮窗已开启")
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            val type = if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 200
            }

            val view = ComposeView(this).apply {
                // 挂上三个 owner，ComposeView 才能脱离 Activity 独立工作
                ViewTreeLifecycleOwner.set(this, this@FloatWindowService)
                ViewTreeViewModelStoreOwner.set(this, this@FloatWindowService)
                ViewTreeSavedStateRegistryOwner.set(this, this@FloatWindowService)
                setContent {
                    McpTheme {
                        FloatContent(
                            onOpenMain = {
                                startActivity(
                                    Intent(this@FloatWindowService, MainActivity::class.java)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            },
                            onDrag = { dx, dy ->
                                params.x += dx
                                params.y += dy
                                try {
                                    windowManager?.updateViewLayout(composeView, params)
                                } catch (_: Exception) {
                                }
                            },
                        )
                    }
                }
            }

            windowManager?.addView(view, params)
            composeView = view
            running = true
        } catch (_: Exception) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        running = false
        try {
            composeView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        composeView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/** 悬浮窗主体：状态呼吸灯 + 运行中/已停止 */
@Composable
private fun FloatContent(
    onOpenMain: () -> Unit,
    onDrag: (Int, Int) -> Unit,
) {
    var active by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            active = McpServerService.server?.isRunning == true
            delay(2000)
        }
    }

    var pulseOn by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            pulseOn = !pulseOn
            delay(900)
        }
    }
    val dotAlpha by animateFloatAsState(
        targetValue = if (active && pulseOn) 1f else if (active) 0.42f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "floatDotPulse",
    )

    Surface(
        shape = MaterialTheme.shapes.largeIncreased,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x.toInt(), dragAmount.y.toInt())
                }
            }
            .clickable(onClick = onOpenMain),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = (if (active) Color(0xFF2E7D32) else Color(0xFFB3261E))
                            .copy(alpha = dotAlpha),
                        shape = CircleShape,
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (active) "运行中" else "已停止",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
