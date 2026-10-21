package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
enum class PlayerListPopupPlacement { CENTER, START, END, END_BOTTOM, ANCHOR_END }

/** Player-owned PiliPlus selector. Neither application material nor glass changes its geometry. */
@Composable
fun PlayerSelectionPopup(
    title: String,
    showTitle: Boolean = true,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    maxHeight: Dp? = 440.dp,
    minWidth: Dp = 120.dp,
    anchorBounds: IntRect? = null,
    scrollState: ScrollState = rememberScrollState(),
    maxWidth: Dp = 288.dp,
    content: @Composable () -> Unit,
) {
    if (placement == PlayerListPopupPlacement.ANCHOR_END && anchorBounds == null) return
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val size = LocalWindowInfo.current.containerSize
    val insets = WindowInsets.safeDrawing
    val left = insets.getLeft(density, direction).coerceIn(0, size.width)
    val top = insets.getTop(density).coerceIn(0, size.height)
    val safe = IntRect(left, top,
        (size.width - insets.getRight(density, direction)).coerceIn(left, size.width),
        (size.height - insets.getBottom(density)).coerceIn(top, size.height))
    val gap = with(density) { 4.dp.roundToPx() }
    val viewport = with(density) {
        resolvePlayerPopupViewport(safe, maxHeight?.roundToPx(), 4.dp.roundToPx(), 4.dp.roundToPx(),
            anchorBounds.takeIf { placement == PlayerListPopupPlacement.ANCHOR_END }, gap)
    }
    if (viewport.bounds.width <= 0 || viewport.maxHeight <= 0) return
    val provider = remember(viewport, placement, anchorBounds, gap) {
        val measuredAnchor = anchorBounds
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize,
                layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
                resolvePlayerPopupOffset(viewport.bounds, popupContentSize, placement,
                    measuredAnchor ?: anchorBounds, layoutDirection, gap)
        }
    }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, clippingEnabled = false)) {
        val width = with(density) { viewport.bounds.width.toDp() }.coerceAtMost(maxWidth)
        Column(modifier
            .widthIn(min = minWidth.coerceAtMost(width), max = width)
            .width(IntrinsicSize.Max)
            .heightIn(max = with(density) { viewport.maxHeight.toDp() })
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.8f))
            .clipToBounds()
            .semantics { paneTitle = title }
            .verticalScroll(scrollState, overscrollEffect = null)
            .padding(vertical = 5.dp)) {
            if (showTitle) PlayerSelectionItem(text = title, onClick = {}, enabled = false)
            content()
        }
    }
}

/** Fixed reference row; disabled options retain their position and expose their reason. */
@Composable
fun PlayerSelectionItem(
    text: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    enabled: Boolean = true,
    summary: String? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 30.dp,
) {
    val color = if (enabled || selected) Color.White else Color.White.copy(alpha = 0.4f)
    Row(modifier.fillMaxWidth().height(35.dp)
        .background(if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
        .semantics(mergeDescendants = true) { this.selected = selected }
        .clickable(enabled = enabled, onClick = onClick)
        .padding(horizontal = horizontalPadding), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text, color = color, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f))
        if (summary != null) Text(summary, color = color.copy(alpha = 0.6f), fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun Modifier.playerPopupSelectedOption(selected: Boolean): Modifier {
    if (!selected) return this
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(requester) { withFrameNanos { }; requester.bringIntoView() }
    return bringIntoViewRequester(requester)
}
