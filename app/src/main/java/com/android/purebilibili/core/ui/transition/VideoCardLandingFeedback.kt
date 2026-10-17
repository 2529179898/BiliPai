package com.android.purebilibili.core.ui.transition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.LocalSystemReduceMotion
import com.android.purebilibili.core.ui.adaptive.MotionTier
import kotlinx.coroutines.flow.collectLatest

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
    val landingFeedback = LocalVideoCardTransitionClock.current?.landingFeedback ?: return this
    val background = rememberUpdatedState(LocalVideoCardTransitionBackgroundState.current)
    val motionEnabled = LocalComponentMotionEnabled.current && !LocalSystemReduceMotion.current
    val density = LocalDensity.current.density
    // Animate physical edge travel, not a fixed percentage: wide cards must not visibly
    // shrink more than feed tiles. Read animated values only in the graphics layer.
    val edgeCompression = remember(layer, bvid, sourceRoute, density) { Animatable(0f, 0.015f) }
    LaunchedEffect(layer, bvid, sourceRoute, density, motionEnabled, landingFeedback) {
        edgeCompression.snapTo(0f)
        if (!motionEnabled) return@LaunchedEffect
        snapshotFlow {
            val state = background.value
            val exposure = state.exposureProvider()
            val sourceMatches = isRecordedNativeCardSource(
                bvid, sourceRoute, state.sourceKeyProvider(),
            )
            if (sourceMatches && state.motionTierProvider() == MotionTier.Normal) {
                exposure to landingFeedback.event
            } else null
        }.collectLatest { frame ->
            edgeCompression.snapTo(0f)
            val readyFrame = frame ?: return@collectLatest
            val event = readyFrame.second ?: return@collectLatest
            if (readyFrame.first != VideoCardTransitionExposure.Idle) return@collectLatest
            var compressionDp = 0f
            // A recreated card may receive the event before its native layer's first draw.
            // Wait for geometry without consuming the event or subscribing composition to size.
            for (attempt in 0..2) {
                compressionDp = resolveVideoCardLandingEdgeCompressionDp(
                    widthPx = layer.size.width.toFloat(),
                    heightPx = layer.size.height.toFloat(),
                    density = density,
                    sourceLayout = event.sourceLayout,
                )
                if (compressionDp > 0f) break
                withFrameNanos { }
            }
            if (compressionDp > 0f && landingFeedback.claim(event, background.value.sourceKeyProvider())) {
                // One impulse starts at identity and comes home on the same spring.
                // No extra timed press after the flying card has already stopped.
                val motion = resolveVideoCardLandingMotion(event.sourceLayout)
                edgeCompression.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = motion.dampingRatio,
                        stiffness = motion.stiffness,
                        visibilityThreshold = 0.015f,
                    ),
                    initialVelocity = resolveVideoCardLandingImpulseVelocity(compressionDp, event.sourceLayout),
                )
            }
        }
    }
    return graphicsLayer {
        // Deferred read: no frame-rate recomposition, layout resizing, or anchor movement.
        val state = background.value
        val value = if (motionEnabled && state.motionTierProvider() == MotionTier.Normal &&
            state.exposureProvider() == VideoCardTransitionExposure.Idle
        ) resolveVideoCardLandingScale(
            widthPx = size.width,
            heightPx = size.height,
            density = density,
            edgeCompressionDp = edgeCompression.value,
            sourceLayout = landingFeedback.event?.sourceLayout ?: VideoCardSourceLayout.COVER_ONLY,
        ) else 1f
        scaleX = value
        scaleY = value
    }
}
