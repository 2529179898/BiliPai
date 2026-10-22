package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.core.ui.components.appElasticPress


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.purebilibili.R
import com.android.purebilibili.core.ui.rememberAppSettingsIcon
import com.android.purebilibili.core.ui.adaptiveSquircleBackground
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppPreference
import com.android.purebilibili.core.ui.components.AppPreferenceDivider
import com.android.purebilibili.core.ui.components.AppPreferenceGroup
import com.android.purebilibili.core.ui.components.AppLiquidAwareSearchField
import com.android.purebilibili.core.ui.components.AppSearchFieldPresentation
import com.android.purebilibili.core.ui.components.AppSearchEntry
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.rememberAdaptiveListVisualCapabilities
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor
import com.android.purebilibili.core.ui.LocalAppThemeConfig
import com.android.purebilibili.core.ui.LocalGlobalWallpaperBackdropVisible
import com.android.purebilibili.core.ui.wallpaper.LocalGlobalWallpaperBackdropSource
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import top.yukonga.miuix.kmp.blur.Backdrop

@Composable
private fun settingsSearchWallpaperBackdrop(): Backdrop? {
    // Search lives inside the page recording. Only the independent wallpaper source is
    // safe here; passing the scroll/header source back into its child would sample itself.
    val source = LocalGlobalWallpaperBackdropSource.current
    return if (LocalAppThemeConfig.current.liquidGlassEnabled &&
        LocalGlobalWallpaperBackdropVisible.current
    ) source?.takeIf { it.isReady }?.backdrop else null
}

@Composable
internal fun SettingsSearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit = {},
    autoFocusEnabled: Boolean = true,
) {
    val placeholder = stringResource(R.string.settings_search_placeholder)
    AppLiquidAwareSearchField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        placeholder = placeholder,
        presentation = AppSearchFieldPresentation.TOP_BAR,
        backdrop = settingsSearchWallpaperBackdrop(),
        autoFocusEnabled = autoFocusEnabled,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
internal fun SettingsHomeSearchEntry(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visualSpec = resolveSettingsVisualSpec()
    val placeholder = stringResource(R.string.settings_search_placeholder)
    val entryModifier = modifier.padding(horizontal = visualSpec.screenHorizontalPadding)
    // Material boundary: keep the disabled MD3/Miuix entry and its defaults intact.
    if (!LocalAppThemeConfig.current.liquidGlassEnabled) {
        AppSearchEntry(
            onClick = onClick,
            placeholder = placeholder,
            modifier = entryModifier,
        )
        return
    }
    BottomBarMatchedReusableLiquidDock(
        shape = CircleShape,
        modifier = entryModifier,
        backdrop = settingsSearchWallpaperBackdrop(),
        reuseEnabled = true,
        elasticFeedbackEnabled = true,
        elasticFeedbackStrength = com.android.purebilibili.core.ui.components.APP_SEARCH_ELASTIC_FEEDBACK_STRENGTH,
        useNeutralLiquidContainer = true,
    ) {
        // Glass entry is a compact click target, independent of native input-field metrics.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AppChromeSizeTokens.LiquidSearchFieldHeightDp.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(
                Icons.Default.Search,
                contentDescription = null,
                tint = AppContentColors.secondaryText(),
                modifier = Modifier.size(24.dp),
            )
            AppText(
                text = placeholder,
                color = AppContentColors.secondaryText(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun SettingsSearchResultsSection(
    results: List<SettingsSearchResult>,
    onResultClick: (SettingsSearchResult) -> Unit,
    settingsState: SettingsUiState? = null,
    onClearQuery: () -> Unit = {},
    query: String = "",
) {
    val visualSpec = rememberAdaptiveListVisualCapabilities().componentSpec
    // 必须包一层 Column：上层 Entrance 是 Box，多个子节点会叠在同一原点，
    // 导致「搜索结果」标题压到第一行图标上（可见半个「果」字）。
    Column(modifier = Modifier.fillMaxWidth()) {
        if (results.isEmpty()) {
            val iconContainerColor =
                rememberAdaptivePreferenceIconContainerColor(AppContentColors.accent())
            val iconContentColor = rememberAdaptivePreferenceIconContentColor(iconContainerColor)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .adaptiveSquircleBackground(iconContainerColor, 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AppIcon(
                        imageVector = rememberAppSettingsIcon(),
                        contentDescription = null,
                        tint = iconContentColor,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                AppText(
                    text = stringResource(R.string.settings_search_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppText(
                    text = stringResource(R.string.settings_search_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppContentColors.secondaryText(),
                    textAlign = TextAlign.Center,
                )
                androidx.compose.material3.TextButton(onClick = onClearQuery, modifier = Modifier.appElasticPress()) { AppText("清除搜索") }
            }
        } else {
            // 顶栏已是「搜索结果」，不再重复放 CategoryHeader，避免与列表叠字。
            Spacer(modifier = Modifier.height(8.dp))
            AppPreferenceGroup {
                results.forEachIndexed { index, result ->
                    key(result.settingId) {
                    val titleResId = settingsSearchResultTitleResource(result.settingId)
                    val localizedTitle = if (titleResId != null) stringResource(titleResId) else result.title
                    val summaryResId = settingsSearchResultSummaryResource(result.settingId)
                    val localizedSummary = if (summaryResId != null) stringResource(summaryResId) else result.subtitle
                    val item = settingsItemDirectory.firstOrNull { it.settingId == result.settingId }
                    val context = LocalContext.current
                    val valueFlow = remember(item, context) { item?.observeValue?.invoke(context) }
                    val currentValue = if (valueFlow != null) {
                        val value by valueFlow.collectAsStateWithLifecycle(initialValue = "")
                        value
                    } else if (settingsState != null) item?.readState?.invoke(settingsState)
                        ?: playbackSettingStateValue(result.settingId, settingsState) else result.currentValue
                    val visual = rememberSettingsEntryVisual(result.target)
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.android.purebilibili.core.ui.components.LocalAppTextSearchQuery provides query,
                    ) {
                        AppPreference(
                            icon = visual.icon,
                            iconPainter = visual.iconResId?.let { painterResource(id = it) },
                            title = localizedTitle,
                            subtitle = listOf(result.path, resolveSettingsSearchDescriptionSnippet(localizedSummary, query)).filter { it.isNotBlank() }.joinToString("\n"),
                            value = currentValue,
                            onClick = { onResultClick(result) },
                            iconTint = visual.iconTint,
                        )
                    }
                    if (index != results.lastIndex) {
                        AppPreferenceDivider(startIndent = visualSpec.dividerStartIndentDp.dp)
                    }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
