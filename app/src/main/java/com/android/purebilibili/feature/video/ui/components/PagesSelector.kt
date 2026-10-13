package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.core.ui.AppContentColors

import com.android.purebilibili.core.ui.components.appSearchElasticPress
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
            val currentPage = pages.getOrNull(currentPageIndex) ?: pages.first()
            val duration = if (currentPage.duration > 0L) {
                com.android.purebilibili.core.util.FormatUtils.formatDuration(
                    currentPage.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                )
            } else ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showExpandedSheet = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = "P${currentPage.page} · ${currentPage.part.ifBlank { "第${currentPage.page}P" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppContentColors.accent(),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (duration.isNotBlank()) {
                    Spacer(Modifier.width(12.dp))
                    AppText(duration, style = MaterialTheme.typography.labelMedium, color = AppContentColors.secondaryText())
                }
            }
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
