package com.android.purebilibili.feature.video.ui.components

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SettingsManager
import com.android.purebilibili.data.model.response.ReplyMember

/** Compose adapter for the source-derived Nagram placement kernel. */
internal fun resolveCommentAvatarPushOffset(
    itemOffsetPx: Int,
    itemHeightPx: Int,
    topInsetPx: Int,
    bottomInsetPx: Int,
    viewportTopPx: Int,
    contentHeightPx: Int? = null,
    minimumSlotPx: Int = 48,
    imageBottomOffsetPx: Int = 44,
): Int {
    val boundedHeight = (contentHeightPx?.coerceAtMost(itemHeightPx) ?: itemHeightPx)
        .coerceAtLeast(0)
    if (boundedHeight <= topInsetPx + bottomInsetPx) return 0
    val groupTop = itemOffsetPx + topInsetPx
    val cellBottom = itemOffsetPx + boundedHeight - bottomInsetPx
    // Chat avatars belong at a message's bottom; comment avatars belong beside
    // their author. Reflect the original boundary solver, then remove its draw
    // inset so a visible author header always yields exactly zero displacement.
    val reflectedTop = resolveNagramChatAvatarTop(
        groupTopPx = -cellBottom,
        cellBottomPx = -groupTop,
        viewportBottomPx = -viewportTopPx,
        minimumSlotPx = minimumSlotPx,
        imageBottomOffsetPx = imageBottomOffsetPx,
    )
    return -reflectedTop - imageBottomOffsetPx - groupTop
}

@Composable
internal fun PushingReplyMemberAvatar(
    member: ReplyMember,
    placeholderColor: Color,
    lightweightMode: Boolean,
    listState: LazyListState,
    itemKey: Any,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    ReplyMemberAvatar(
        member = member,
        placeholderColor = placeholderColor,
        lightweightMode = lightweightMode,
        modifier = modifier.commentAvatarPushOffset(listState, itemKey),
        onClick = onClick,
    )
}

/** Geometry is read during placement, including measured content bounds for mixed lazy items. */
internal fun Modifier.commentAvatarPushOffset(
    listState: LazyListState,
    itemKey: Any,
    topInset: Dp = 10.dp,
    bottomInset: Dp = 1.dp,
    contentHeightPxProvider: (() -> Int)? = null,
): Modifier = offset {
    val layout = listState.layoutInfo
    val item = layout.visibleItemsInfo.firstOrNull { it.key == itemKey }
    val offset = if (item == null || layout.reverseLayout) {
        0
    } else {
        resolveCommentAvatarPushOffset(
            itemOffsetPx = item.offset,
            // A root comment may share its lazy item with a section title. Its avatar
            // must leave with the comment rather than drift into that title.
            itemHeightPx = item.size,
            contentHeightPx = contentHeightPxProvider?.invoke(),
            topInsetPx = topInset.roundToPx(),
            bottomInsetPx = bottomInset.roundToPx(),
            // Respect the existing top content inset below floating chrome.
            viewportTopPx = layout.viewportStartOffset + layout.beforeContentPadding,
            minimumSlotPx = 48.dp.roundToPx(),
            imageBottomOffsetPx = 44.dp.roundToPx(),
        )
    }
    IntOffset(0, offset)
}

@Composable
internal fun rememberCommentAvatarPushEnabled(): Boolean {
    val context = LocalContext.current
    val enabled by remember(context) { SettingsManager.getCommentAvatarPushEnabled(context) }
        .collectAsStateWithLifecycle(initialValue = false)
    return enabled
}
