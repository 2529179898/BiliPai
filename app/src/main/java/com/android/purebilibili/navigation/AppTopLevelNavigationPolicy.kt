package com.android.purebilibili.navigation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.android.purebilibili.feature.home.components.BottomNavItem
import com.android.purebilibili.navigation3.BiliPaiNavKey
import com.android.purebilibili.navigation3.toLegacyRoute

internal enum class TopLevelNavigationAction {
    SKIP,
    POP_EXISTING,
    NAVIGATE_WITH_RESTORE
}

internal enum class BottomBarSelectionAction {
    NAVIGATE,
    RESELECT
}

internal enum class AppSystemBackAction {
    RETURN_TO_HOME_TAB,
    NAVIGATE_UP,
    FINISH_ACTIVITY
}

internal data class BottomPagerRenderBudget(
    val isTransitionRunning: Boolean,
    val forceLowBlurBudget: Boolean,
    val deferProfileImmersiveBackground: Boolean
)

internal const val BOTTOM_TAB_RENDER_BUDGET_HOLD_MILLIS = 220L
internal const val BOTTOM_BAR_MAX_VISIBLE_ITEMS = 5
// 底栏最多有 5 个栏目；保留其余页的轻量布局槽，内容按各页预加载许可创建。
internal const val BOTTOM_PAGER_MAX_PRELOAD_DISTANCE = BOTTOM_BAR_MAX_VISIBLE_ITEMS - 1
// 预热只是「挂载组合树」，各页真正的数据与图片加载仍由 isBottomPagerPageActive /
// LocalPageImageLoadingAllowed 门控。因此可以尽早开始：原来的 500ms 起步 + 240ms 间隔
// 让 5 个栏目要约 1.5~2s 才全热，正好和用户「一回到前台就连点底栏」的窗口完全重叠。
// 真正的节流器是调度器里的 awaitFrame()（每帧最多一页），这里的间隔只是一帧的安全余量。
internal const val BOTTOM_PAGER_PRELOAD_IDLE_MILLIS = 120L
internal const val BOTTOM_PAGER_PRELOAD_INTERVAL_MILLIS = 16L

/**
 * 点击底栏后先让出若干帧，让目标页在动画启动前完成首次组合与测量。
 *
 * 目标页在 selectedPage 更新后立刻成为转场参与者并开始组合；如果动画与这次冷组合抢
 * 同一帧预算，首帧就会超时，表现为切页动画掉帧。让出一帧即可把这笔成本提前付掉。
 * 只让一帧：更多会让底栏指示器的手感明显变迟钝。
 */
internal const val BOTTOM_PAGER_TARGET_PREWARM_FRAMES = 1

/**
 * 已预热栏目集合的跨重建保留策略。
 *
 * 进程被系统回收后是冷启动，此时没有可恢复的集合，轻量首帧策略照旧生效；
 * 而配置变更 / Activity 重建属于「进程仍然是热的」，数据与 ViewModel 都还在内存里，
 * 重新挂载这些页既便宜又必要——否则重建后第一次点底栏又要冷组合整页 Feed。
 */
internal val BottomPagerPreloadedItemsSaver: Saver<Set<BottomNavItem>, Any> =
    listSaver(
        save = { items -> items.map { it.name } },
        restore = { names ->
            names.mapNotNull { name -> BottomNavItem.entries.firstOrNull { it.name == name } }
                .toSet()
        }
    )

internal fun shouldAllowBottomPagerBackgroundWork(
    isMainHostTop: Boolean,
    isCardTransitionIdle: Boolean,
    isPagerNavigating: Boolean,
): Boolean = isMainHostTop && isCardTransitionIdle && !isPagerNavigating

internal fun nextBottomPagerPreloadItem(
    visibleItems: List<BottomNavItem>,
    preloadedItems: Set<BottomNavItem>,
): BottomNavItem? = visibleItems.firstOrNull {
    it != BottomNavItem.STORY && it !in preloadedItems
}

internal fun resolveTopLevelNavigationAction(
    currentRoute: String?,
    targetRoute: String,
    hasTargetInBackStack: Boolean
): TopLevelNavigationAction {
    if (currentRoute == targetRoute) {
        return TopLevelNavigationAction.SKIP
    }

    if (hasTargetInBackStack) {
        return TopLevelNavigationAction.POP_EXISTING
    }

    return TopLevelNavigationAction.NAVIGATE_WITH_RESTORE
}

internal fun resolveBottomBarSelectionAction(
    currentItem: BottomNavItem,
    tappedItem: BottomNavItem
): BottomBarSelectionAction {
    return if (currentItem == tappedItem) {
        BottomBarSelectionAction.RESELECT
    } else {
        BottomBarSelectionAction.NAVIGATE
    }
}

