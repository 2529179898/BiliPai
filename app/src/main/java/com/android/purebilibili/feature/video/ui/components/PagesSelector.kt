package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.AppContentColors

import com.android.purebilibili.core.ui.components.appSearchElasticPress
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.AppSurfaceTokens
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import androidx.compose.material3.MaterialTheme
import com.android.purebilibili.core.ui.components.AppOutlinedTextField
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppModalBottomSheet
import com.android.purebilibili.data.model.response.Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesSelector(
    pages: List<Page>,
    currentPageIndex: Int,
    onPageSelect: (Int) -> Unit,
    forceGridMode: Boolean = false,
    blockParentVerticalScroll: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    coverUrl: String = "",
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val configuration = LocalConfiguration.current
    val isLandscape = remember(configuration.orientation, configuration.screenWidthDp, configuration.screenHeightDp) {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            configuration.screenWidthDp > configuration.screenHeightDp
    }
    val layoutPolicy = remember(
        configuration.screenWidthDp,
        isLandscape,
        pages.size,
        forceGridMode
    ) {
        resolvePagesSelectorLayoutPolicy(
            widthDp = configuration.screenWidthDp,
            isLandscape = isLandscape,
            pagesCount = pages.size,
            forceGridMode = forceGridMode
        )
    }
    val showExpandAction = !forceGridMode
    val navigationBarBottomPadding = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val gridBottomPaddingDp = resolvePagesSelectorBottomContentPaddingDp(
        navigationBarBottomDp = navigationBarBottomPadding.value.toInt()
    )
    val groups = remember(pages) { resolvePageSelectorGroups(pages) }

    var inlineQuery by rememberSaveable(pages.size, forceGridMode) { mutableStateOf("") }
    var inlineGroupKey by rememberSaveable(pages.size, forceGridMode) { mutableStateOf<String?>(null) }
    val inlineVisibleIndices = remember(pages, inlineGroupKey, inlineQuery) {
        filterPageIndicesForSelector(
            pages = pages,
            selectedGroupKey = inlineGroupKey,
            query = inlineQuery
        )
    }

    var showExpandedSheet by rememberSaveable(pages.size, forceGridMode) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (forceGridMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "选集",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppText(
                    text = "(${pages.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppContentColors.secondaryText()
                )
                Spacer(modifier = Modifier.weight(1f))
                if (showExpandAction) {
                    Row(
                        modifier = Modifier
                            .clip(AppShapes.container(ContainerLevel.Pill))
                            .clickable { showExpandedSheet = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppText(
                            text = "展开",
                            style = MaterialTheme.typography.labelLarge,
                            color = AppContentColors.accent()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        AppIcon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = "展开选集",
                            tint = AppContentColors.accent(),
                            modifier = Modifier.width(16.dp)
                        )
                    }
                }
                if (onDismissRequest != null) {
                    AppIconButton(onClick = onDismissRequest) {
                        AppIcon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭选集面板",
                            tint = AppContentColors.secondaryText()
                        )
                    }
                }
            }
        } else {
            //  标题行：视频选集 + 正在播放：<当前分P> + 共N集
            val currentPage = pages.getOrNull(currentPageIndex) ?: pages.first()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "视频选集",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppContentColors.primaryText()
                )
                Spacer(modifier = Modifier.width(8.dp))
                AppText(
                    text = "正在播放：${currentPage.part.ifBlank { "第${currentPage.page}P" }}",
                    fontSize = 12.sp,
                    color = AppContentColors.secondaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Row(
                    modifier = Modifier
                        .clip(AppShapes.container(ContainerLevel.Pill))
                        .clickable { showExpandedSheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "共${pages.size}集",
                        fontSize = 13.sp,
                        color = AppContentColors.accent()
                    )
                }
                if (onDismissRequest != null) {
                    AppIconButton(onClick = onDismissRequest) {
                        AppIcon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭选集面板",
                            tint = AppContentColors.secondaryText()
                        )
                    }
                }
            }
        }

        if (forceGridMode) {
            Spacer(modifier = Modifier.height(12.dp))

            if (pages.size >= 10) {
                PagesSelectorFilterBar(
                    groups = groups,
                    selectedGroupKey = inlineGroupKey,
                    query = inlineQuery,
                    totalCount = pages.size,
                    onGroupSelect = { inlineGroupKey = it },
                    onQueryChange = { inlineQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            PageCoverList(
                pages = pages,
                visiblePageIndices = inlineVisibleIndices,
                currentPageIndex = currentPageIndex,
                coverUrl = coverUrl,
                onPageSelect = onPageSelect,
                blockParentVerticalScroll = blockParentVerticalScroll,
                bottomContentPaddingDp = if (forceGridMode) gridBottomPaddingDp else 12,
                onResetFilters = { inlineQuery = ""; inlineGroupKey = null },
                modifier = Modifier.fillMaxWidth().heightIn(max = layoutPolicy.maxGridHeightDp.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            PagesHorizontalPreviewRow(
                pages = pages,
                currentPageIndex = currentPageIndex,
                isPlaying = isPlaying,
                onPageSelect = onPageSelect,
                modifier = Modifier.fillMaxWidth(),
            )
        }

    }

    if (showExpandedSheet) {
        var expandedQuery by rememberSaveable(pages.size) { mutableStateOf("") }
        var expandedGroupKey by rememberSaveable(pages.size) { mutableStateOf<String?>(null) }
        val expandedVisibleIndices = remember(pages, expandedGroupKey, expandedQuery) {
            filterPageIndicesForSelector(
                pages = pages,
                selectedGroupKey = expandedGroupKey,
                query = expandedQuery
            )
        }

        AppModalBottomSheet(
            onDismissRequest = { showExpandedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            windowInsets = WindowInsets(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "分集(${pages.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppContentColors.primaryText()
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    AppIconButton(onClick = { showExpandedSheet = false }) {
                        AppIcon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "关闭选集",
                            tint = AppContentColors.secondaryText()
                        )
                    }
                }

                PagesSelectorFilterBar(
                    groups = groups,
                    selectedGroupKey = expandedGroupKey,
                    query = expandedQuery,
                    totalCount = pages.size,
                    onGroupSelect = { expandedGroupKey = it },
                    onQueryChange = { expandedQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                PageCoverList(
                    pages = pages,
                    visiblePageIndices = expandedVisibleIndices,
                    currentPageIndex = currentPageIndex,
                    coverUrl = coverUrl,
                    onPageSelect = { index ->
                        onPageSelect(index)
                        showExpandedSheet = false
                    },
                    blockParentVerticalScroll = true,
                    bottomContentPaddingDp = gridBottomPaddingDp,
                    onResetFilters = { expandedQuery = ""; expandedGroupKey = null },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

/** 正在播放指示：三根竖条随播放节奏起伏，未播放时保持静止波形。 */
@Composable
private fun PlayingEqualizerIcon(
    playing: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val phase = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (!playing) {
            phase.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            phase.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing),
            )
            phase.snapTo(0f)
        }
    }
    Canvas(modifier = modifier) {
        val barCount = 3
        val gap = size.width * 0.22f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val currentPhase = phase.value
        repeat(barCount) { index ->
            val ratio = if (playing) {
                0.28f + 0.72f * sin((currentPhase + index * 0.24f) * PI.toFloat())
            } else {
                IdleEqualizerRatios[index]
            }
            val barHeight = (size.height * ratio).coerceAtLeast(1f)
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

/**
 *  横向分集卡片行：固定 150dp 卡片宽、10dp 间距、35dp 行高，
 * 当前分 P 在卡片内额外显示播放波形图标并使用强调色文字，其余分 P 使用主文字色；
 * 当前分 P 变化时横向滚动到对应卡片。
 */
@Composable
private fun PagesHorizontalPreviewRow(
    pages: List<Page>,
    currentPageIndex: Int,
    isPlaying: Boolean,
    onPageSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val currentIndex = currentPageIndex.coerceIn(0, pages.lastIndex)
    var hasPositioned by remember(pages) { mutableStateOf(false) }

    LaunchedEffect(currentIndex, pages.size) {
        // 定位当前项：其左边缘停在视口左侧半张卡片宽度处，首帧不做动画，之后平滑滚动。
        val halfItemPx = with(density) { PagesPreviewItemWidth.toPx() / 2f }
        if (hasPositioned) {
            listState.animateScrollToItem(currentIndex)
            listState.animateScrollBy(-halfItemPx)
        } else {
            listState.scrollToItem(currentIndex)
            listState.scrollBy(-halfItemPx)
            hasPositioned = true
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.height(PagesPreviewRowHeight),
        horizontalArrangement = Arrangement.spacedBy(PagesPreviewItemSpacing),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(pages.size) { index ->
            val page = pages[index]
            val isCurrent = index == currentIndex
            Row(
                modifier = Modifier
                    .width(PagesPreviewItemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(PagesPreviewItemCorner))
                    .background(AppSurfaceTokens.cardContainer())
                    .clickable { onPageSelect(index) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isCurrent) {
                    PlayingEqualizerIcon(
                        playing = isPlaying,
                        color = AppContentColors.accent(),
                        modifier = Modifier.size(PagePreviewEqualizerSize),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                AppText(
                    text = page.part.ifBlank { "第${page.page}P" },
                    fontSize = 13.sp,
                    color = if (isCurrent) AppContentColors.accent() else AppContentColors.primaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val PagesPreviewItemWidth = 150.dp
private val PagesPreviewItemSpacing = 10.dp
private val PagesPreviewRowHeight = 35.dp
private val PagesPreviewItemCorner = 6.dp
private val PagePreviewEqualizerSize = 12.dp

/** 未播放时保持的静止波形高度比例。 */
private val IdleEqualizerRatios = floatArrayOf(0.45f, 0.9f, 0.6f)

@Composable
private fun PagesSelectorFilterBar(
    groups: List<PageSelectorGroup>,
    selectedGroupKey: String?,
    query: String,
    totalCount: Int,
    onGroupSelect: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        AppOutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().appSearchElasticPress(),
            placeholder = {
                AppText(text = "搜索 P号 / 标题")
            },
            leadingIcon = {
                AppIcon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null
                )
            },
            trailingIcon = if (query.isNotBlank()) {
                {
                    AppIconButton(onClick = { onQueryChange("") }) {
                        AppIcon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "清空搜索"
                        )
                    }
                }
            } else {
                null
            }
        )

        if (groups.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AppFilterChip(
                        selected = selectedGroupKey == null,
                        onClick = { onGroupSelect(null) },
                        label = { AppText("全部 $totalCount") }
                    )
                }
                items(groups, key = { it.key }) { group ->
                    AppFilterChip(
                        selected = selectedGroupKey == group.key,
                        onClick = { onGroupSelect(group.key) },
                        label = { AppText("${group.label} ${group.count}") }
                    )
                }
            }
        }
    }
}
