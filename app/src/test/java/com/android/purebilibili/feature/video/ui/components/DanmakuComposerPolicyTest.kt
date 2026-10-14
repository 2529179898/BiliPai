package com.android.purebilibili.feature.video.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuComposerPolicyTest {

    @Test
    fun inlineComposer_usedForLandscapeFullscreenMode() {
        assertTrue(shouldUseInlineDanmakuComposer(isFullscreenMode = true))
        assertFalse(shouldUseInlineDanmakuComposer(isFullscreenMode = false))
    }

    @Test
    fun fullscreenSend_remainsVisibleAcrossViewportWidths() {
        listOf(320, 393, 479, 480, 616, 720, 840, 1600).forEach { widthDp ->
            assertTrue(shouldShowDanmakuSendInControlBar(isFullscreen = true, widthDp = widthDp))
        }
    }

    @Test
    fun inlineSend_retainsRoomyCinemaVisibility() {
        assertFalse(shouldShowDanmakuSendInControlBar(isFullscreen = false, widthDp = 411))
        assertFalse(shouldShowDanmakuSendInControlBar(isFullscreen = false, widthDp = 600))
        assertFalse(shouldShowDanmakuSendInControlBar(isFullscreen = false, widthDp = 719))
        assertTrue(shouldShowDanmakuSendInControlBar(isFullscreen = false, widthDp = 720))
        assertTrue(shouldShowDanmakuSendInControlBar(isFullscreen = false, widthDp = 800))
    }


}