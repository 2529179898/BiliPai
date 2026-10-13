package com.android.purebilibili.navigation

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

/**
 * 让出若干帧，让即将成为动画目标的那一页先完成首次组合与测量。
 *
 * 目标页在 `selectedPage` 更新后立刻成为转场参与者并开始组合。若动画与这次冷组合抢
 * 同一帧预算，动画首帧就会超时——这正是「切回前台后点底栏掉帧」最直接的成因。
 * 提前让出 [BOTTOM_PAGER_TARGET_PREWARM_FRAMES] 帧即可把这笔成本付在动画之前。
 */
internal suspend fun awaitPagerTargetPagePrewarm(
    maxFrames: Int = BOTTOM_PAGER_TARGET_PREWARM_FRAMES,
) {
    repeat(maxFrames.coerceAtLeast(0)) { withFrameNanos { } }
}

/**
 * Moves a tab-backed pager through every intermediate page.
 *
 * Compose's animateScrollToPage may pre-jump for distant targets. That optimization is useful
 * for content carousels, but makes a tab indicator appear to hard-cut. Tab rails use this helper
 * so page content and indicators share one continuous position. The main bottom navigation and
 * every tab rail that opts into progressive page traversal call this same implementation.
 */
internal suspend fun animatePagerSelection(
    pagerState: PagerState,
    targetPage: Int,
    animationSpec: FiniteAnimationSpec<Float>? = null,
) {
    val lastPage = pagerState.pageCount - 1
    if (lastPage < 0) return
    val safeTargetPage = targetPage.coerceIn(0, lastPage)
    if (
        safeTargetPage == pagerState.currentPage &&
        abs(pagerState.currentPageOffsetFraction) <= 0.001f
    ) {
        return
    }

    val pageDistance = abs(safeTargetPage - pagerState.currentPage).coerceAtLeast(1)
    val resolvedSpec = animationSpec ?: tween(
        durationMillis = resolveBottomPagerNavigationDurationMillis(pageDistance),
        easing = EaseInOut,
    )

    pagerState.scroll(MutatePriority.UserInput) {
        val pageSizePx = pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
        if (pageSizePx <= 0) return@scroll
        val remainingPages = safeTargetPage -
            pagerState.currentPage -
            pagerState.currentPageOffsetFraction
        val scrollDistancePx = remainingPages * pageSizePx
        var consumedPx = 0f
        animate(
            initialValue = 0f,
            targetValue = scrollDistancePx,
            animationSpec = resolvedSpec,
        ) { value, _ ->
            consumedPx += scrollBy(value - consumedPx)
        }
    }

    if (pagerState.currentPage != safeTargetPage ||
        abs(pagerState.currentPageOffsetFraction) > 0.001f
    ) {
        pagerState.scrollToPage(safeTargetPage)
    }
}
