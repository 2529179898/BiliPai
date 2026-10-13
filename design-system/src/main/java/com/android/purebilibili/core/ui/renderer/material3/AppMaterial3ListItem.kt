@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.purebilibili.core.ui.renderer.material3

import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun AppMaterial3ListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier,
    overlineContent: (@Composable () -> Unit)?,
    supportingContent: (@Composable () -> Unit)?,
    leadingContent: (@Composable () -> Unit)?,
    trailingContent: (@Composable () -> Unit)?,
) = SegmentedListItem(
    colors = ListItemDefaults.colors(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        headlineColor = com.android.purebilibili.core.ui.AppContentColors.primaryText(),
        supportingColor = com.android.purebilibili.core.ui.AppContentColors.secondaryText(),
        overlineColor = com.android.purebilibili.core.ui.AppContentColors.secondaryText(),
        trailingIconColor = com.android.purebilibili.core.ui.AppContentColors.secondaryText(),
    ),
    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
    content = headlineContent,
    modifier = modifier,
    overlineContent = overlineContent,
    supportingContent = supportingContent,
    leadingContent = leadingContent,
    trailingContent = trailingContent,
)
