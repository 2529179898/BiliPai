package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class PlayerSubtitleAppearance(
    val inlineFontScale: Float = 1f,
    val fullscreenFontScale: Float = 1.5f,
    val fontWeight: Int = 6,
    val strokeWidth: Float = 2f,
    val horizontalPadding: Int = 24,
    val bottomPadding: Int = 24,
    val backgroundOpacity: Float = 0.67f,
)

/** Changes are controlled by the caller and must update both the renderer and its persisted store. */
@Composable
fun PlayerSubtitleSettingsPanel(
    appearance: PlayerSubtitleAppearance,
    onAppearanceChange: (PlayerSubtitleAppearance) -> Unit,
    onDismiss: () -> Unit,
    isFullscreen: Boolean = false,
    onTimingSettings: (() -> Unit)? = null,
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val bottomPaddingDp = if (isFullscreen && configuration.screenHeightDp > configuration.screenWidthDp) 70 else 0
    PlayerNativeSettingsHost(onDismiss, bottomPaddingDp = bottomPaddingDp) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Box(Modifier.fillMaxWidth().height(45.dp), contentAlignment = Alignment.Center) {
                Text("字幕设置", fontSize = 14.sp)
            }
            SubtitleAppearanceSlider("字体大小", appearance.inlineFontScale, 0.5f..2.5f, 1f, { "${(it * 100).roundToInt()}%" }) {
                onAppearanceChange(appearance.copy(inlineFontScale = it))
            }
            SubtitleAppearanceSlider("全屏字体大小", appearance.fullscreenFontScale, 0.5f..2.5f, 1.5f, { "${(it * 100).roundToInt()}%" }) {
                onAppearanceChange(appearance.copy(fullscreenFontScale = it))
            }
            SubtitleAppearanceSlider("字体粗细（可能无法精确调节）", appearance.fontWeight.toFloat(), 1f..9f, 6f, { "${it.roundToInt()}" }, steps = 7) {
                onAppearanceChange(appearance.copy(fontWeight = it.roundToInt()))
            }
            SubtitleAppearanceSlider("描边粗细", appearance.strokeWidth, 0f..5f, 2f, { "$it" }, steps = 9) {
                onAppearanceChange(appearance.copy(strokeWidth = it))
            }
            SubtitleAppearanceSlider("左右边距", appearance.horizontalPadding.toFloat(), 0f..100f, 24f, { "${it.roundToInt()}" }, steps = 99) {
                onAppearanceChange(appearance.copy(horizontalPadding = it.roundToInt()))
            }
            SubtitleAppearanceSlider("底部边距", appearance.bottomPadding.toFloat(), 0f..200f, 24f, { "${it.roundToInt()}" }, steps = 199) {
                onAppearanceChange(appearance.copy(bottomPadding = it.roundToInt()))
            }
            SubtitleAppearanceSlider("背景不透明度", appearance.backgroundOpacity, 0f..1f, 0.67f, { "${(it * 100).roundToInt()}%" }, steps = 99) {
                onAppearanceChange(appearance.copy(backgroundOpacity = it))
            }
            onTimingSettings?.let { action ->
                HorizontalDivider()
                PlayerSettingsRow("字幕时间偏移", onClick = action)
            }
        }
    }
}

@Composable
private fun SubtitleAppearanceSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    default: Float,
    format: (Float) -> String,
    steps: Int = 199,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$label ${format(value)}", Modifier.weight(1f), fontSize = 14.sp)
            IconButton({ onChange(default) }, Modifier.size(24.dp)) {
                Icon(Icons.Outlined.Refresh, "默认值: ${format(default)}", Modifier.size(24.dp))
            }
        }
        Slider(value.coerceIn(range), onChange, valueRange = range, steps = steps, modifier = Modifier.padding(vertical = 16.dp))
    }
}
