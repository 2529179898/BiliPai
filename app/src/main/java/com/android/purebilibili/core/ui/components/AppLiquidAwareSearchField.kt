package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.isSpecified
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppChromeSizeTokens
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale
import top.yukonga.miuix.kmp.blur.Backdrop

/** Native search input with an optional liquid glass surface behind it. */
@Composable
fun AppLiquidAwareSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: () -> Unit = {},
    onClear: () -> Unit = { onQueryChange("") },
    autoFocusEnabled: Boolean = false,
    focusRequester: FocusRequester? = null,
    interactionSource: MutableInteractionSource? = null,
    backdrop: Backdrop? = null,
    liquidContentContainerColor: Color = Color.Transparent,
    isScrollInProgressProvider: () -> Boolean = { false },
    leadingIconHorizontalOffset: Dp = 0.dp,
    presentation: AppSearchFieldPresentation = AppSearchFieldPresentation.STANDARD,
    compactHeight: Dp = Dp.Unspecified,
) {
    BottomBarMatchedReusableLiquidDock(
        shape = CircleShape,
        modifier = modifier,
        backdrop = backdrop,
        reuseEnabled = true,
        elasticFeedbackEnabled = true,
        elasticFeedbackStrength = com.android.purebilibili.core.ui.components.APP_SEARCH_ELASTIC_FEEDBACK_STRENGTH,
        useNeutralLiquidContainer = true,
        drawShellLens = true,
        shellLensIntensity = resolveFloatingDockGeometryScale(
            AppChromeSizeTokens.BottomBarMatchedSegmentedControlHeightDp.toFloat()
        ),
        isScrollInProgressProvider = isScrollInProgressProvider,
    ) { liquidChromeActive ->
        if (compactHeight.isSpecified) {
            CompactLiquidSearchInput(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = placeholder,
                height = compactHeight,
                onSearch = onSearch,
                onClear = onClear,
                focusRequester = focusRequester,
                autoFocusEnabled = autoFocusEnabled,
                interactionSource = interactionSource,
                leadingIconHorizontalOffset = leadingIconHorizontalOffset,
            )
        } else {
            AppSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = placeholder,
                onSearch = onSearch,
                onClear = onClear,
                presentation = presentation,
                autoFocusEnabled = autoFocusEnabled,
                focusRequester = focusRequester,
                interactionSource = interactionSource,
                leadingIconHorizontalOffset = leadingIconHorizontalOffset,
                containerColor = if (liquidChromeActive) {
                    liquidContentContainerColor
                } else {
                    Color.Unspecified
                },
            )
        }
    }
}

@Composable
private fun CompactLiquidSearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    height: Dp,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester?,
    autoFocusEnabled: Boolean,
    interactionSource: MutableInteractionSource?,
    leadingIconHorizontalOffset: Dp,
) {
    val requester = focusRequester ?: androidx.compose.runtime.remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(requester, autoFocusEnabled) {
        if (autoFocusEnabled) requester.requestFocus()
    }
    val contentColor = com.android.purebilibili.core.ui.AppContentColors.primaryText()
    val hintColor = com.android.purebilibili.core.ui.AppContentColors.secondaryText()
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = height).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(
            Icons.Default.Search,
            contentDescription = null,
            tint = hintColor,
            modifier = Modifier.padding(start = leadingIconHorizontalOffset).size(24.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).focusRequester(requester).padding(vertical = 6.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = contentColor),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(contentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        AppText(
                            placeholder,
                            color = hintColor,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            AppIconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                AppIcon(Icons.Default.Clear, contentDescription = "清除搜索内容", tint = hintColor)
            }
        }
    }
}
