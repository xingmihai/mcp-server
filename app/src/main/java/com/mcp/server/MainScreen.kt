package com.mcp.server

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mcp.server.ui.theme.MaterialSymbol
import com.mcp.server.ui.theme.McpShapeTokens
import com.mcp.server.ui.theme.rememberMorphShape
import com.mcp.server.ui.theme.m3DefaultEffectsSpec
import com.mcp.server.ui.theme.m3DefaultSpatialSpec
import com.mcp.server.ui.theme.m3FastEffectsSpec
import com.mcp.server.ui.theme.m3FastSpatialSpec
import com.mcp.server.ui.theme.MaterialSymbols
import com.mcp.server.server.LogEntry
import com.mcp.server.server.LogStore
import com.mcp.server.server.McpServerService
import com.mcp.server.server.Net
import com.mcp.server.server.Settings
import com.mcp.server.tools.Workspace
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class StatusInfo(
    val running: Boolean = false,
    val ips: List<String> = emptyList(),
    val wifi: String? = null,
    val tunnelUrl: String? = null,
    val tunnelActive: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenBridge: () -> Unit = {},
    onClearLogs: () -> Unit = {},
    showStorageDialog: Boolean = false,
    onStorageDialogDismiss: () -> Unit = {},
    onRequestStorage: () -> Unit = {},
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0=主页 1=日志
    var status by remember { mutableStateOf(StatusInfo()) }
    var logs by remember { mutableStateOf<List<LogEntry>>(emptyList()) }
    var showStorageDialogState by remember { mutableStateOf(showStorageDialog) }
    var workspaceDisplay by remember { mutableStateOf(Settings.workspace(context)) }
    var portText by remember { mutableStateOf(Settings.port(context).toString()) }
    var showPortDialog by remember { mutableStateOf(false) }
    var showRestartHint by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var logFilter by remember { mutableIntStateOf(0) } // 0=全部 1=仅警告/错误

    // 系统文件夹选择器：选择应用允许访问的文件夹作为工作区（SAF）
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: Exception) {
            }
            // 保存 SAF URI + 解析出的真实路径（供路径校验与显示）
            Settings.setWorkspaceUri(context, uri.toString())
            val realPath = Workspace.pathFromTreeUri(context, uri)
            if (realPath != null) {
                Settings.setWorkspace(context, realPath)
                workspaceDisplay = realPath
            } else {
                workspaceDisplay = uri.toString()
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // 轮询服务器状态 + 日志（含隧道状态，动态刷新）
    LaunchedEffect(Unit) {
        while (true) {
            val srv = McpServerService.server
            status = StatusInfo(
                running = srv?.isRunning == true,
                ips = Net.ipv4Addresses(),
                wifi = Net.wifiSsid(context),
                tunnelUrl = McpServerService.tunnelUrl,
                tunnelActive = McpServerService.tunnelActive,
            )
            logs = LogStore.snapshot()
            delay(1000)
        }
    }

    val currentPort = Settings.port(context)
    // 显示真实路径（SAF 模式下也显示文件系统路径，不显示 content:// URI）
    val currentWorkspace = workspaceDisplay

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                // 侧栏头部：图标 + 应用名 + 版本号
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "应用图标",
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape,
                            )
                            .padding(10.dp),
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("MCP 服务器", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    label = { Text("工具列表") },
                    icon = { MaterialSymbol(MaterialSymbols.Tune, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenTools()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("设置") },
                    icon = { MaterialSymbol(MaterialSymbols.Settings, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenSettings()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("MCP 桥接") },
                    icon = { MaterialSymbol(MaterialSymbols.Link, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenBridge()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                NavigationDrawerItem(
                    label = { Text("关于应用") },
                    icon = { MaterialSymbol(MaterialSymbols.Info, contentDescription = null) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenAbout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (selectedTab) {
                                0 -> "MCP 服务器"
                                else -> "运行日志"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            MaterialSymbol(MaterialSymbols.Menu, contentDescription = "菜单")
                        }
                    },
                    actions = {
                        if (selectedTab == 1 && logs.isNotEmpty()) {
                            // 首页点名能力：shape-shifting ButtonGroup
                            // customItem + connected shapes + animateWidth：按压时按钮互相挤压，
                            // 圆角按 pressedShape 形变，而不是普通的一排按钮
                            val copyInteraction = remember { MutableInteractionSource() }
                            val clearInteraction = remember { MutableInteractionSource() }
                            ButtonGroup(
                                overflowIndicator = { menuState ->
                                    ButtonGroupDefaults.OverflowIndicator(menuState = menuState)
                                },
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                val text = logs.joinToString("\n") {
                                                    "${it.timeText} [${it.level}] ${it.message}"
                                                }
                                                copyText(context, text)
                                                scope.launch { snackbarHostState.showSnackbar("已复制全部日志") }
                                            },
                                            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                            modifier = Modifier.animateWidth(copyInteraction),
                                            interactionSource = copyInteraction,
                                        ) { Text("复制") }
                                    },
                                    menuContent = { },
                                )
                                customItem(
                                    buttonGroupContent = {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                onClearLogs()
                                                scope.launch { snackbarHostState.showSnackbar("日志已清空") }
                                            },
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            modifier = Modifier.animateWidth(clearInteraction),
                                            interactionSource = clearInteraction,
                                        ) { Text("清空") }
                                    },
                                    menuContent = { },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                // M3 Expressive 紧凑底栏 + 日志条数 Badge
                ShortNavigationBar {
                    ShortNavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { MaterialSymbol(MaterialSymbols.Home, contentDescription = null,
                            filled = selectedTab == 0) },
                        label = { Text("主页") },
                    )
                    ShortNavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (logs.isNotEmpty()) {
                                        Badge { Text(logs.size.coerceAtMost(99).toString()) }
                                    }
                                },
                            ) { MaterialSymbol(MaterialSymbols.Info, contentDescription = null,
                                filled = selectedTab == 1) }
                        },
                        label = { Text("日志") },
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    // 首页点名能力：Toolbar 配 FAB（vibrant 配色 + Vibrant FAB）
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = {
                            FloatingToolbarDefaults.VibrantFloatingActionButton(
                                onClick = if (status.running) onStopServer else onStartServer,
                            ) {
                                MaterialSymbol(
                                    if (status.running) MaterialSymbols.Stop else MaterialSymbols.PlayArrow,
                                    contentDescription = if (status.running) "停止服务器" else "启动服务器",
                                    filled = true,
                                )
                            }
                        },
                        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    ) {
                        IconButton(onClick = onOpenTools) {
                            MaterialSymbol(MaterialSymbols.Tune, contentDescription = "工具列表")
                        }
                        IconButton(onClick = onOpenSettings) {
                            MaterialSymbol(MaterialSymbols.Settings, contentDescription = "设置")
                        }
                        IconButton(onClick = {
                            onOpenBridge()
                        }) {
                            MaterialSymbol(MaterialSymbols.Link, contentDescription = "MCP 桥接")
                        }
                    }
                }
            },
        ) { padding ->
            // transitionSpec 不是 Composable 作用域，先从主题取好规格再传入。
            // 整页级动效用 default 档（原文：多数动效用 default，小元素才用 fast）
            val defaultEffects = m3DefaultEffectsSpec<Float>()
            val defaultSpatial = m3DefaultSpatialSpec<IntOffset>()
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (fadeIn(animationSpec = defaultEffects) +
                            slideInHorizontally(animationSpec = defaultSpatial) { it / 6 })
                            .togetherWith(
                                fadeOut(animationSpec = defaultEffects) +
                                    slideOutHorizontally(animationSpec = defaultSpatial) { -it / 6 }
                            )
                    } else {
                        (fadeIn(animationSpec = defaultEffects) +
                            slideInHorizontally(animationSpec = defaultSpatial) { -it / 6 })
                            .togetherWith(
                                fadeOut(animationSpec = defaultEffects) +
                                    slideOutHorizontally(animationSpec = defaultSpatial) { it / 6 }
                            )
                    }
                },
                label = "tabSharedAxis",
            ) { tab ->
                if (tab == 0) {
                HomeTab(
                    context = context,
                    status = status,
                    currentPort = currentPort,
                    currentWorkspace = currentWorkspace,
                    onStartServer = onStartServer,
                    onStopServer = onStopServer,
                    onPickWorkspace = { folderPicker.launch(null) },
                    onEditPort = {
                        portText = Settings.port(context).toString()
                        showPortDialog = true
                    },
                    modifier = Modifier.padding(padding),
                )
                } else {
                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        SegmentedButton(
                            selected = logFilter == 0,
                            onClick = { logFilter = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            label = { Text("全部") },
                        )
                        SegmentedButton(
                            selected = logFilter == 1,
                            onClick = { logFilter = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            label = { Text("仅警告/错误") },
                        )
                    }
                    LogTab(
                        logs = if (logFilter == 1) {
                            logs.filter { it.level == "E" || it.level == "W" }
                        } else logs,
                        modifier = Modifier.weight(1f),
                    )
                }
                }
            }
        }
    }

    // ===== 修改端口弹窗（无遮罩：Dialog 默认不绘制 dim scrim） =====
    if (showPortDialog) {
        var portError by remember { mutableStateOf<String?>(null) }
        Dialog(
            onDismissRequest = { showPortDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "修改端口",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "默认端口 1145。修改后需重启服务器才能生效。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it; portError = null },
                        label = { Text("端口号") },
                        singleLine = true,
                        isError = portError != null,
                        supportingText = { portError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showPortDialog = false }) { Text("取消") }
                        Spacer(Modifier.width(4.dp))
                        TextButton(onClick = {
                            val p = portText.trim().toIntOrNull()
                            if (p == null || p !in 1..65535) {
                                portError = "请输入 1-65535 之间的端口号"
                                return@TextButton
                            }
                            Settings.setPort(context, p)
                            showPortDialog = false
                            // 服务器运行中提示重启
                            if (McpServerService.server?.isRunning == true) {
                                showRestartHint = true
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("端口已保存为 $p，启动服务器后生效")
                                }
                            }
                        }) { Text("保存") }
                    }
                }
            }
        }
    }

    // ===== 端口修改后重启提示弹窗 =====
    if (showRestartHint) {
        ModalBottomSheet(onDismissRequest = { showRestartHint = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Text("端口已修改", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "新端口将在下次启动时生效。建议先停止再重新启动服务器。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                FilledTonalButton(
                    onClick = { showRestartHint = false },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("知道了") }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ===== 首次启动引导弹窗 =====
    if (showStorageDialogState) {
        AlertDialog(
            onDismissRequest = {
                showStorageDialogState = false
                onStorageDialogDismiss()
            },
            title = { Text("选择工作区文件夹") },
            text = {
                Text("客户端文件操作需要一个工作区文件夹。点击确定后使用系统文件夹选择器选择工作区（无需额外存储权限）。")
            },
            confirmButton = {
                TextButton(onClick = {
                    showStorageDialogState = false
                    onRequestStorage()
                }) { Text("选择文件夹") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showStorageDialogState = false
                    onStorageDialogDismiss()
                }) { Text("稍后") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeTab(
    context: Context,
    status: StatusInfo,
    currentPort: Int,
    currentWorkspace: String,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit,
    onPickWorkspace: () -> Unit,
    onEditPort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ===== 状态卡片 =====
        // M3 Motion token 映射（scheme 由主题统一设为 expressive）：
        // - 按钮圆角 = spatial / fast（组件级；原文明确 spatial 管 rounded corners）
        // - 状态灯 opacity = effects / fast（小元素；effects 不应 overshoot）
        // 形状形变（shape morph）
        val buttonInteraction = remember { MutableInteractionSource() }
        val buttonPressed by buttonInteraction.collectIsPressedAsState()
        val buttonCorner by animateDpAsState(
            targetValue = if (buttonPressed) 16.dp else 28.dp,
            animationSpec = m3FastSpatialSpec(),
            label = "buttonShapeMorph",
        )
        var pulseOn by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                pulseOn = !pulseOn
                delay(900)
            }
        }
        val pulseAlpha by animateFloatAsState(
            targetValue = if (status.running && pulseOn) 1f else if (status.running) 0.42f else 0.6f,
            animationSpec = m3FastEffectsSpec<Float>(),
            label = "statusPulse",
        )
        // 37 个形状令牌中的 Cookie4Sided ↔ Sunny 形变（M3 shape morph motion）
        val statusShape = rememberMorphShape(
            start = MaterialShapes.Cookie4Sided,
            end = MaterialShapes.Sunny,
            targetProgress = if (status.running) 1f else 0f,
            label = "statusShapeMorph",
        )
        Card(
            shape = RoundedCornerShape(buttonCorner),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            color = (if (status.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                .copy(alpha = pulseAlpha),
                            shape = statusShape,
                        ),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (status.running) "服务器运行中" else "服务器未运行",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                    )
                    if (status.running) {
                        Text(
                            "端口 $currentPort · ${status.wifi ?: "未知网络"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }
                // 首页点名能力：Split button —— 主按钮 + 关联动作菜单（连通形状 + 按压形变）
                var showMainMenu by remember { mutableStateOf(false) }
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = if (status.running) onStopServer else onStartServer,
                            interactionSource = buttonInteraction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status.running) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            MaterialSymbol(
                                if (status.running) MaterialSymbols.Stop else MaterialSymbols.PlayArrow,
                                contentDescription = null,
                                filled = true,
                                size = 18.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (status.running) "停止" else "启动")
                        }
                    },
                    trailingButton = {
                        Box {
                            SplitButtonDefaults.TrailingButton(onClick = { showMainMenu = true }) {
                                MaterialSymbol(MaterialSymbols.Tune, contentDescription = "更多操作", size = 20.dp)
                            }
                            DropdownMenu(
                                expanded = showMainMenu,
                                onDismissRequest = { showMainMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("编辑端口") },
                                    onClick = { showMainMenu = false; onEditPort() },
                                )
                                DropdownMenuItem(
                                    text = { Text("选择工作区") },
                                    onClick = { showMainMenu = false; onPickWorkspace() },
                                )
                            }
                        }
                    },
                )
            }
            // 端口编辑行（点击整行弹出修改弹窗）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onEditPort)
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MaterialSymbol(MaterialSymbols.Dns, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    size = 18.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "端口 $currentPort",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }

        // ===== 连接地址（仅运行时显示） =====
        if (status.running) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val ip = status.ips.firstOrNull() ?: "192.168.x.x"
                    val url = "http://$ip:$currentPort/mcp"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MaterialSymbol(MaterialSymbols.Dns, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("连接地址", style = MaterialTheme.typography.titleSmallEmphasized,
                            modifier = Modifier.weight(1f))
                        // 一键复制最常用的局域网地址
                        TextButton(onClick = { copyAddress(context, "局域网 MCP 端点", url) }) {
                            MaterialSymbol(MaterialSymbols.ContentCopy, contentDescription = null,
                                size = 16.dp)
                            Spacer(Modifier.width(6.dp))
                            Text("复制")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    ConnectionRow(label = "局域网 MCP 端点", value = url) {
                        copyAddress(context, "局域网 MCP 端点", url)
                    }
                    val localUrl = "http://127.0.0.1:$currentPort/mcp"
                    ConnectionRow(label = "手机本机 MCP", value = localUrl) {
                        copyAddress(context, "手机本机 MCP", localUrl)
                    }
                    // 内网穿透公网地址（状态每秒轮询刷新）
                    val tunnelUrl = status.tunnelUrl
                    val tunnelActive = status.tunnelActive
                    if (tunnelActive) {
                        if (tunnelUrl != null) {
                            ConnectionRow(label = "公网 MCP 端点", value = tunnelUrl) {
                                copyAddress(context, "公网 MCP 端点", tunnelUrl)
                            }
                            Text(
                                "隧道已连接",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // LoadingIndicator：用 37 形状令牌里的多边形做形变序列
                                    LoadingIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        polygons = listOf(
                                            MaterialShapes.Sunny,
                                            MaterialShapes.Cookie4Sided,
                                            MaterialShapes.Puffy,
                                        ),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "隧道连接中",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                }
                                // 首页点名能力：可定制波形与粗细的 WavyProgressIndicator
                                val density = LocalDensity.current
                                LinearWavyProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    stroke = Stroke(
                                        width = with(density) { 5.dp.toPx() },
                                        cap = StrokeCap.Round,
                                    ),
                                    trackStroke = Stroke(
                                        width = with(density) { 2.dp.toPx() },
                                        cap = StrokeCap.Round,
                                    ),
                                    wavelength = 18.dp,
                                    amplitude = 0.7f,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "同一 Wi-Fi 下的电脑可通过局域网地址连接；开启内网穿透后可通过公网地址连接。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ===== 工作区卡片 =====
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MaterialSymbol(MaterialSymbols.Folder, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("工作区", style = MaterialTheme.typography.titleSmallEmphasized)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "客户端只能操作工作区内的文件。点击下方按钮使用系统文件夹选择器选择工作区文件夹（无需额外存储权限）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        currentWorkspace,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(onClick = onPickWorkspace) { Text("选择工作区文件夹") }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "M3 Expressive · ${McpShapeTokens.OFFICIAL_COUNT} 个官方形状 + ${McpShapeTokens.CUSTOM_COUNT} 个形变令牌 = ${McpShapeTokens.TOTAL_COUNT} 个形状令牌",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LogTab(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "暂无日志",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        items(logs, key = { it.id }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    entry.timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.level,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (entry.level) {
                        "E" -> MaterialTheme.colorScheme.error
                        "W" -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    entry.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConnectionRow(label: String, value: String, onCopy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(8.dp))
        // 明确的复制按钮：带水波纹、contentDescription 与无障碍提示
        IconButton(onClick = onCopy) {
            MaterialSymbol(
                MaterialSymbols.ContentCopy,
                contentDescription = "复制$label",
                tint = MaterialTheme.colorScheme.primary,
                size = 20.dp,
            )
        }
    }
}

/** 复制连接地址并给出提示，避免点了没反应 */
private fun copyAddress(context: Context, label: String, value: String) {
    copyText(context, value)
    android.widget.Toast.makeText(context, "已复制：$label", android.widget.Toast.LENGTH_SHORT).show()
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("mcp", text))
}
