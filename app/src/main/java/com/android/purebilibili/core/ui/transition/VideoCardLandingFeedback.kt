package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.adaptive.MotionTier
import com.android.purebilibili.core.util.CardPositionManager
import kotlinx.coroutines.flow.collectLatest

/** Only a committed return may start the source card's landing feedback. */
internal fun shouldPlayVideoCardLandingFeedback(
    previous: VideoCardTransitionExposure?,
    current: VideoCardTransitionExposure?,
): Boolean = previous == VideoCardTransitionExposure.Returning &&
    current == VideoCardTransitionExposure.Idle

/**
 * Custom video morph feedback, shared across materials; native MD3/Miuix controls are untouched.
 * Keep the flying entry and the first source frame at 1x. Compress only after handoff, outside
 * native pixel recording, so another click never captures an already scaled card.
 * Cover-overlay recording must not opt in: the entire card already owns this transform.
 */
@Composable
internal fun Modifier.videoCardLandingFeedback(
    layer: GraphicsLayer,
    bvid: String,
    sourceRoute: String?,
    enabled: Boolean,
): Modifier {
    if (!enabled) return this
    val background = rememberUpdatedState(LocalVideoCardTransitionBackgroundState.current)
    val motionEnabled = LocalComponentMotionEnabled.current && !LocalSystemReduceMotion.current
    val scale = remember(layer, bvid, sourceRoute) { Animatable(1f, 0.0001f) }
    LaunchedEffect(layer, bvid, sourceRoute, motionEnabled) {
        scale.snapTo(1f)
        if (!motionEnabled) return@LaunchedEffect
        var previous: VideoCardTransitionExposure? = null
        snapshotFlow {
            val state = background.value
            val exposure = state.exposureProvider()
            val sourceMatches = isRecordedNativeCardSource(
                bvid, sourceRoute, state.sourceKeyProvider(),
            )
            // Native layers retire two frames after landing. Do not cancel an active pulse
            // when that reference clears; ownership is needed only to arm the return.
            if (sourceMatches && state.motionTierProvider() == MotionTier.Normal &&
                (exposure == VideoCardTransitionExposure.Idle ||
                    CardPositionManager.isNativeVideoCardLayerCurrentOwner(layer))
            ) exposure else null
        }.collectLatest { exposure ->
            val play = shouldPlayVideoCardLandingFeedback(previous, exposure)
            previous = exposure
            scale.snapTo(1f)
            if (play) {
                scale.animateTo(0.992f, tween(durationMillis = 60))
                scale.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 500f))
            }
        }
    }
    return graphicsLayer {
        // Deferred read: no frame-rate recomposition, layout resizing, or anchor movement.
        val state = background.value
        val value = if (motionEnabled && state.motionTierProvider() == MotionTier.Normal &&
            state.exposureProvider() == VideoCardTransitionExposure.Idle
        ) scale.value else 1f
        scaleX = value
        scaleY = value
    }
}
