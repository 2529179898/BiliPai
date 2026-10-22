package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.LocalAppThemeConfig


internal fun playerNativeMenuConfig(config: com.android.purebilibili.core.ui.AppThemeConfig) =
    config.copy(liquidGlassEnabled = false, nativeMiuixPopupsEnabled = false)
/** Player sheets intentionally do not inherit the application's glass or popup material. */
@Composable
internal fun PlayerNativeMenuTheme(content: @Composable () -> Unit) {
    val config = LocalAppThemeConfig.current
    CompositionLocalProvider(
        LocalAppThemeConfig provides playerNativeMenuConfig(config),
        LocalAppUiStyle provides AppUiStyle.MATERIAL3,
        content = content,
    )
}

@Composable
internal fun PlayerNativeSettingsHost(
    onDismiss: () -> Unit,
    bottomPaddingDp: Int = 0,
    isFullscreen: Boolean = false,
    playerBounds: androidx.compose.ui.unit.IntRect? = null,
    content: @Composable () -> Unit,
) {
    val windowSize = androidx.compose.ui.platform.LocalWindowInfo.current.containerSize
    // 播放器提供避让宿主时，横屏更多操作与选集共用侧面板；其他入口保留原有弹窗。
    if (isFullscreen && windowSize.width > windowSize.height && LocalPlayerPanelViewport.current != null) {
        PlayerNativeMenuTheme {
            PlayerDetailPanel("更多操作", isFullscreen, playerBounds, onDismiss) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(45.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("更多操作", fontSize = 16.sp)
                    Spacer(Modifier.weight(1f))
                    PanelAction(Icons.Outlined.Close, "关闭", onDismiss)
                }
                content()
            }
        }
        return
    }
    val config = LocalConfiguration.current
    val policy = resolveDanmakuSettingsPanelLayoutPolicy(
        isFullscreen = false,
        screenWidthDp = config.screenWidthDp,
        screenHeightDp = config.screenHeightDp,
    )
    PlayerNativeMenuTheme {
        DanmakuSettingsPanelHost(false, policy, onDismiss) {
            Surface(
                modifier = Modifier
                    .widthIn(max = policy.maxWidthDp.dp)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(bottom = bottomPaddingDp.dp)
                    .heightIn(max = policy.maxHeightDp.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            ) { content() }
        }
    }
}

@Composable
internal fun PlayerSettingsRow(title: String, subtitle: String? = null, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title == "弹幕设置") {
            PiliPlusDanmakuSettingsIcon(size = 20.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
        Icon(
            imageVector = when (title) {
                "添加至「稍后再看」" -> Icons.Outlined.WatchLater
                "查看笔记" -> Icons.Outlined.EditNote
                "离线缓存" -> Icons.Outlined.FolderZip
                "保存字幕" -> Icons.Outlined.Download
                "保存封面" -> Icons.Outlined.Image
                "定时关闭" -> Icons.Outlined.HourglassTop
                "播放地址" -> Icons.Outlined.Link
                "重载视频" -> Icons.Outlined.Refresh
                "超分辨率" -> Icons.Outlined.StayCurrentLandscape
                "播放器音量" -> Icons.Outlined.VolumeUp
                "CDN 设置" -> Icons.Outlined.Cloud
                "选择画质" -> Icons.Outlined.PlayCircle
                "选择音质" -> Icons.Outlined.Album
                "解码格式" -> Icons.Outlined.AvTimer
                "播放顺序" -> Icons.Outlined.Repeat
                "弹幕列表" -> Icons.Outlined.Comment
                "字幕设置" -> Icons.Outlined.Subtitles
                "加载字幕" -> Icons.Outlined.FileOpen
                "播放信息" -> Icons.Outlined.Info
                "举报" -> Icons.Outlined.ErrorOutline
                "返回" -> Icons.Outlined.ArrowBack
                else -> Icons.Outlined.Settings
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (!subtitle.isNullOrBlank()) Text(
                subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun PlayerSettingsPopupRow(
    title: String,
    current: String,
    options: List<String>,
    enabledOptions: List<Boolean> = List(options.size) { true },
    onSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PlayerSettingsRow(title, current) { expanded = true }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 14.sp) },
                    enabled = enabledOptions.getOrElse(index) { true },
                    onClick = {
                        expanded = false
                        if (label != current) onSelected(index)
                    },
                    trailingIcon = if (label == current) ({ Icon(Icons.Outlined.Done, null) }) else null,
                )
            }
        }
    }
}

@Composable
internal fun PlayerSettingsSelectionRow(
    title: String,
    selected: Boolean,
    enabled: Boolean = true,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.38f)) },
        supportingContent = if (subtitle != null) ({ Text(subtitle, fontSize = 12.sp) }) else null,
        trailingContent = { if (selected) Icon(Icons.Outlined.Done, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { if (!selected) onClick() },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
