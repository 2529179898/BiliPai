package com.android.purebilibili.feature.audio.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.player.waveform.AudioWaveformSource
import com.android.purebilibili.core.player.waveform.isWaveformSnapshotFresh
import com.android.purebilibili.core.player.waveform.resolveWaveformBars
import com.android.purebilibili.core.ui.rememberDecorativeAnimationGate
import com.android.purebilibili.feature.audio.lyrics.halcyon.LocalPlayerSurfaceActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.exp

/** Slow energy following prevents speech and drum transients from flashing the whole screen. */
internal fun resolveMusicAtmosphereEnergy(current: Float, target: Float, elapsedMs: Float): Float {
    val from = current.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val to = target.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val timeConstant = if (to > from) 400f else 900f
    val delta = elapsedMs.takeIf { it.isFinite() }?.coerceIn(0f, 100f) ?: 0f
    val next = from + (to - from) * (1f - exp(-delta / timeConstant))
    return if (to == 0f && next < 0.001f) 0f else next
}

/** Supplies audio energy to the original immersive renderer. No replacement colours or scrim. */
@Composable
internal fun MusicAtmosphereHost(
    enabled: Boolean,
    isPlaying: Boolean,
    reduceMotion: Boolean,
    lyricsPageProgress: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable (() -> Float) -> Unit,
) {
    val gate = rememberDecorativeAnimationGate()
    val canAnimate = enabled && gate.active && LocalPlayerSurfaceActive.current && !reduceMotion
    var energy by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(canAnimate, isPlaying) {
        if (!canAnimate) {
            energy = 0f
            return@LaunchedEffect
        }
        var previousNanos = System.nanoTime()
        while (isActive) {
            val now = System.nanoTime()
            val elapsedMs = ((now - previousNanos) / 1_000_000f).coerceIn(0f, 100f)
            previousNanos = now
            val snapshot = AudioWaveformSource.snapshot()
            val target = if (isPlaying && isWaveformSnapshotFresh(snapshot.updatedAtNanos, now)) {
                resolveWaveformBars(snapshot.samples, snapshot.positionUs, barCount = 1).first()
            } else 0f
            energy = resolveMusicAtmosphereEnergy(energy, target, elapsedMs)
            if (!isPlaying && energy == 0f) break
            delay(50L)
        }
    }
    Box(modifier = modifier.then(gate.modifier)) {
        // The renderer invokes this only in its graphics/draw layer, keeping all audio and
        // pager frame updates out of foreground composition. Disabled is an exact identity.
        content {
            if (canAnimate) energy * (1f - 0.65f * lyricsPageProgress().coerceIn(0f, 1f)) else 0f
        }
    }
}
