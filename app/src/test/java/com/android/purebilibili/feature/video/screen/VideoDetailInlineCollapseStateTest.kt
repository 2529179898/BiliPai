package com.android.purebilibili.feature.video.screen

import com.android.purebilibili.feature.video.policy.reduceVideoDetailPostScroll
import com.android.purebilibili.feature.video.policy.reduceVideoDetailPreScroll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailInlineCollapseStateTest {
    @Test
    fun disabledCollapse_commentScrollDoesNotInterruptRestore() {
        val state = InlinePortraitPlayerCollapseState(initialOffsetPx = -120f)
        state.restore()

        val preScroll = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -24f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = false,
            isPortraitFullscreen = false,
        )
        assertEquals(0f, state.consumeScroll(preScroll))
        val postScroll = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 24f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = false,
            isPortraitFullscreen = false,
        )
        assertEquals(0f, state.consumeScroll(postScroll))
        assertEquals(0f, state.offsetPx)
        assertTrue(state.restoreRequested)
    }

    @Test
    fun expandedPlayer_downwardCommentDragDoesNotInterruptRestore() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPostScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = 30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )

        assertEquals(0f, state.consumeScroll(update))
        assertTrue(state.restoreRequested)
    }

    @Test
    fun consumedPlayerDrag_interruptsRestoreAndFollowsFinger() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = false,
        )

        assertEquals(-30f, state.consumeScroll(update))
        assertEquals(-30f, state.offsetPx)
        assertFalse(state.restoreRequested)
    }

    @Test
    fun fullscreenCommentScroll_doesNotMutateInlinePlayerState() {
        val state = InlinePortraitPlayerCollapseState(initialRestoreRequested = true)
        val update = reduceVideoDetailPreScroll(
            currentOffsetPx = state.offsetPx,
            deltaPx = -30f,
            minOffsetPx = -240f,
            inlinePortraitScrollEnabled = true,
            isPortraitFullscreen = true,
        )

        assertEquals(0f, state.consumeScroll(update))
        assertEquals(0f, state.offsetPx)
        assertTrue(state.restoreRequested)
    }
}
