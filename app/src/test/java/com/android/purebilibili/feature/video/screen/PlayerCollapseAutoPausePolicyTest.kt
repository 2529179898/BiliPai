package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.core.store.PortraitPlayerCollapseMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerCollapseAutoPausePolicyTest {

    @Test
    fun pausesOnlyWhenEnabledCollapsedAndPlaying() {
        assertTrue(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = false,
                isPlayerCollapsed = true,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                isPlaying = true,
            )
        )
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = false,
            )
        )
    }

    @Test
    fun resumesOnlyWhenExpandingAfterAutoPause() {
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = false,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
            )
        )
        assertFalse(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = false,
                isPlayerCollapsed = false,
                wasAutoPausedByCollapse = true,
            )
        )
    }

    @Test
    fun compactCommentLayoutRemainsPausedUntilLeavingComments() {
        var playing = true
        var autoPaused = false
        val actions = mutableListOf<String>()
        val expanded = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 760f,
            isCollapsed = false,
        )
        val compact = resolvePortraitInlinePlayerLayoutSpec(
            screenWidthDp = 412f,
            screenHeightDp = 760f,
            isCollapsed = true,
        )
        val autoPauseRange = expanded.heightDp - compact.heightDp

        // Recomposition after pausing enables comment hiding, but must not resume playback.
        for (tab in listOf(1, 1, 1, 0)) {
            val inlineScrollEnabled = shouldEnableInlinePortraitScrollTransform(
                collapseMode = PortraitPlayerCollapseMode.OFF,
                selectedTabIndex = tab,
                isPlaybackPaused = !playing,
            )
            val compactForComments = shouldUseCompactInlinePortraitPlayerForCommentTab(
                useOfficialInlinePortraitDetailExperience = true,
                selectedTabIndex = tab,
                isPortraitFullscreen = false,
                isPlaybackPaused = !playing,
                availableCommentHeightDp = 760f - expanded.heightDp - 32f - 24f,
            )
            val collapsed = resolvePlayerCollapsedForAutoPause(
                swipeHidePlayerEnabled = inlineScrollEnabled,
                playerHeightOffsetPx = 0f,
                videoHeightPx = autoPauseRange,
                compactForCommentTab = compactForComments,
            )
            if (shouldAutoPauseOnPlayerCollapse(true, collapsed, playing)) {
                playing = false
                autoPaused = true
                actions += "pause"
            } else if (shouldAutoResumeOnPlayerExpand(true, collapsed, autoPaused)) {
                playing = true
                autoPaused = false
                actions += "resume"
            }
            if (tab == 1) assertFalse(playing)
        }

        assertEquals(listOf("pause", "resume"), actions)
        assertTrue(playing)
    }

    @Test
    fun nonCompactPlayerResumesOnlyAfterSwipeExpansion() {
        val collapsed = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = -300f,
            videoHeightPx = 300f,
            compactForCommentTab = false,
        )
        assertTrue(shouldAutoPauseOnPlayerCollapse(true, collapsed, true))
        assertFalse(shouldAutoResumeOnPlayerExpand(true, collapsed, true))

        val expanded = resolvePlayerCollapsedForAutoPause(
            swipeHidePlayerEnabled = true,
            playerHeightOffsetPx = 0f,
            videoHeightPx = 300f,
            compactForCommentTab = false,
        )
        assertFalse(shouldAutoPauseOnPlayerCollapse(true, expanded, true))
        assertTrue(shouldAutoResumeOnPlayerExpand(true, expanded, true))
    }

    @Test
    fun portraitFullscreenDoesNotTreatHiddenInlinePlayerAsCollapsed() {
        assertFalse(
            shouldAutoPauseOnPlayerCollapse(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                isPlaying = true,
                isPortraitFullscreen = true,
            )
        )
        assertTrue(
            shouldAutoResumeOnPlayerExpand(
                autoPauseEnabled = true,
                isPlayerCollapsed = true,
                wasAutoPausedByCollapse = true,
                isPortraitFullscreen = true,
            )
        )
    }
}
