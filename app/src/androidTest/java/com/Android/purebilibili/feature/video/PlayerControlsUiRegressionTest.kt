package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.feature.video.ui.overlay.BottomControlBar
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerControlsUiRegressionTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun seekRegionRemainsGestureSafeWithSinglePlaybackIdentityAcrossMaterialsAndWidths() {
        val width = mutableStateOf(960)
        val fullscreen = mutableStateOf(false)
        val glass = mutableStateOf(false)
        rule.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = LocalDensity.current.density * maxWidth.value / 960f
                CompositionLocalProvider(
                    LocalDensity provides Density(density),
                    LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                ) {
                    MaterialTheme {
                        Box(Modifier.width(width.value.dp)) {
                            BottomControlBar(
                                isPlaying = true,
                                progress = PlayerProgress(30_000, 120_000),
                                isFullscreen = fullscreen.value,
                                viewportWidthDpOverride = width.value,
                                currentQualityLabel = "1080P",
                                onPlayPauseClick = {}, onSeek = {}, onToggleFullscreen = {},
                            )
                        }
                    }
                }
            }
        }
        for (widthDp in listOf(320, 500, 720, 960)) {
            for (isFullscreen in listOf(false, true)) {
                rule.runOnIdle { width.value = widthDp; fullscreen.value = isFullscreen; glass.value = false }
                val tags = listOf("player_progress", "player_progress_seek_area", "player_control_row", "player_play_pause", "player_fullscreen_toggle")
                val baseline = tags.associateWith { rule.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
                for (useGlass in listOf(false, true)) {
                    rule.runOnIdle { glass.value = useGlass }
                    rule.onAllNodesWithTag("player_play_pause").assertCountEquals(1)
                    rule.onAllNodesWithContentDescription("暂停").assertCountEquals(1)
                    val bounds = tags.associateWith { rule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
                    val controls = bounds.getValue("player_control_row")
                    val seek = bounds.getValue("player_progress_seek_area")
                    val play = bounds.getValue("player_play_pause")
                    val fullscreenButton = bounds.getValue("player_fullscreen_toggle")
                    assertTrue(play.center.x < controls.center.x)
                    assertTrue(play.right <= fullscreenButton.left)
                    assertTrue(bounds.getValue("player_progress").bottom <= controls.top)
                    val pxPerDp = controls.width / (widthDp - 20f)
                    // The control row has 10dp outer padding; seek keeps at least 20dp window-edge exclusion.
                    assertTrue(seek.left >= controls.left + 10f * pxPerDp - 1f)
                    assertTrue(seek.right <= controls.right - 10f * pxPerDp + 1f)
                    baseline.forEach { (tag, expected) -> assertEquals("Glass moved $tag", expected, bounds.getValue(tag)) }
                }
            }
        }
    }
}
