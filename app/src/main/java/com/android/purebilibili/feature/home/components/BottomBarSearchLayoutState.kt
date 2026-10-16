package com.android.purebilibili.feature.home.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.android.purebilibili.core.store.BottomBarSearchLayoutMode
import com.android.purebilibili.core.ui.AppSpacingTokens

internal data class BiliPaiBottomBarSearchLayoutState(
    val dockWidth: State<Dp>,
    val dockHeight: State<Dp>,
    val minimumIndicatorWidth: Dp,
    val indicatorReferenceWidth: Dp,
    val searchWidth: State<Dp>,
    val searchHeight: State<Dp>,
    val searchGap: State<Dp>,
    val launchAdjustedSearchGap: State<Dp>,
    val shellHeight: State<Dp>
)

@Composable
internal fun rememberBiliPaiBottomBarSearchLayoutState(
    containerWidth: Dp,
    itemCount: Int,
    minEdgePadding: Dp,
    searchEnabled: Boolean,
    searchExpanded: Boolean,
    labelMode: Int,
    searchLayoutMode: BottomBarSearchLayoutMode,
    hasUiSkinDecoration: Boolean,
    fillAllocatedDockWidth: Boolean = false,
): BiliPaiBottomBarSearchLayoutState {
    val targetDockHeight = resolveBiliPaiBottomBarDockHeight(
        searchExpanded = searchExpanded,
        hasUiSkinDecoration = hasUiSkinDecoration,
    )
    val resolvedSearchLayout = resolveBiliPaiBottomBarSearchLayout(
        containerWidth = containerWidth,
        itemCount = itemCount,
        minEdgePadding = minEdgePadding,
        searchEnabled = searchEnabled,
        searchExpanded = searchExpanded,
        labelMode = labelMode,
        cornerRadius = targetDockHeight / 2,
        searchLayoutMode = searchLayoutMode
    )
    // Embedded navigation receives its final width from LinkedBottomDock. Applying
    // preferred widths and outer margins again would inset only the lower capsule.
    val targetSearchLayout = if (fillAllocatedDockWidth && !searchEnabled) {
        resolvedSearchLayout.copy(dockWidth = containerWidth)
    } else resolvedSearchLayout
    val emptySize = remember { mutableStateOf(AppSpacingTokens.None) }
    if (!searchEnabled) {
        val dockWidth = animateDpAsState(
            targetValue = targetSearchLayout.dockWidth,
            animationSpec = bottomBarDockWidthMotionSpec(),
            label = "bottomBarDockWidth"
        )
        val dockHeight = animateDpAsState(
            targetValue = targetDockHeight,
            animationSpec = bottomBarChromeHeightMotionSpec(),
            label = "bottomBarDockHeight"
        )
        return remember(
            dockWidth, dockHeight, targetSearchLayout.minimumIndicatorWidth,
            targetSearchLayout.indicatorReferenceWidth, emptySize,
        ) {
            BiliPaiBottomBarSearchLayoutState(
                dockWidth = dockWidth,
                dockHeight = dockHeight,
                minimumIndicatorWidth = targetSearchLayout.minimumIndicatorWidth,
                indicatorReferenceWidth = targetSearchLayout.indicatorReferenceWidth,
                searchWidth = emptySize,
                searchHeight = emptySize,
                searchGap = emptySize,
                launchAdjustedSearchGap = emptySize,
                shellHeight = dockHeight
            )
        }
    }

    val dockWidth = animateDpAsState(
        targetValue = targetSearchLayout.dockWidth,
        animationSpec = bottomBarDockWidthMotionSpec(),
        label = "bottomBarDockWidth"
    )
    val searchWidth = animateDpAsState(
        targetValue = targetSearchLayout.searchWidth,
        animationSpec = bottomBarDockWidthMotionSpec(),
        label = "bottomBarSearchWidth"
    )
    val searchGap = animateDpAsState(
        targetValue = targetSearchLayout.gap,
        animationSpec = bottomBarSearchGapMotionSpec(),
        label = "bottomBarSearchGap"
    )
    val dockHeight = animateDpAsState(
        targetValue = targetDockHeight,
        animationSpec = bottomBarChromeHeightMotionSpec(),
        label = "bottomBarDockHeight"
    )
    val searchHeight = animateDpAsState(
        targetValue = resolveBiliPaiBottomBarSearchHeight(
            searchExpanded = searchExpanded
        ),
        animationSpec = bottomBarChromeHeightMotionSpec(),
        label = "bottomBarSearchHeight"
    )
    val shellHeight = remember(dockHeight, searchHeight) {
        derivedStateOf { maxOf(dockHeight.value, searchHeight.value) }
    }
    return remember(
        dockWidth, dockHeight, searchWidth, searchHeight, searchGap, shellHeight,
        targetSearchLayout.minimumIndicatorWidth, targetSearchLayout.indicatorReferenceWidth,
    ) {
        BiliPaiBottomBarSearchLayoutState(
            dockWidth = dockWidth,
            dockHeight = dockHeight,
            minimumIndicatorWidth = targetSearchLayout.minimumIndicatorWidth,
            indicatorReferenceWidth = targetSearchLayout.indicatorReferenceWidth,
            searchWidth = searchWidth,
            searchHeight = searchHeight,
            searchGap = searchGap,
            launchAdjustedSearchGap = searchGap,
            shellHeight = shellHeight
        )
    }
}

/** Read animated sizes in measurement, with the same parent constraints as width/height. */
internal fun Modifier.bottomBarAnimatedSize(
    width: State<Dp>? = null,
    height: State<Dp>? = null,
): Modifier = layout { measurable, constraints ->
    val widthPx = width?.value?.roundToPx()?.coerceIn(constraints.minWidth, constraints.maxWidth)
    val heightPx = height?.value?.roundToPx()?.coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = widthPx ?: constraints.minWidth,
            maxWidth = widthPx ?: constraints.maxWidth,
            minHeight = heightPx ?: constraints.minHeight,
            maxHeight = heightPx ?: constraints.maxHeight,
        )
    )
    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
}

/** FloatingBottomBar's material geometry still needs a value; confine that read here. */
@Composable
internal fun BottomBarDockHeightContent(
    height: State<Dp>,
    content: @Composable (Dp) -> Unit,
) {
    content(height.value)
}
