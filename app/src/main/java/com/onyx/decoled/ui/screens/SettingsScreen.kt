package com.onyx.decoled.ui.screens

import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onyx.decoled.BuildConfig
import com.onyx.decoled.DecoTileService
import com.onyx.decoled.LedController
import com.onyx.decoled.MainViewModel
import com.onyx.decoled.NodeState
import com.onyx.decoled.R
import com.onyx.decoled.RootState
import com.onyx.decoled.ui.components.IconBadge
import com.onyx.decoled.ui.components.ListRow
import com.onyx.decoled.ui.components.NavItem
import com.onyx.decoled.ui.components.S1ngerHaptics
import com.onyx.decoled.ui.components.SectionHeader
import com.onyx.decoled.ui.components.SegmentedGroup
import com.onyx.decoled.ui.components.SegmentedItem
import com.onyx.decoled.ui.components.SwitchItem
import com.onyx.decoled.ui.components.ValueItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    outerPadding: PaddingValues,
    listState: LazyListState,
    topBarState: TopAppBarState,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var showBlackoutDialog by remember { mutableStateOf(false) }

    val deviceName = remember {
        val modelName = if (Build.MODEL == "25053RT47C") "Redmi Turbo 4 Pro" else Build.MODEL
        "$modelName (${Build.DEVICE})"
    }

    PageScaffold(
        title = "设置",
        outerPadding = outerPadding,
        listState = listState,
        topBarState = topBarState
    ) {
        // Section: 权限与驱动
        item(key = "section_permissions") {
            SectionHeader(text = "权限与驱动")
        }

        item(key = "group_permissions") {
            SegmentedGroup {
                // Item 0: Root permission
                val rootSupporting = when (uiState.root) {
                    RootState.Checking -> "检查中…"
                    RootState.Granted -> "已就绪 · KernelSU"
                    RootState.Denied -> "未获取，点击重试"
                }

                SegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = {
                        S1ngerHaptics.confirm(haptic)
                        viewModel.checkRoot()
                    }
                ) {
                    ListRow(
                        title = "Root 权限",
                        supporting = rootSupporting,
                        leading = { IconBadge(Icons.Outlined.Security) },
                        trailing = {
                            if (uiState.root == RootState.Checking) {
                                LoadingIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "刷新",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    )
                }

                // Item 1: Driver node
                val nodeText = when (uiState.node) {
                    NodeState.Writable -> "可写"
                    NodeState.ReadOnly -> "只读（Shield）"
                    NodeState.Missing -> "未找到"
                    NodeState.Unknown -> "未知"
                }

                SegmentedItem(
                    index = 1,
                    count = 2,
                    onClick = null
                ) {
                    ListRow(
                        title = "驱动节点",
                        supporting = LedController.SYSFS_LED,
                        leading = { IconBadge(Icons.Outlined.Memory) },
                        trailing = {
                            Text(
                                text = nodeText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Section: 保护
        item(key = "section_protection") {
            SectionHeader(text = "保护")
        }

        item(key = "group_protection") {
            SegmentedGroup {
                // Item 0: Shield mode
                SegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = {
                        val target = !uiState.shield
                        if (target) S1ngerHaptics.toggleOn(haptic) else S1ngerHaptics.toggleOff(haptic)
                        viewModel.setShieldMode(target)
                    }
                ) {
                    SwitchItem(
                        icon = Icons.Outlined.Lock,
                        title = "防系统抢占",
                        supporting = "锁定节点为只读，防止充电/通知灯效覆盖",
                        checked = uiState.shield,
                        onCheckedChange = { viewModel.setShieldMode(it) }
                    )
                }

                // Item 1: Boot sync
                SegmentedItem(
                    index = 1,
                    count = 2,
                    onClick = {
                        val target = !uiState.bootSync
                        if (target) S1ngerHaptics.toggleOn(haptic) else S1ngerHaptics.toggleOff(haptic)
                        viewModel.setBootSync(target)
                    }
                ) {
                    SwitchItem(
                        icon = Icons.Outlined.RestartAlt,
                        title = "开机自启保持",
                        supporting = "写入 /data/adb/service.d，重启后自动应用",
                        checked = uiState.bootSync,
                        onCheckedChange = { viewModel.setBootSync(it) }
                    )
                }
            }
        }

        // Section: 快捷操作
        item(key = "section_actions") {
            SectionHeader(text = "快捷操作")
        }

        item(key = "group_actions") {
            SegmentedGroup {
                // Item 0: Quick settings tile
                SegmentedItem(
                    index = 0,
                    count = 2,
                    onClick = {
                        S1ngerHaptics.segmentTick(haptic)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val sbm = context.getSystemService(StatusBarManager::class.java)
                            sbm?.requestAddTileService(
                                ComponentName(context, DecoTileService::class.java),
                                context.getString(R.string.tile_name),
                                AndroidIcon.createWithResource(context, R.drawable.ic_qs_led),
                                context.mainExecutor
                            ) { /* Result callback */ }
                        } else {
                            Toast.makeText(context, "请在下拉面板编辑中手动添加", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    NavItem(
                        icon = Icons.Outlined.Dashboard,
                        title = "添加快速设置磁贴",
                        supporting = "将开关放入下拉控制中心"
                    )
                }

                // Item 1: Full blackout
                SegmentedItem(
                    index = 1,
                    count = 2,
                    onClick = {
                        S1ngerHaptics.segmentTick(haptic)
                        showBlackoutDialog = true
                    }
                ) {
                    ListRow(
                        title = "一键全熄并锁定",
                        supporting = "关闭光环并写保护节点",
                        leading = { IconBadge(Icons.Outlined.PowerSettingsNew) },
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Section: 关于
        item(key = "section_about") {
            SectionHeader(text = "关于")
        }

        item(key = "group_about") {
            SegmentedGroup {
                SegmentedItem(
                    index = 0,
                    count = 3,
                    onClick = null
                ) {
                    ValueItem(
                        icon = Icons.Outlined.Info,
                        title = "版本",
                        value = BuildConfig.VERSION_NAME
                    )
                }

                SegmentedItem(
                    index = 1,
                    count = 3,
                    onClick = null
                ) {
                    ValueItem(
                        icon = Icons.Outlined.Smartphone,
                        title = "设备",
                        value = deviceName
                    )
                }

                SegmentedItem(
                    index = 2,
                    count = 3,
                    onClick = null
                ) {
                    ValueItem(
                        icon = Icons.Outlined.DeveloperBoard,
                        title = "驱动芯片",
                        value = "Awinic AW21024"
                    )
                }
            }
        }
    }

    if (showBlackoutDialog) {
        AlertDialog(
            onDismissRequest = { showBlackoutDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp,
            title = {
                Text(
                    text = "全熄并锁定？",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "将关闭氛围灯、开启 Shield，并更新开机脚本。",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        S1ngerHaptics.reject(haptic)
                        viewModel.fullBlackout()
                        showBlackoutDialog = false
                    }
                ) {
                    Text(
                        text = "全熄锁定",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBlackoutDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
}
