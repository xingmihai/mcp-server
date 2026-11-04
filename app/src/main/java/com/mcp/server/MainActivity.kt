package com.mcp.server

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.mcp.server.server.LogStore
import com.mcp.server.server.McpServerService
import com.mcp.server.ui.theme.McpTheme

/**
 * 唯一的 Activity。原先的 Tools / Settings / About / Bridge / BridgeTools
 * 已收进 [AppNavHost] 的 Compose 导航图，跨页转场因此能走 MotionScheme 的 spring token。
 *
 * 保留 singleTask：通知 PendingIntent、悬浮窗点击、BOOT 后服务通知都指向这里，
 * singleTask 保证不会叠出多个主页实例。
 */
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* 结果不阻塞任何流程 */ }

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                com.mcp.server.server.Settings.setWorkspaceUri(this, uri.toString())
                val realPath = com.mcp.server.tools.Workspace.pathFromTreeUri(this, uri)
                if (realPath != null) {
                    com.mcp.server.server.Settings.setWorkspace(this, realPath)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("mcp_server_settings", MODE_PRIVATE)
        // 首次运行且还没有选择工作区文件夹（SAF）时，引导选择
        val firstRun = !prefs.getBoolean("storage_permission_dialog_shown", false)
        val noWorkspace = com.mcp.server.server.Settings.workspaceUri(this).isNullOrEmpty()
        val needStorage = firstRun && noWorkspace
        setContent {
            McpTheme {
                AppNavHost(
                    onStartServer = { startServer() },
                    onStopServer = { stopServer() },
                    onClearLogs = { LogStore.clear() },
                    showStorageDialog = firstRun && needStorage,
                    onStorageDialogDismiss = {
                        prefs.edit().putBoolean("storage_permission_dialog_shown", true).apply()
                    },
                    onRequestStorage = {
                        prefs.edit().putBoolean("storage_permission_dialog_shown", true).apply()
                        // 直接打开系统文件夹选择器选择工作区（SAF 方式，无需任何权限）
                        folderPicker.launch(null)
                    },
                )
            }
        }
        // 悬浮窗开关已开启且已授权时，重新打开应用自动恢复悬浮窗
        restoreFloatWindowIfNeeded()
    }

    private fun restoreFloatWindowIfNeeded() {
        try {
            if (com.mcp.server.server.Settings.floatWindowEnabled(this) &&
                (Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this)) &&
                !com.mcp.server.server.FloatWindowService.running) {
                com.mcp.server.server.FloatWindowService.start(this)
            }
        } catch (_: Exception) {
        }
    }

    private fun startServer() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            // 前台服务通知需要通知权限，先请求
            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
        } else {
            startForegroundServer()
        }
    }

    private fun startForegroundServer() {
        val intent = Intent(this, McpServerService::class.java)
            .setAction(McpServerService.ACTION_START)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopServer() {
        val intent = Intent(this, McpServerService::class.java)
            .setAction(McpServerService.ACTION_STOP)
        startService(intent)
    }
}
