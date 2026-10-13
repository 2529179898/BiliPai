@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.purebilibili.feature.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewSidebar
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.AdaptivePlainTooltipBox
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.util.HapticType
import com.android.purebilibili.core.util.rememberHapticFeedback

@Composable
internal fun MaterialExpressiveSideBar(
    currentItem: BottomNavItem,
    onItemClick: (BottomNavItem) -> Unit,
    visibleItems: List<BottomNavItem>,
    itemLabels: Map<String, String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onHomeDoubleTap: () -> Unit,
    onDynamicDoubleTap: () -> Unit,
    onToggleSidebar: (() -> Unit)?,
    onAccountSwitchClick: (() -> Unit)?,
    firstItemModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val state = rememberWideNavigationRailState(
        initialValue = if (expanded) WideNavigationRailValue.Expanded else WideNavigationRailValue.Collapsed,
    )
    LaunchedEffect(expanded, state) {
        if (expanded) state.expand() else state.collapse()
    }
    val haptic = rememberHapticFeedback()
    var lastHomeClickMs by remember { mutableLongStateOf(0L) }
    var lastDynamicClickMs by remember { mutableLongStateOf(0L) }
    WideNavigationRail(
        modifier = modifier,
        state = state,
        header = {
            Row(horizontalArrangement = Arrangement.Center) {
                AdaptivePlainTooltipBox(text = if (expanded) "收起导航" else "展开导航") {
                    AppIconButton(onClick = {
                        if (onToggleSidebar != null) onToggleSidebar() else onExpandedChange(!expanded)
                    }) {
                        AppIcon(Icons.Outlined.ViewSidebar, contentDescription = if (expanded) "收起导航" else "展开导航")
                    }
                }
                if (onAccountSwitchClick != null) {
                    AdaptivePlainTooltipBox(text = "切换账号") {
                        AppIconButton(onClick = onAccountSwitchClick) {
                            AppIcon(Icons.Outlined.SwapHoriz, contentDescription = "切换账号")
                        }
                    }
                }
            }
        },
    ) {
        visibleItems.forEachIndexed { index, item ->
            val selected = currentItem == item
            WideNavigationRailItem(
                selected = selected,
                railExpanded = state.targetValue == WideNavigationRailValue.Expanded,
                modifier = if (index == 0) firstItemModifier else Modifier,
                icon = { AppIcon(resolveMaterialBottomBarIcon(item, selected), contentDescription = null) },
                label = { AppText(resolveBottomNavItemLabel(item, itemLabels)) },
                onClick = {
                    val now = android.os.SystemClock.elapsedRealtime()
                    when {
                        resolveDynamicSideBarClickAction(item, now, lastDynamicClickMs) -> {
                            lastDynamicClickMs = 0L
                            haptic(HapticType.MEDIUM)
                            onDynamicDoubleTap()
                        }
                        resolveHomeSideBarClickAction(item, now, lastHomeClickMs) == HomeSideBarClickAction.HOME_DOUBLE_TAP -> {
                            lastHomeClickMs = 0L
                            haptic(HapticType.MEDIUM)
                            onHomeDoubleTap()
                        }
                        else -> {
                            if (item == BottomNavItem.HOME) lastHomeClickMs = now
                            if (item == BottomNavItem.DYNAMIC) lastDynamicClickMs = now
                            performHomeSideBarItemTap(haptic = haptic, onClick = { onItemClick(item) })
                        }
                    }
                },
            )
        }
    }
}
