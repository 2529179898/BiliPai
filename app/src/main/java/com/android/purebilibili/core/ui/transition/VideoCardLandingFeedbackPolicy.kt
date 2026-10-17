package com.android.purebilibili.core.ui.transition

import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

internal const val VIDEO_CARD_LANDING_DAMPING = 0.80f
internal const val VIDEO_CARD_LANDING_STIFFNESS = 400f
internal class VideoCardLandingMotion(
    val dampingRatio: Float,
    val stiffness: Float,
    val maxEdgeCompressionDp: Float,
    val maxScaleCompression: Float,
) {
    // Cache the unit-velocity spring response once per layout profile, not per card/frame.
    val peakResponseSeconds: Float = run {
        val omega = sqrt(stiffness)
        val dampedOmega = omega * sqrt(1f - dampingRatio * dampingRatio)
        val decay = dampingRatio * omega
        val peakTime = atan(dampedOmega / decay) / dampedOmega
        exp(-decay * peakTime) * sin(dampedOmega * peakTime) / dampedOmega
    }
}

private val stackedLandingMotion = VideoCardLandingMotion(
    VIDEO_CARD_LANDING_DAMPING, VIDEO_CARD_LANDING_STIFFNESS, 1.1f, 0.012f,
)
private val horizontalLandingMotion = VideoCardLandingMotion(
    dampingRatio = 0.84f,
    stiffness = 320f,
    maxEdgeCompressionDp = 1.25f,
    maxScaleCompression = 0.009f,
)
private val coverLandingMotion = VideoCardLandingMotion(
    VIDEO_CARD_LANDING_DAMPING, VIDEO_CARD_LANDING_STIFFNESS, 0.65f, 0.008f,
)

/** Custom card-morph profiles only: native material defaults and stacked cards stay unchanged. */
internal fun resolveVideoCardLandingMotion(sourceLayout: VideoCardSourceLayout): VideoCardLandingMotion =
    when (sourceLayout) {
        VideoCardSourceLayout.STACKED -> stackedLandingMotion
        VideoCardSourceLayout.SIDE_BY_SIDE -> horizontalLandingMotion
        VideoCardSourceLayout.COVER_ONLY -> coverLandingMotion
    }

/** Keep stacked feed tiles perceptible; wide cards retain a smaller physical travel budget. */
internal fun resolveVideoCardLandingEdgeCompressionDp(
    widthPx: Float,
    heightPx: Float,
    density: Float,
    sourceLayout: VideoCardSourceLayout,
): Float {
    if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f ||
        !density.isFinite() || density <= 0f
    ) return 0f
    val longestEdgeDp = maxOf(widthPx, heightPx) / density
    if (!longestEdgeDp.isFinite()) return 0f
    val motion = resolveVideoCardLandingMotion(sourceLayout)
    return minOf(motion.maxEdgeCompressionDp, longestEdgeDp * motion.maxScaleCompression / 2f)
}

internal fun resolveVideoCardLandingImpulseVelocity(
    edgeCompressionDp: Float,
    sourceLayout: VideoCardSourceLayout,
): Float {
    if (!edgeCompressionDp.isFinite() || edgeCompressionDp <= 0f) return 0f
    val motion = resolveVideoCardLandingMotion(sourceLayout)
    return edgeCompressionDp.coerceAtMost(motion.maxEdgeCompressionDp) / motion.peakResponseSeconds
}

internal fun resolveVideoCardLandingScale(
    widthPx: Float,
    heightPx: Float,
    density: Float,
    edgeCompressionDp: Float,
    sourceLayout: VideoCardSourceLayout,
): Float {
    if (!widthPx.isFinite() || !heightPx.isFinite() || widthPx <= 0f || heightPx <= 0f ||
        !density.isFinite() || density <= 0f ||
        !edgeCompressionDp.isFinite()
    ) return 1f
    val longestEdgePx = maxOf(widthPx, heightPx)
    val limit = resolveVideoCardLandingEdgeCompressionDp(widthPx, heightPx, density, sourceLayout)
    // Clamp the imperceptible negative spring lobe: no second enlargement or text shimmer.
    val edgeTravel = edgeCompressionDp.coerceIn(0f, limit)
    return 1f - 2f * edgeTravel * density / longestEdgePx
}
