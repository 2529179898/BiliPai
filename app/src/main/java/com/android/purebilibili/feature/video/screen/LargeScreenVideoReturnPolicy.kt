package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.resolveVideoDetailReturnMediaLayoutHandoffProgress
import kotlin.math.roundToInt

internal fun resolveLargeScreenSidePanelRevealWidthPx(
    baseWidthPx: Int,
    windowWidthPx: Int,
    progress: Float,
): Int {
    val base = baseWidthPx.coerceAtLeast(0)
    val target = windowWidthPx.coerceAtLeast(base)
    return (base + (target - base) * progress.coerceIn(0f, 1f)).roundToInt()
}

internal fun resolveLargeScreenVideoReturnMediaProgress(
    hasLandingGeometry: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
    isReturning: Boolean,
    isCommittedReturn: Boolean,
    depth: Float,
    sourceLayout: VideoCardSourceLayout,
): Float {
    if (!hasLandingGeometry) return 0f
    if (isCommittedReturn && phase == VideoCardTransitionBackgroundPhase.IDLE) return 1f
    if (!isReturning && phase != VideoCardTransitionBackgroundPhase.RETURNING) return 0f
    return resolveVideoDetailReturnMediaLayoutHandoffProgress(
        morphDepthProgress = depth,
        phase = phase,
        isReturnGestureInProgress = isReturning,
        sourceLayout = sourceLayout,
    )
}

internal fun shouldHideLargeScreenVideoAfterReturn(
    isCommittedReturn: Boolean,
    phase: VideoCardTransitionBackgroundPhase,
): Boolean = isCommittedReturn && phase == VideoCardTransitionBackgroundPhase.IDLE
