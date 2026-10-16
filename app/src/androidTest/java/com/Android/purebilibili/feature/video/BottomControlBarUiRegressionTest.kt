package com.Android.purebilibili.feature.video

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.subtitle.SubtitleDisplayMode
import com.android.purebilibili.feature.video.subtitle.SubtitleTrackOption
import com.android.purebilibili.feature.video.ui.overlay.BottomControlBar
import com.android.purebilibili.feature.video.ui.overlay.PlayerProgress
import com.android.purebilibili.feature.video.ui.overlay.SubtitleControlCallbacks
import com.android.purebilibili.feature.video.ui.overlay.SubtitleControlUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomControlBarUiRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun hiResOnCompactPhone_showsOneAudioLabelAndClickableFullscreen() {
        verifyFullscreenOnCompactPhone(fontScale = 1f)
        composeTestRule.onAllNodesWithText("Hi-Res").assertCountEquals(1)
    }

    @Test
    fun hiResWithLargerFont_keepsFullscreenInsidePlayer() {
        verifyFullscreenOnCompactPhone(fontScale = 1.5f)
    }

    @Test
    fun longSubtitleList_canScrollToAiChineseAndSelectIt() {
        val chineseTrack = SubtitleTrackOption(
            trackKey = "ai-zh",
            languageCode = "ai-zh",
            label = "中文（自动生成）",
            selected = false,
            likelyAi = true,
        )
        val tracks = List(40) { index ->
            SubtitleTrackOption(
                trackKey = "track-$index",
                languageCode = "lang-$index",
                label = "语言 $index",
                selected = index == 0,
                likelyAi = false,
            )
        } + chineseTrack
        var selectedTrackKey: String? = null
        var selectedMode = SubtitleDisplayMode.OFF
        composeTestRule.setContent {
            MaterialTheme {
                BottomControlBar(
                    isPlaying = false,
                    isFullscreen = true,
                    onPlayPauseClick = {},
                    onSeek = {},
                    onToggleFullscreen = {},
                    subtitleControlState = SubtitleControlUiState(
                        trackAvailable = true,
                        primaryAvailable = true,
                        primaryLabel = "语言 0",
                        trackOptions = tracks,
                    ),
                    subtitleControlCallbacks = SubtitleControlCallbacks(
                        onTrackSelected = { selectedTrackKey = it },
                        onDisplayModeChange = { selectedMode = it },
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("字幕").performClick()
        composeTestRule.onNodeWithText("锁定位置", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(chineseTrack.label)
            .performScrollTo()
            .assertIsDisplayed()
            .performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertEquals(chineseTrack.trackKey, selectedTrackKey)
            assertEquals(SubtitleDisplayMode.PRIMARY_ONLY, selectedMode)
        }
        composeTestRule.onNodeWithText("字幕显示").assertDoesNotExist()
    }

    private fun verifyFullscreenOnCompactPhone(fontScale: Float) {
        var fullscreenClicked = false
        composeTestRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(360.dp).testTag("inline_player_controls")) {
                        BottomControlBar(
                            isPlaying = false,
                            progress = PlayerProgress(current = 115_000L, duration = 197_000L),
                            isFullscreen = false,
                            viewportWidthDpOverride = 360,
                            currentAudioQualityLabel = "Hi-Res",
                            isHiResAudioSelected = true,
                            currentQualityLabel = "1080P60",
                            onPlayPauseClick = {},
                            onSeek = {},
                            onToggleFullscreen = { fullscreenClicked = true },
                        )
                    }
                }
            }
        }
        val fullscreen = composeTestRule.onNodeWithTag("player_fullscreen_toggle")
        fullscreen.assertIsDisplayed().assertWidthIsEqualTo(40.dp)
        val playerBounds = composeTestRule.onNodeWithTag("inline_player_controls")
            .fetchSemanticsNode().boundsInRoot
        val fullscreenBounds = fullscreen.fetchSemanticsNode().boundsInRoot
        assertTrue(fullscreenBounds.left >= playerBounds.left)
        assertTrue(fullscreenBounds.right <= playerBounds.right)
        fullscreen.performTouchInput { click(center) }
        composeTestRule.runOnIdle { assertTrue(fullscreenClicked) }
    }
}
