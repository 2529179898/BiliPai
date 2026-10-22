package com.android.purebilibili.navigation3

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.purebilibili.core.store.AppNavigationSettings
import com.android.purebilibili.core.store.mapAppNavigationSettingsFromPreferences
import com.android.purebilibili.core.store.navigation.settingsContainerTransitionPreferencesKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase

class SettingsContainerTransitionPolicyTest {
    @Test
    fun `nested container keeps root source mounted through deeper pushes and pops`() {
        val root = BiliPaiNavKey.Settings
        val category = BiliPaiNavKey.SettingsCategory(
            com.android.purebilibili.feature.settings.SettingsRootCategory.PLAYBACK_QUALITY
        )
        val detail = BiliPaiNavKey.PlaybackSettings
        val parents = mapOf<BiliPaiNavKey, BiliPaiNavKey>(category to root, detail to category)
        assertEquals(1f, settingsContainerRetainedDepth(listOf(root)) { parents[it] })
        assertEquals(2f, settingsContainerRetainedDepth(listOf(root, category)) { parents[it] })
        assertEquals(3f, settingsContainerRetainedDepth(listOf(root, category, detail)) { parents[it] })
        // An unrelated page below settings does not enlarge the retained settings chain.
        assertEquals(2f, settingsContainerRetainedDepth(listOf(BiliPaiNavKey.Home, root, category)) { parents[it] })
    }

    @Test
    fun `page drawing grows with container width and lands without a size jump`() {
        // Phone, wide pane and narrow split use their actual entry-to-host proportions.
        for ((closedWidth, pageWidth) in listOf(328f to 360f, 400f to 840f, 248f to 280f)) {
            var previous = 0f
            for (step in 0..100) {
                val progress = settingsContainerProgress(step / 100f, closing = false)
                val width = closedWidth + (pageWidth - closedWidth) * progress
                val scale = settingsContainerPageScale(width, pageWidth)
                assertTrue(scale >= previous && scale <= 1f)
                assertEquals(width, pageWidth * scale, 0.001f)
                previous = scale
            }
            assertEquals(1f, previous)
        }
        assertEquals(1f, settingsContainerPageScale(360f, 360f))
        assertEquals(1f, settingsContainerPageScale(0f, 0f))
    }

    @Test
    fun `miuix card surface returns to its own tone in light and dark themes`() {
        val pairs = listOf(
            androidx.compose.ui.graphics.Color(0xFF242424) to androidx.compose.ui.graphics.Color(0xFF101010),
            androidx.compose.ui.graphics.Color.White to androidx.compose.ui.graphics.Color(0xFFF5F5F5),
        )
        for ((card, page) in pairs) {
            assertEquals(card, settingsContainerSurfaceColor(card, page, 0f, inheritedSurface = true))
            assertEquals(page, settingsContainerSurfaceColor(card, page, 1f, inheritedSurface = true))
            // A sizable returning container must still contain readable page content.
            val progress = settingsContainerProgress(0.6f, closing = true)
            assertTrue(settingsContainerContentAlpha(0.6f, closing = true) > 0f)
            assertEquals(
                androidx.compose.ui.graphics.lerp(card, page, progress),
                settingsContainerSurfaceColor(card, page, progress, inheritedSurface = true),
            )
        }
    }