internal fun shouldScrollToTopOnBottomBarReselect(item: BottomNavItem): Boolean {
    return when (item) {
        BottomNavItem.HOME,
        BottomNavItem.DYNAMIC,
        BottomNavItem.HISTORY,
        BottomNavItem.PROFILE,
        BottomNavItem.FAVORITE,
        BottomNavItem.LIVE,
        BottomNavItem.WATCHLATER -> true
        BottomNavItem.LISTEN_VIDEO,
        BottomNavItem.SETTINGS,
        BottomNavItem.PLUGINS,
        BottomNavItem.STORY -> false
    }
}

internal fun resolveAppSystemBackAction(
    isAtMainHostRoot: Boolean,
    currentBottomItem: BottomNavItem,
    homeItem: BottomNavItem = BottomNavItem.HOME
): AppSystemBackAction {
    if (!isAtMainHostRoot) {
        return AppSystemBackAction.NAVIGATE_UP
    }
    if (currentBottomItem != homeItem) {
        return AppSystemBackAction.RETURN_TO_HOME_TAB
    }
    return AppSystemBackAction.FINISH_ACTIVITY
}

internal fun shouldInterceptSystemBackForAppAction(
    action: AppSystemBackAction
): Boolean {
    return action == AppSystemBackAction.RETURN_TO_HOME_TAB
}

internal fun resolveBottomPagerPageForRoute(
    route: String?,
    visibleItems: List<BottomNavItem>
): Int? {
    val routeBase = route?.substringBefore("?") ?: return null
    return visibleItems.indexOfFirst { item -> item.route == routeBase }
        .takeIf { it >= 0 }
}

internal fun resolveBottomPagerItemForPage(
    page: Int,
    visibleItems: List<BottomNavItem>
): BottomNavItem {
    return visibleItems.getOrNull(page) ?: BottomNavItem.HOME
}

internal fun shouldResetNavigation3BackStackForBottomPager(
    currentStack: List<BiliPaiNavKey>
): Boolean {
    // Sibling tabs switch entirely inside the pager. BiliPai only needs Nav3 here when leaving a
    // secondary destination; rebuilding an existing MainHost invalidates the
    // composable that currently owns the pager mutation.
    return currentStack.size != 1 || currentStack.singleOrNull() != BiliPaiNavKey.MainHost
}

internal fun resolveVisibleBottomBarItems(
    orderedVisibleTabIds: List<String>
): List<BottomNavItem> {
    return orderedVisibleTabIds
        .mapNotNull { id -> BottomNavItem.entries.find { it.name == id } }
        .distinct()
        .take(BOTTOM_BAR_MAX_VISIBLE_ITEMS)
        .ifEmpty { listOf(BottomNavItem.HOME) }
}

internal fun resolveActiveBottomTabRoute(
    currentKey: BiliPaiNavKey?,
    currentBottomItem: BottomNavItem
): String? {
    if (currentKey == null || currentKey == BiliPaiNavKey.MainHost) {
        return currentBottomItem.route
    }
    val route = currentKey.toLegacyRoute()
    return if (route == BiliPaiNavKey.MainHost.routeBase) currentBottomItem.route else route
}

internal fun shouldShowBottomBarForNavigation(
    activeRoute: String?,
    visibleBottomBarRoutes: Set<String>,
    useSideNavigation: Boolean,
    shouldHideBottomBarOnTablet: Boolean,
    shouldDeferReveal: Boolean
): Boolean {
    return !activeRoute.orEmpty().startsWith("story") &&
        activeRoute in visibleBottomBarRoutes &&
        !useSideNavigation &&
        !shouldHideBottomBarOnTablet &&
        !shouldDeferReveal
}

internal fun shouldMountSidebarForNavigation(
    routeAllowsSidebar: Boolean,
    isVideoDetailDestination: Boolean
): Boolean = routeAllowsSidebar && !isVideoDetailDestination

internal fun resolveVideoCardSourceRouteForNavigation(
    currentRoute: String?,
    videoBvid: String,
    lastClickedVideoSourceKey: String?,
    visibleBottomBarRoutes: Set<String>
): String? {
    if (videoBvid.isBlank() || lastClickedVideoSourceKey.isNullOrBlank()) return null
    val routeBase = normalizeVideoCardNavigationSourceRoute(currentRoute)
    val currentRouteMatch = routeBase
        ?.takeIf { route -> lastClickedVideoSourceKey == "$route:$videoBvid" }
    if (currentRouteMatch != null) return currentRouteMatch

    // Bottom-bar tabs first (MainHost top is not the card host).
    visibleBottomBarRoutes.firstOrNull { route ->
        lastClickedVideoSourceKey == "$route:$videoBvid"
    }?.let { return it }

    // Search / Space / History / video-related / collection hosts are not bottom-bar routes.
    // Still honor the recorded card key so predictive-back sharedBounds land on the same route.
    return resolveClickedVideoSourceRoute(lastClickedVideoSourceKey, videoBvid)
}

