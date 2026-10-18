/*
 * Centered profile geometry ported from NextAlone/Nagram ProfileActivity.java
 * (fb56ea9801e5de68c60f0152b444bbe2a0fda520), GPL-2.0-or-later / GPL-3.0.
 * Telegram source copyright Nikolai Kudashov, 2013-2018, and Nagram contributors.
 */
package com.android.purebilibili.feature.profile

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.core.ui.components.UserLevelBadge
import com.android.purebilibili.core.ui.components.UserVipBadge
import com.android.purebilibili.feature.home.UserState
import kotlin.math.roundToInt

@Composable
internal fun ProfileIdentityHeader(
    user: UserState,
    heroChrome: ProfileHeroChrome,
    collapsedTextColor: Color,
    statusBarTopPadding: Dp,
    expandedFractionProvider: () -> Float,
    statsTopProvider: () -> Float,
    collapseRangeProvider: () -> Float,
    onIdentityMeasured: (nameHeightPx: Int, uidHeightPx: Int) -> Unit,
    listState: LazyListState,
    flingBehavior: FlingBehavior,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val measuredIdentityHeights = remember(LocalDensity.current, statusBarTopPadding) {
        intArrayOf(-1, -1)
    }
    Box(modifier = modifier) {
        ProfileAvatarEffect(
            user = user,
            expandedFractionProvider = expandedFractionProvider,
            statusBarTopPadding = statusBarTopPadding,
            borderColor = heroChrome.avatarBorderColor,
            listState = listState,
            flingBehavior = flingBehavior,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.matchParentSize(),
        )
        Layout(
            modifier = Modifier.matchParentSize(),
            content = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        AppText(
                            text = user.name,
                            color = heroChrome.textColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer { alpha = expandedFractionProvider() },
                        )
                        AppText(
                            text = user.name,
                            color = collapsedTextColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .graphicsLayer { alpha = 1f - expandedFractionProvider() }
                                .clearAndSetSemantics {},
                        )
                    }
                    UserLevelBadge(level = user.level)
                    if (user.isVip) UserVipBadge(label = user.vipLabel, compact = true)
                }
                AppText(
                    text = "UID ${user.mid}",
                    color = heroChrome.secondaryTextColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            },
        ) { measurables, constraints ->
            val expanded = expandedFractionProvider().coerceIn(0f, 1f)
            val width = constraints.maxWidth
            val top = statusBarTopPadding.toPx()
            val nameScale = 1f + (PROFILE_EXPANDED_NAME_SCALE - 1f) * expanded
            val name = measurables[0].measure(Constraints(maxWidth = ((width - 112.dp.toPx()).coerceAtLeast(0f) / nameScale).roundToInt()))
            val status = measurables[1].measure(Constraints(maxWidth = width))
            if (measuredIdentityHeights[0] != name.height || measuredIdentityHeights[1] != status.height) {
                measuredIdentityHeights[0] = name.height
                measuredIdentityHeights[1] = status.height
                onIdentityMeasured(name.height, status.height)
            }
            // Large text grows the hero reserve downward, never moves the name into the avatar.
            val expandedNameY = top + PROFILE_EXPANDED_NAME_TOP_DP.dp.toPx()
            val collapsedNameY = top + (56.dp.toPx() - name.height) / 2f
            // Both the foreground identity and the scrolling statistics use one measured range.
            val nameY = collapsedNameY + (expandedNameY - collapsedNameY) * expanded
            val statusY = nameY + name.height * nameScale + 6.dp.toPx()
            val statsY = statsTopProvider() - collapseRangeProvider() * (1f - expanded)
            val statusAlpha = expanded * ((statsY - statusY - status.height - 8.dp.toPx()) / 16.dp.toPx()).coerceIn(0f, 1f)
            val centeredNameX = (width - name.width * nameScale) / 2f
            val nameX = 56.dp.toPx() + (centeredNameX - 56.dp.toPx()) * expanded
            layout(width, constraints.maxHeight) {
                name.placeWithLayer(nameX.roundToInt(), nameY.roundToInt()) {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    scaleX = nameScale
                    scaleY = nameScale
                }
                status.placeWithLayer((width - status.width) / 2, statusY.roundToInt()) {
                    alpha = statusAlpha
                }
            }
        }
    }
}