    @Test
    fun `wallpaper card tint is not painted twice at the closed endpoint`() {
        val card = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.74f)
        val page = androidx.compose.ui.graphics.Color(0xFF101010)
        assertEquals(androidx.compose.ui.graphics.Color.Transparent,
            settingsContainerSurfaceColor(card, page, 0f, inheritedSurface = true))
        assertEquals(page, settingsContainerSurfaceColor(card, page, 1f, inheritedSurface = true))
        // Explicitly painted sources keep their original alpha instead of inheriting this rule.
        assertEquals(card, settingsContainerSurfaceColor(card, page, 0f))
    }

    @Test
    fun `return coordinates are measured in the current host and invalid sources fall back`() {
        val host = androidx.compose.ui.geometry.Rect(40f, 60f, 440f, 860f)
        val source = androidx.compose.ui.geometry.Rect(56f, 160f, 424f, 240f)
        assertEquals(androidx.compose.ui.geometry.Rect(16f, 100f, 384f, 180f), settingsContainerLocalBounds(source, host))
        val moved = source.translate(androidx.compose.ui.geometry.Offset(0f, 120f))
        assertEquals(androidx.compose.ui.geometry.Rect(16f, 220f, 384f, 300f), settingsContainerLocalBounds(moved, host))
        assertNull(settingsContainerLocalBounds(androidx.compose.ui.geometry.Rect.Zero, host))
        assertNull(settingsContainerLocalBounds(source.translate(androidx.compose.ui.geometry.Offset(500f, 0f)), host))
    }

    @Test
    fun `reference drag easing passes through its shared cubic midpoint`() {
        assertEquals(0f, SettingsContainerDragEasing.transform(0f), 0.0001f)
        assertEquals(0.541f, SettingsContainerDragEasing.transform(0.198f), 0.0001f)
        assertEquals(1f, SettingsContainerDragEasing.transform(1f), 0.0001f)
        assertEquals(300, SETTINGS_CONTAINER_DURATION_MILLIS)
        assertEquals(350, SETTINGS_CONTAINER_DRAG_SETTLE_MILLIS)
    }

    @Test
    fun `source owns first and last frames without waiting for destination mount or disposal`() {
        fun owns(depth: Float) = settingsContainerDestinationOwnsContent(depth, true)
        assertFalse(owns(0f)) // Incoming entry is still culled.
        assertTrue(owns(0.01f))
        assertTrue(owns(1f)) // Settled destination covers the source.
        assertTrue(owns(0.5f))
        assertFalse(owns(0f)) // Outgoing entry culled, onDispose has not run.
    }

    @Test
    fun `live entry stays in container throughout return and invalid geometry restores source`() {
        for (depth in listOf(1f, 0.75f, 0.5f, 0.25f)) {
            assertTrue(settingsContainerDestinationOwnsContent(depth, true))
        }
        assertFalse(settingsContainerDestinationOwnsContent(0f, true))
        assertFalse(settingsContainerDestinationOwnsContent(0.5f, false))
        assertFalse(settingsContainerDestinationOwnsContent(-1f, true))
    }

    @Test
    fun `container return mode survives release through commit and cancellation`() {
        assertTrue(settingsContainerDragActive(true, null))
        assertTrue(settingsContainerDragActive(false, NavSettlePhase.Commit))
        assertTrue(settingsContainerDragActive(false, NavSettlePhase.Cancel))
        assertFalse(settingsContainerDragActive(false, NavSettlePhase.Programmatic))
        assertFalse(settingsContainerDragActive(false, null))
    }

    @Test
    fun `gesture commit and cancel retrace the same container geometry and content fade`() {
        val frame = SettingsContainerFrame()
        val phases = listOf<NavSettlePhase?>(null, NavSettlePhase.Commit, NavSettlePhase.Cancel)
        for (phase in phases) {
            val dragging = settingsContainerDragActive(phase == null, phase)
            for (raw in listOf(1f, 0.75f, 0.5f, 0.25f, 0f, 0.25f, 0.5f, 0.75f, 1f)) {
                frame.update(raw, returning = dragging)
                assertEquals(settingsContainerProgress(raw, true), frame.progress, 0.0001f)
                assertEquals(settingsContainerContentAlpha(raw, true), frame.alpha, 0.0001f)
            }
            frame.reset()
        }
    }

    @Test
    fun `container expansion is opt in and saved preference is preserved`() {
        assertFalse(AppNavigationSettings().settingsContainerTransitionEnabled)
        assertFalse(mapAppNavigationSettingsFromPreferences(mutablePreferencesOf()).settingsContainerTransitionEnabled)
        assertTrue(mapAppNavigationSettingsFromPreferences(mutablePreferencesOf(
            settingsContainerTransitionPreferencesKey to true,
        )).settingsContainerTransitionEnabled)
    }

    @Test
    fun `settings mode keeps ordinary navigation effects independent`() {
        val settings = mapAppNavigationSettingsFromPreferences(mutablePreferencesOf(
            settingsContainerTransitionPreferencesKey to true,
        ))
        assertTrue(settings.miuixTransitionBlurEnabled)
        assertTrue(settings.videoSharedReturnGestureFollowEnabled)
        assertTrue(settings.videoReturnContentFollowProgressEnabled)
        assertTrue(settings.videoSharedReturnGestureTranslationEnabled)
    }

    @Test
    fun `only one appended settings destination can own a container`() {
        val root = listOf(BiliPaiNavKey.MainHost)
        val child = root + BiliPaiNavKey.AppearanceSettings
        assertEquals(BiliPaiNavKey.AppearanceSettings, settingsContainerTarget(root, child))
        assertEquals(BiliPaiNavKey.AnimationSettings, settingsContainerTarget(child, child + BiliPaiNavKey.AnimationSettings))
        assertNull(settingsContainerTarget(root, root)) // Dialog / external action.
        assertNull(settingsContainerTarget(child, root)) // Back.
        assertNull(settingsContainerTarget(child, root + BiliPaiNavKey.AnimationSettings)) // Replacement.
        assertNull(settingsContainerTarget(root, child + BiliPaiNavKey.AnimationSettings)) // Multi-push.
        assertNull(settingsContainerTarget(root, root + BiliPaiNavKey.History))
        val video = BiliPaiNavKey.VideoDetail(bvid = "BV1example", cid = 1L)
        assertNull(settingsContainerTarget(root, root + video))
        assertNull(settingsContainerTarget(root, root + BiliPaiNavKey.Search))
        assertNull(settingsContainerTarget(root, root + BiliPaiNavKey.MainHost))
    }

    @Test
    fun `space search and playback stay outside settings container scope`() {
        assertTrue(settingsContainerDestination(BiliPaiNavKey.PlaybackSettings))
        assertFalse(settingsContainerDestination(BiliPaiNavKey.Space(mid = 1L)))
        assertFalse(settingsContainerDestination(BiliPaiNavKey.VideoDetail(bvid = "BV1example")))
        assertFalse(settingsContainerDestination(BiliPaiNavKey.Search))
        assertFalse(settingsContainerDestination(BiliPaiNavKey.AudioMode(sourceBvid = "BV1example")))
    }

    @Test
    fun `source corner shrinks monotonically and retraces identical radii on return`() {
        val initial = 24f
        var previous = initial
        for (step in 0..100) {
            val progress = step / 100f
            val radius = containerCornerRadius(initial, progress)
            assertTrue(radius <= previous)
            assertEquals(initial * (1f - progress), radius, 0.0001f)
            previous = radius
        }
        assertEquals(initial, containerCornerRadius(initial, 0f))
        assertEquals(0f, containerCornerRadius(initial, 1f))
        assertEquals(0f, containerCornerRadius(-4f, 0.5f))
    }

    @Test
    fun `background blur clears at rest and stays on the covered layer only`() {
        assertEquals(0f, containerBackgroundBlurProgress(-0.5f, true, true))
        assertEquals(0f, containerBackgroundBlurProgress(0.5f, false, true))
        assertEquals(0f, containerBackgroundBlurProgress(0.5f, true, false))
        assertEquals(0.25f, containerBackgroundBlurProgress(0.5f, true, true), 0.0001f)
        assertEquals(1f, containerBackgroundBlurProgress(1f, true, true))
    }

    @Test
    fun `opening and return preserve endpoints and the reverse curve`() {
        for (returning in listOf(false, true)) {
            assertEquals(0f, settingsContainerProgress(0f, returning), 0.0001f)
            assertEquals(1f, settingsContainerProgress(1f, returning), 0.0001f)
        }
        for (step in 0..100) {
            val t = step / 100f
            assertEquals(1f - settingsContainerProgress(1f - t, false), settingsContainerProgress(t, true), 0.0001f)
        }
        for (closing in listOf(false, true)) {
            assertEquals(0f, settingsContainerContentAlpha(0f, closing))
            assertEquals(1f, settingsContainerContentAlpha(1f, closing))
            for (step in 1..99) {
                val raw = step / 100f
                if (settingsContainerProgress(raw, closing) > 0.2f) {
                    assertTrue(settingsContainerContentAlpha(raw, closing) > 0f)
                }
            }
        }
    }

    @Test
    fun `back during opening continues from the current pixels and can cancel`() {
        val frame = SettingsContainerFrame()
        frame.update(0.5f, false)
        val progress = frame.progress
        val alpha = frame.alpha
        frame.update(0.5f, true)
        assertEquals(progress, frame.progress, 0.0001f)
        assertEquals(alpha, frame.alpha, 0.0001f)
        frame.update(0.25f, true)
        assertTrue(frame.progress < progress)
        frame.update(1f, true) // Cancel returns to a fully readable page.
        assertEquals(1f, frame.progress, 0.0001f)
        assertEquals(1f, frame.alpha, 0.0001f)
        frame.reset()
        frame.update(0f, true)
        assertEquals(0f, frame.progress, 0.0001f)
        assertEquals(0f, frame.alpha, 0.0001f)
    }
}
