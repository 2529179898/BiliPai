package com.android.purebilibili.feature.video.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaybackResumeNoticePolicyTest {
    @Test
    fun `history restoration is announced but restarts and handoffs are silent`() {
        assertTrue(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 0L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(0L, 0L, 0L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 436_000L, false, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, true, false))
        assertFalse(shouldAnnounceSavedPlaybackPosition(436_000L, 436_000L, 0L, false, true))
    }

    @Test
    fun `notice waits for ready player at the restored position`() {
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 0L, true))
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 436_000L, false))
        assertTrue(hasReachedSavedPlaybackPosition(436_000L, 436_000L, true))
        assertTrue(hasReachedSavedPlaybackPosition(436_000L, 437_000L, true))
        assertFalse(hasReachedSavedPlaybackPosition(436_000L, 600_000L, true))
    }

    @Test
    fun `position message supports minutes and hour long videos`() {
        assertEquals("欢迎回来，已为您定位至 07:16", buildPlaybackResumeNoticeMessage(436_000L))
        assertEquals("欢迎回来，已为您定位至 01:07:16", buildPlaybackResumeNoticeMessage(4_036_000L))
    }
}