private fun normalizeVideoCardNavigationSourceRoute(route: String?): String? {
    val normalized = route?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return if (normalized.startsWith("home?category=")) {
        ScreenRoutes.Home.route
    } else {
        normalized.substringBefore("?")
    }
}

private fun resolveClickedVideoSourceRoute(
    lastClickedVideoSourceKey: String,
    videoBvid: String
): String? {
    val expectedSuffix = ":$videoBvid"
    return lastClickedVideoSourceKey
        .takeIf { it.endsWith(expectedSuffix) }
        ?.removeSuffix(expectedSuffix)
        ?.takeIf { it.isNotBlank() }
}

internal fun resolveBottomPagerSaveableStateKey(item: BottomNavItem): String {
    return "bottom:${item.route}"
}

internal fun resolveBottomPagerLayoutKey(page: Int, visibleItems: List<BottomNavItem>): String {
    // 设置缩减底栏时，Pager 的旧布局/预取仍可能查询已移除的页。
    // 临时空槽必须有独立键，不能复用首页键造成重复。
    return visibleItems.getOrNull(page)?.let(::resolveBottomPagerSaveableStateKey)
        ?: "bottom:removed-slot:$page"
}

internal fun resolveBottomPagerNavigationDurationMillis(pageDistance: Int): Int {
    val distance = pageDistance.coerceAtLeast(2)
    return distance * 100 + 100
}

internal fun resolveBottomPagerBeyondViewportPageCount(
    pageCount: Int,
    contentReady: Boolean
): Int {
    if (!contentReady) return 0
    return pageCount.coerceIn(1, BOTTOM_BAR_MAX_VISIBLE_ITEMS) - 1
}

internal fun resolveBottomPagerRenderBudget(isNavigating: Boolean): BottomPagerRenderBudget {
    return BottomPagerRenderBudget(
        isTransitionRunning = isNavigating,
        forceLowBlurBudget = isNavigating,
        deferProfileImmersiveBackground = isNavigating
    )
}

internal fun shouldEnableBottomPagerUserScroll(): Boolean = false

/**
 * BiliPai MainScreen composition:
 * `if (isCurrentPage || contentReady) XxxPager(...)`
 *
 * After an individual page is ready, its bottom-tab slot stays mounted so
 * [MainBottomPagerState.switchToPage] `animateScrollBy` far jumps
 * (rightmost → home) scroll across real pages instead of empty Boxes.
 * Story is intentionally excluded while inactive: mounting it creates a real media player and
 * playback loading session, which is not safe or useful as visual-only pager precomposition.
 *
 * Before ready, only mount start / selected / current to keep cold start light.
 */
internal fun shouldComposeBottomPagerPage(
    item: BottomNavItem,
    page: Int,
    currentPage: Int,
    selectedPage: Int,
    isNavigating: Boolean,
    navigationStartPage: Int,
    contentReady: Boolean
): Boolean {
    val isTransitionParticipant = page == currentPage ||
        page == selectedPage ||
        page == navigationStartPage
    if (item == BottomNavItem.STORY) {
        return isTransitionParticipant
    }
    if (contentReady) {
        return true
    }
    return isTransitionParticipant
}

internal fun shouldBypassNavigationDebounceForRoute(targetRoute: String): Boolean {
    return BottomNavItem.entries.any { item -> item.route == targetRoute }
}

internal fun canProceedWithNavigation(
    currentTimeMillis: Long,
    lastNavigationTimeMillis: Long,
    debounceWindowMillis: Long,
    bypassDebounce: Boolean
): Boolean {
    return bypassDebounce || currentTimeMillis - lastNavigationTimeMillis > debounceWindowMillis
}

internal fun shouldPreserveProfileStackForShortcut(targetRoute: String): Boolean {
    return targetRoute == ScreenRoutes.Settings.route ||
        targetRoute == ScreenRoutes.History.route ||
        targetRoute == ScreenRoutes.Favorite.route ||
        targetRoute == ScreenRoutes.LikedVideos.route ||
        targetRoute == ScreenRoutes.WatchLater.route ||
        targetRoute == ScreenRoutes.DownloadList.route ||
        targetRoute == ScreenRoutes.Inbox.route ||
        targetRoute == ScreenRoutes.Following.route ||
        targetRoute.startsWith("following/")
}
