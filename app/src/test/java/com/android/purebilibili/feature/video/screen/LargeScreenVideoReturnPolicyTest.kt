package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LargeScreenVideoReturnPolicyTest {
    @Test
    fun sidePaneDragExpandsOnlyThePlayerViewportAtTheFingerProgress() {
        assertEquals(1200, resolveLargeScreenSidePanelRevealWidthPx(900, 1500, 0.5f))
        assertEquals(900, resolveLargeScreenSidePanelRevealWidthPx(900, 1500, 0f))
        assertEquals(1500, resolveLargeScreenSidePanelRevealWidthPx(900, 1500, 1f))
    }

    @Test
    fun openingNeverShrinksTheSplitPlayerIntoTheSourceRow() {
        assertEquals(0f, progress(VideoCardTransitionBackgroundPhase.OPENING, depth = 0.1f))
    }

    @Test
    fun cancelledReturnRestoresTheDetailViewport() {
        val returning = progress(VideoCardTransitionBackgroundPhase.RETURNING, returning = true, depth = 0f)
        assertTrue(returning > 0f)
        assertEquals(0f, progress(VideoCardTransitionBackgroundPhase.HELD))
        assertFalse(shouldHideLargeScreenVideoAfterReturn(false, VideoCardTransitionBackgroundPhase.IDLE))
    }

    @Test
    fun committedReturnKeepsLandingGeometryAndYieldsDrawingToList() {
        assertEquals(1f, progress(VideoCardTransitionBackgroundPhase.IDLE, committed = true))
        assertTrue(shouldHideLargeScreenVideoAfterReturn(true, VideoCardTransitionBackgroundPhase.IDLE))
        assertFalse(shouldHideLargeScreenVideoAfterReturn(true, VideoCardTransitionBackgroundPhase.RETURNING))
    }

    @Test
    fun missingSourceGeometryLeavesPlayerUntouched() {
        assertEquals(0f, resolveLargeScreenVideoReturnMediaProgress(
            hasLandingGeometry = false,
            phase = VideoCardTransitionBackgroundPhase.RETURNING,
            isReturning = true,
            isCommittedReturn = true,
            depth = 0f,
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
        ))
    }

    private fun progress(
        phase: VideoCardTransitionBackgroundPhase,
        returning: Boolean = false,
        committed: Boolean = false,
        depth: Float = 1f,
    ) = resolveLargeScreenVideoReturnMediaProgress(
        hasLandingGeometry = true,
        phase = phase,
        isReturning = returning,
        isCommittedReturn = committed,
        depth = depth,
        sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
    )
}
