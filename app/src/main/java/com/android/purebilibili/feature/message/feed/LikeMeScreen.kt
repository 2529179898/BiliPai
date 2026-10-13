package com.android.purebilibili.feature.message.feed

import com.android.purebilibili.core.ui.AppContentColors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import com.android.purebilibili.core.ui.components.AppLazyColumn as LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.components.AppIcon
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.purebilibili.feature.message.MessageAppScaffold as AppScaffold
import com.android.purebilibili.core.ui.AppTopBar
import com.android.purebilibili.core.ui.AdaptivePullToRefreshBox
import com.android.purebilibili.core.ui.rememberAppBackIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.data.model.response.MessageFeedLikeItem
import com.android.purebilibili.data.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LikeMeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val latestItems: List<MessageFeedLikeItem> = emptyList(),
    val totalItems: List<MessageFeedLikeItem> = emptyList(),
    val cursor: Long? = null,
    val cursorTime: Long? = null,
    val hasMore: Boolean = false,
    val error: String? = null
)

private fun mergeLikeItems(
    existing: List<MessageFeedLikeItem>,
    incoming: List<MessageFeedLikeItem>
): List<MessageFeedLikeItem> {
    val seen = linkedSetOf<Long>()
    return buildList {
        (existing + incoming).forEach { item ->
            if (seen.add(item.id)) add(item)
        }
    }
}

class LikeMeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LikeMeUiState())
    val uiState: StateFlow<LikeMeUiState> = _uiState.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            MessageRepository.getLikeFeed().fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        latestItems = data.latest?.items.orEmpty(),
                        totalItems = data.total?.items.orEmpty(),
                        cursor = data.total?.cursor?.id,
                        cursorTime = data.total?.cursor?.time,
                        hasMore = data.total?.cursor?.isEnd != true && !data.total?.items.isNullOrEmpty()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message ?: "加载失败")
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            MessageRepository.getLikeFeed().fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        latestItems = data.latest?.items.orEmpty(),
                        totalItems = data.total?.items.orEmpty(),
                        cursor = data.total?.cursor?.id,
                        cursorTime = data.total?.cursor?.time,
                        hasMore = data.total?.cursor?.isEnd != true && !data.total?.items.isNullOrEmpty()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(isRefreshing = false, error = error.message ?: "刷新失败")
                }
            )
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current.isLoadingMore || !current.hasMore) return
        viewModelScope.launch {
            _uiState.value = current.copy(isLoadingMore = true)
            MessageRepository.getLikeFeed(cursor = current.cursor, cursorTime = current.cursorTime).fold(
                onSuccess = { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        latestItems = mergeLikeItems(current.latestItems, data.latest?.items.orEmpty()),
                        totalItems = mergeLikeItems(current.totalItems, data.total?.items.orEmpty()),
                        cursor = data.total?.cursor?.id,
                        cursorTime = data.total?.cursor?.time,
                        hasMore = data.total?.cursor?.isEnd != true && !data.total?.items.isNullOrEmpty()
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            )
        }
    }

    fun remove(id: Long, latest: Boolean) {
        viewModelScope.launch {
            MessageRepository.deleteFeedItem(type = 0, id = id).onSuccess {
                _uiState.value = _uiState.value.copy(
                    latestItems = if (latest) _uiState.value.latestItems.filterNot { it.id == id } else _uiState.value.latestItems,
                    totalItems = if (latest) _uiState.value.totalItems else _uiState.value.totalItems.filterNot { it.id == id }
                )
            }
        }
    }

    fun toggleNotice(item: MessageFeedLikeItem) {
        viewModelScope.launch {
            val enableNotifications = item.noticeState == 1
            MessageRepository.setLikeFeedNotice(id = item.id, enabled = enableNotifications).onSuccess {
                fun update(list: List<MessageFeedLikeItem>): List<MessageFeedLikeItem> {
                    return list.map {
                        if (it.id == item.id) {
                            it.copy(noticeState = if (enableNotifications) 0 else 1)
                        } else {
                            it
                        }
                    }
                }

                _uiState.value = _uiState.value.copy(
                    latestItems = update(_uiState.value.latestItems),
                    totalItems = update(_uiState.value.totalItems)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikeMeScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
    onOpenSpace: (Long) -> Unit,
    viewModel: LikeMeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AppScaffold(
        blurContentReady = !uiState.isLoading,
        topBar = {
            AppTopBar(
                title = "收到的赞",
                navigationIcon = {
                    AppIconButton(onClick = onBack) {
                        AppIcon(rememberAppBackIcon(), contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 骨架与真实卡片共用同一个 LazyColumn 与同一个 contentType：数据到达时
            // LazyList 在原地替换 item（配合 animateItem 交叉淡入），不再整棵子树重建。
            // Scaffold already pads out the top bar — indicator at box top (inset 0).
            AdaptivePullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                indicatorTopInset = paddingValues.calculateTopPadding(),
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = paddingValues.calculateTopPadding() + 10.dp, bottom = paddingValues.calculateBottomPadding() + 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when {
                        uiState.isLoading -> {
                            items(
                                count = MESSAGE_FEED_SKELETON_ITEM_COUNT,
                                key = { index -> "like_me_skeleton_$index" },
                                contentType = { MESSAGE_FEED_CARD_CONTENT_TYPE }
                            ) { index ->
                                MessageFeedCardSkeleton(
                                    slotIndex = index,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .animateItem(),
                                )
                            }
                        }

                        uiState.error != null -> {
                            item(key = "like_me_error") {
                                MessageFeedError(
                                    text = uiState.error ?: "加载失败",
                                    onRetry = viewModel::loadInitial,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillParentMaxHeight(0.6f)
                                )
                            }
                        }

                        uiState.latestItems.isEmpty() && uiState.totalItems.isEmpty() -> {
                            item(key = "like_me_empty") {
                                MessageFeedEmpty(
                                    text = "暂无点赞消息",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillParentMaxHeight(0.6f)
                                )
                            }
                        }

                        else -> {
                            if (uiState.latestItems.isNotEmpty()) {
                                item(key = "like_me_latest_header") { MessageFeedSectionHeader("最新") }
                                items(
                                    items = uiState.latestItems,
                                    key = { "latest_${it.id}" },
                                    contentType = { MESSAGE_FEED_CARD_CONTENT_TYPE }
                                ) { item ->
                                    LikeMeCard(
                                        item = item,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem(),
                                        onClick = {
                                            item.item?.let { content ->
                                                buildMessageFeedCommentNavigationLink(
                                                    nativeUri = content.nativeUri,
                                                    uri = content.uri,
                                                    businessId = content.businessId,
                                                    subjectId = content.subjectId,
                                                    rootId = content.rootId,
                                                    sourceId = content.sourceId,
                                                    targetId = content.targetId,
                                                    business = content.business
                                                )?.let(onOpenLink)
                                            }
                                        },
                                        onUserClick = {
                                            item.users?.firstOrNull()?.mid?.takeIf { it > 0 }?.let(onOpenSpace)
                                        },
                                        onToggleNotice = { viewModel.toggleNotice(item) },
                                        onRemove = { viewModel.remove(item.id, latest = true) }
                                    )
                                }
                            }

                            if (uiState.totalItems.isNotEmpty()) {
                                item(key = "like_me_total_header") { MessageFeedSectionHeader("累计") }
                                items(
                                    items = uiState.totalItems,
                                    key = { "total_${it.id}" },
                                    contentType = { MESSAGE_FEED_CARD_CONTENT_TYPE }
                                ) { item ->
                                    LikeMeCard(
                                        item = item,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .animateItem(),
                                        onClick = {
                                            item.item?.let { content ->
                                                buildMessageFeedCommentNavigationLink(
                                                    nativeUri = content.nativeUri,
                                                    uri = content.uri,
                                                    businessId = content.businessId,
                                                    subjectId = content.subjectId,
                                                    rootId = content.rootId,
                                                    sourceId = content.sourceId,
                                                    targetId = content.targetId,
                                                    business = content.business
                                                )?.let(onOpenLink)
                                            }
                                        },
                                        onUserClick = {
                                            item.users?.firstOrNull()?.mid?.takeIf { it > 0 }?.let(onOpenSpace)
                                        },
                                        onToggleNotice = { viewModel.toggleNotice(item) },
                                        onRemove = { viewModel.remove(item.id, latest = false) }
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.hasMore || uiState.isLoadingMore) {
                        item(key = "like_me_load_more") {
                            MessageFeedLoadMore(
                                isLoadingMore = uiState.isLoadingMore,
                                hasMore = uiState.hasMore,
                                onLoadMore = viewModel::loadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LikeMeCard(
    item: MessageFeedLikeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    onToggleNotice: () -> Unit,
    onRemove: () -> Unit
) {
    val firstUser = item.users?.firstOrNull()
    MessageFeedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            MessageFeedAvatar(
                avatarUrl = firstUser?.avatar.orEmpty(),
                modifier = Modifier.clickable(onClick = onUserClick)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    text = buildString {
                        append(firstUser?.nickname.orEmpty().ifBlank { "用户" })
                        if (item.counts > 1) append(" 等${item.counts}人")
                        append(" 赞了我的")
                        append(item.item?.business.orEmpty().ifBlank { "内容" })
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                item.item?.title?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    AppText(text = it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(
                        text = formatMessageFeedTime(item.likeTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.secondaryText()
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AppText(
                        text = if (item.noticeState == 1) "接收通知" else "不再通知",
                        modifier = Modifier.clickable(onClick = onToggleNotice),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.accent()
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    AppText(
                        text = "删除",
                        modifier = Modifier.clickable(onClick = onRemove),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppContentColors.accent()
                    )
                }
            }
        }
    }
}
