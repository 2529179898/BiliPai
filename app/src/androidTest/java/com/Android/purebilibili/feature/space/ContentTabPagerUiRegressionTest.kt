package com.Android.purebilibili.feature.space

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.core.ui.common.contentTabPagerGestures
import com.android.purebilibili.core.ui.common.rememberContentTabPager
import com.android.purebilibili.core.ui.components.AppHorizontalPager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentTabPagerUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var pager: PagerState
    private val selection = mutableIntStateOf(0)

    private fun showPager(headerHeightPx: Float = 0f) {
        rule.setContent {
            MaterialTheme {
                pager = rememberContentTabPager(
                    pageCount = 3,
                    selectedPage = selection.intValue,
                    onPageSelected = { selection.intValue = it },
                    prepareNeighbors = {},
                )
                AppHorizontalPager(
                    state = pager,
                    userScrollEnabled = false,
                    modifier = Modifier.size(300.dp, 400.dp).testTag("pager")
                        .contentTabPagerGestures(pager, enabled = true) { headerHeightPx },
                ) { page ->
                    Box(Modifier.fillMaxSize().testTag("page_$page"))
                }
            }
        }
    }

    @Test
    fun dragging_exposesNeighborBeforeSelectionCommits() {
        showPager()
        rule.onNodeWithTag("pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.4f, 0f), delayMillis = 100)
        }
        rule.runOnIdle {
            val position = pager.currentPage + pager.currentPageOffsetFraction
            assertTrue(position > 0.1f && position < 0.9f)
            assertEquals(0, selection.intValue)
        }
        rule.onNodeWithTag("page_1").assertIsDisplayed()
        rule.onNodeWithTag("pager").performTouchInput {
            advanceEventTime(500)
            up()
        }
        rule.runOnIdle {
            assertEquals(1, selection.intValue)
            assertEquals(0f, pager.currentPageOffsetFraction, 0.001f)
        }
    }

    @Test
    fun tabClick_traversesContentContinuously() {
        showPager()
        rule.mainClock.autoAdvance = false
        rule.runOnIdle { selection.intValue = 1 }
        rule.mainClock.advanceTimeBy(100)
        rule.runOnIdle {
            val position = pager.currentPage + pager.currentPageOffsetFraction
            assertTrue(position > 0f && position < 1f)
        }
        rule.mainClock.advanceTimeBy(1_000)
        rule.runOnIdle {
            assertEquals(1, pager.settledPage)
            assertEquals(0f, pager.currentPageOffsetFraction, 0.001f)
        }
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun shortSlowDrag_returnsToOriginalPage() {
        showPager()
        rule.onNodeWithTag("pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.1f, 0f), delayMillis = 200)
            advanceEventTime(500)
            up()
        }
        rule.runOnIdle {
            assertEquals(0, selection.intValue)
            assertEquals(0f, pager.currentPageOffsetFraction, 0.001f)
        }
    }

    @Test
    fun dragStartingInHeader_doesNotMoveContentPager() {
        showPager(headerHeightPx = Float.POSITIVE_INFINITY)
        rule.onNodeWithTag("pager").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.6f, 0f), delayMillis = 100)
            up()
        }
        rule.runOnIdle {
            assertEquals(0, selection.intValue)
            assertEquals(0f, pager.currentPageOffsetFraction, 0.001f)
        }
    }
}
