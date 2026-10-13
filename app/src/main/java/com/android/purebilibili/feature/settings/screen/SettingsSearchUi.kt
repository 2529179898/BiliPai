package com.android.purebilibili.feature.settings

import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.components.appElasticPress


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
import com.android.purebilibili.core.ui.components.AppSearchEntry
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.rememberAdaptiveListVisualCapabilities
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContainerColor
import com.android.purebilibili.core.ui.components.rememberAdaptivePreferenceIconContentColor

@Composable
internal fun SettingsSearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit = {},
) {
    val placeholder = stringResource(R.string.settings_search_placeholder)
    AppLiquidAwareSearchField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = onSearch,
        placeholder = placeholder,
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
    AppSearchEntry(
        onClick = onClick,
        placeholder = placeholder,
        modifier = modifier
            .padding(horizontal = visualSpec.screenHorizontalPadding),
    )
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
                            title = result.title,
                            subtitle = listOf(result.path, resolveSettingsSearchDescriptionSnippet(result.subtitle, query)).filter { it.isNotBlank() }.joinToString("\n"),
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
