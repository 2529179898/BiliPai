package com.android.purebilibili.feature.video

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppThemeConfig
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.feature.video.screen.isVideoContentCommentListAtTop
import com.android.purebilibili.feature.video.ui.components.CommentSortHeader
import com.android.purebilibili.feature.video.ui.components.VideoCommentFloatingHeader
import com.android.purebilibili.feature.video.viewmodel.CommentSortMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoCommentHeaderScrollUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun materialFixedHeaderTracksScrollAndMaterialChanges() = verifyHeader(AppUiStyle.MATERIAL3, false)

    @Test
    fun miuixFixedHeaderTracksScrollAndMaterialChanges() = verifyHeader(AppUiStyle.MIUIX, false)

    @Test
    fun materialFloatingHeaderTracksScrollAndMaterialChanges() = verifyHeader(AppUiStyle.MATERIAL3, true)

    @Test
    fun miuixFloatingHeaderTracksScrollAndMaterialChanges() = verifyHeader(AppUiStyle.MIUIX, true)

    private fun verifyHeader(style: AppUiStyle, floating: Boolean) {
        val glass = mutableStateOf(true)
        val currentList = mutableStateOf(LazyListState())
        val title = if (!floating && style == AppUiStyle.MIUIX) "评论" else "最新评论"
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalAppUiStyle provides style,
                    LocalAppThemeConfig provides AppThemeConfig(liquidGlassEnabled = glass.value),
                ) {
                    val listState = currentList.value
                    val listAtTop by remember(listState) {
                        derivedStateOf {
                            isVideoContentCommentListAtTop(
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset,
                            )
                        }
                    }
                    Column(Modifier.width(280.dp)) {
                        // Floating hosts reserve the header inset independently of visibility.
                        androidx.compose.foundation.layout.Box(
                            if (floating) Modifier.height(46.dp) else Modifier,
                        ) {
                            if (floating) {
                                VideoCommentFloatingHeader(
                                    count = 427,
                                    sortMode = CommentSortMode.NEWEST,
                                    onSortModeChange = {},
                                    onSearchClick = {},
                                    backdrop = null,
                                    liquidGlassEnabled = glass.value,
                                    listAtTop = listAtTop,
                                    motionEnabled = false,
                                    compactTitleTypography = true,
                                )
                            } else {
                                CommentSortHeader(
                                    count = 427,
                                    sortMode = CommentSortMode.NEWEST,
                                    onSortModeChange = {},
                                    onSearchClick = {},
                                    compactTypography = true,
                                    scrollState = listState,
                                )
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth().height(200.dp).testTag("rows"),
                        ) {
                            items(40) { Text("评论内容 $it", Modifier.height(48.dp)) }
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        val initialListTop = composeRule.onNodeWithTag("rows").fetchSemanticsNode().boundsInRoot.top
        composeRule.onNodeWithTag("rows").performScrollToIndex(3)
        composeRule.onNodeWithText(title).assertIsNotDisplayed()
        composeRule.onNodeWithContentDescription("搜索评论").assertIsNotDisplayed()
        assertEquals(initialListTop, composeRule.onNodeWithTag("rows").fetchSemanticsNode().boundsInRoot.top, 0.5f)

        // Turning glass off while already scrolled restores the entire native header.
        composeRule.runOnIdle { glass.value = false }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("搜索评论").assertIsDisplayed()
        composeRule.runOnIdle { glass.value = true }
        composeRule.onNodeWithText(title).assertIsNotDisplayed()
        composeRule.onNodeWithTag("rows").performScrollToIndex(0)
        composeRule.onNodeWithText(title).assertIsDisplayed()

        // Switching videos replaces the state; only the new list may drive visibility.
        composeRule.onNodeWithTag("rows").performScrollToIndex(3)
        composeRule.runOnIdle { currentList.value = LazyListState() }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithTag("rows").performScrollToIndex(2)
        composeRule.onNodeWithText(title).assertIsNotDisplayed()
    }
}
