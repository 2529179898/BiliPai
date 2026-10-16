package com.android.purebilibili.feature.video

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.ui.gesture.detectPlayerTapGestures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerTapGesturesUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var viewConfiguration: ViewConfiguration
    private val taps = mutableListOf<Offset>()
    private val doubleTaps = mutableListOf<Offset>()
    private var controlsVisible = false
    private var paused = false
    private var playbackSpeed = 1f
    private var longPressStarts = 0

    private fun setPlayerSurface() {
        rule.setContent {
            viewConfiguration = LocalViewConfiguration.current
            Box(
                Modifier
                    .size(300.dp, 180.dp)
                    .testTag("player-tap-surface")
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                playbackSpeed = 2f
                                longPressStarts += 1
                            },
                            onDragEnd = { playbackSpeed = 1f },
                            onDragCancel = { playbackSpeed = 1f },
                            onDrag = { change, _ -> change.consume() }
                        )
                    }
                    .pointerInput(Unit) {
                        detectPlayerTapGestures(
                            onTap = {
                                taps += it
                                controlsVisible = !controlsVisible
                            },
                            onDoubleTap = {
                                doubleTaps += it
                                paused = !paused
                            }
                        )
                    }
            )
        }
    }

    private fun finishTapWindow() {
        rule.mainClock.advanceTimeBy(viewConfiguration.doubleTapTimeoutMillis + 100L)
        rule.waitForIdle()
    }

    @Test
    fun longPressRelease_thenImmediateTap_onlyShowsControls() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            longClick(center, durationMillis = viewConfiguration.longPressTimeoutMillis + 100L)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, longPressStarts)
            assertEquals(1f, playbackSpeed, 0f)
            assertTrue(controlsVisible)
            assertFalse(paused)
            assertEquals(1, taps.size)
            assertTrue(doubleTaps.isEmpty())
        }
    }

    @Test
    fun nearbyShortTaps_onlyTogglePlayback() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val second = center + Offset(viewConfiguration.touchSlop / 2f, 0f)
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(second)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertTrue(paused)
            assertFalse(controlsVisible)
            assertTrue(taps.isEmpty())
            assertEquals(1, doubleTaps.size)
            assertEquals(0, longPressStarts)
        }
    }

    @Test
    fun oppositeSides_areTwoIndependentTaps() {
        setPlayerSurface()
        var left = Offset.Zero
        var right = Offset.Zero
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            left = Offset(width * 0.1f, center.y)
            right = Offset(width * 0.9f, center.y)
            click(left)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(right)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(listOf(left, right), taps)
            assertFalse(controlsVisible)
            assertFalse(paused)
            assertTrue(doubleTaps.isEmpty())
        }
    }

    @Test
    fun distantSecondTap_canStartNearbyDoubleTap() {
        setPlayerSurface()
        var left = Offset.Zero
        var third = Offset.Zero
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            left = Offset(width * 0.1f, center.y)
            val right = Offset(width * 0.9f, center.y)
            third = right - Offset(viewConfiguration.touchSlop / 2f, 0f)
            click(left)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(right)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(third)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(listOf(left), taps)
            assertEquals(listOf(third), doubleTaps)
            assertTrue(controlsVisible)
            assertTrue(paused)
        }
    }

    @Test
    fun diagonalDistanceOutsideSlop_isNotDoubleTap() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            val delta = viewConfiguration.touchSlop * 0.8f
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center + Offset(delta, delta))
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(2, taps.size)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }

    @Test
    fun nearbySecondLongPress_thenTap_doesNotPausePlayback() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            longClick(center, durationMillis = viewConfiguration.longPressTimeoutMillis + 100L)
            advanceEventTime(viewConfiguration.doubleTapMinTimeMillis + 20L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(1, longPressStarts)
            assertEquals(1f, playbackSpeed, 0f)
            assertEquals(2, taps.size)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }

    @Test
    fun nearbyTapsOutsideTimeWindow_areIndependent() {
        setPlayerSurface()
        rule.onNodeWithTag("player-tap-surface").performTouchInput {
            click(center)
            advanceEventTime(viewConfiguration.doubleTapTimeoutMillis + 100L)
            click(center)
        }
        finishTapWindow()

        rule.runOnIdle {
            assertEquals(2, taps.size)
            assertTrue(doubleTaps.isEmpty())
            assertFalse(paused)
        }
    }
}
