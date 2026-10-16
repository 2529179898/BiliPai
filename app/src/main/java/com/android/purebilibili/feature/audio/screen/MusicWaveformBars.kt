package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.android.purebilibili.core.player.waveform.AUDIO_WAVEFORM_BAR_COUNT
import com.android.purebilibili.core.player.waveform.AudioWaveformSource
import com.android.purebilibili.core.player.waveform.isWaveformSnapshotFresh
import com.android.purebilibili.core.player.waveform.resolveWaveformBars
import com.android.purebilibili.core.player.waveform.resolveWaveformFrame

internal const val MUSIC_WAVEFORM_BAR_WIDTH_DP = 2
internal const val MUSIC_WAVEFORM_BAR_GAP_DP = 2
internal const val MUSIC_WAVEFORM_HEIGHT_DP = 18
internal const val MUSIC_WAVEFORM_WIDTH_DP =
    AUDIO_WAVEFORM_BAR_COUNT * MUSIC_WAVEFORM_BAR_WIDTH_DP +
        (AUDIO_WAVEFORM_BAR_COUNT - 1) * MUSIC_WAVEFORM_BAR_GAP_DP

/**
 * 灵动岛式声音指示器：圆头短柱围绕中线伸缩，不改变布局或驱动歌词缩放。
 * 读取真实 PCM 能量，单次包络跟随；暂停回落后停止请求帧，后台暂停刷新。
 * 减少动态效果时显示固定短柱，不冻结某一帧音频；静音时收拢为圆点。
 */
@Composable
internal fun MusicWaveformBars(
    isPlaying: Boolean,
    reduceMotion: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = AUDIO_WAVEFORM_BAR_COUNT,
) {
    val density = LocalDensity.current
    val barWidthPx = with(density) { MUSIC_WAVEFORM_BAR_WIDTH_DP.dp.toPx() }
    val gapPx = with(density) { MUSIC_WAVEFORM_BAR_GAP_DP.dp.toPx() }
    val maxHeightPx = with(density) { MUSIC_WAVEFORM_HEIGHT_DP.dp.toPx() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val count = barCount.coerceAtLeast(0)
    // 仅在 Canvas 内读取，音频帧只触发重绘，不重组歌曲信息。
    var bars by remember(count) { mutableStateOf(FloatArray(count)) }

    LaunchedEffect(count, reduceMotion, isPlaying, lifecycleOwner) {
        if (reduceMotion || count == 0) {
            bars = FloatArray(count)
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousFrameNanos = 0L
            var settled = false
            while (!settled) {
                withFrameNanos { frameNanos ->
                    val elapsedMs = if (previousFrameNanos == 0L) 0f else
                        (frameNanos - previousFrameNanos) / 1_000_000f
                    previousFrameNanos = frameNanos
                    val snapshot = AudioWaveformSource.snapshot()
                    val followingAudio = isPlaying &&
                        isWaveformSnapshotFresh(snapshot.updatedAtNanos, System.nanoTime())
                    val target = if (followingAudio) resolveWaveformBars(snapshot.samples, snapshot.positionUs, count) else FloatArray(count)
                    val next = resolveWaveformFrame(bars, target, followingAudio, elapsedMs)
                    if (!next.contentEquals(bars)) bars = next
                    settled = !isPlaying && next.all { it == 0f }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        if (count == 0 || size.height <= 0f || size.width <= 0f) return@Canvas
        // 缩为图标时仍在画布内等比收拢；48dp 控件槽位也不会拉高短柱。
        val naturalWidth = count * barWidthPx + (count - 1) * gapPx
        val fit = minOf(1f, size.width / naturalWidth, size.height / barWidthPx)
        val width = barWidthPx * fit
        val gap = gapPx * fit
        val height = minOf(size.height, maxHeightPx)
        val centerY = size.height / 2f
        var x = (size.width - (count * width + (count - 1) * gap)) / 2f
        repeat(count) { index ->
            val value = if (reduceMotion) {
                if (isPlaying) when (index % 5) { 0, 4 -> 0.12f; 1, 3 -> 0.28f; else -> 0.42f } else 0f
            } else bars[index]
            val barHeight = width + (height - width) * value.coerceIn(0f, 1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(width, barHeight),
                cornerRadius = CornerRadius(width / 2f, width / 2f),
            )
            x += width + gap
        }
    }
}
