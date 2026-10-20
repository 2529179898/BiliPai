package com.android.purebilibili.feature.home.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens

/**
 * Shared side-by-side video-card geometry, based on the related-video row.
 *
 * Callers own shell effects (click, selection, shared transition and native snapshot) while this
 * frame keeps cover geometry, spacing, info padding and the trailing action lane identical.
 */
@Composable
internal fun HorizontalVideoCardFrame(
    coverContent: @Composable BoxScope.() -> Unit,
    infoContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
    coverOverlayModifier: Modifier = Modifier,
    infoModifier: Modifier = Modifier,
    coverOverlayContent: (@Composable BoxScope.() -> Unit)? = null,
    trailingContent: (@Composable BoxScope.() -> Unit)? = null,
    coverAspectRatio: Float = com.android.purebilibili.core.ui.videoCardCoverAspectRatio(),
    stacked: Boolean = false,
) {
    // This is shared behavior for MD3, Miuix and glass. Both horizontal and stacked
    // video cards honor the selected ratio; material-specific effects do not change geometry.
    val effectiveAspectRatio = coverAspectRatio.takeIf { it.isFinite() && it > 1f }
        ?: HORIZONTAL_VIDEO_CARD_COVER_ASPECT_RATIO
    val coverWidth = HORIZONTAL_VIDEO_CARD_COVER_WIDTH_DP.dp
    val resolvedMinimumHeight = resolveHorizontalVideoCardMinimumHeightDp(LocalDensity.current.fontScale, effectiveAspectRatio).dp
    val infoPadding = HORIZONTAL_VIDEO_CARD_INFO_PADDING_DP.dp
    val infoEndPadding = if (trailingContent == null) infoPadding else
        (HORIZONTAL_VIDEO_CARD_ACTION_SIZE_DP + HORIZONTAL_VIDEO_CARD_ACTION_INSET_DP +
            HORIZONTAL_VIDEO_CARD_INFO_SPACING_DP).dp
    val infoArrangement = Arrangement.spacedBy(HORIZONTAL_VIDEO_CARD_INFO_SPACING_DP.dp)

    Box(modifier = modifier) {
        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacingTokens.Small),
            ) {
                Box(
                    modifier = coverModifier
                        .fillMaxWidth()
                        .aspectRatio(effectiveAspectRatio)
                        .clip(AppShapes.mediaCover())
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    coverContent()
                    if (coverOverlayContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(coverOverlayModifier),
                            content = coverOverlayContent,
                        )
                    }
                }
                Column(
                    modifier = infoModifier
                        .fillMaxWidth()
                        .padding(
                            top = infoPadding,
                            bottom = infoPadding,
                            end = infoEndPadding,
                        ),
                    verticalArrangement = infoArrangement,
                    content = infoContent,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = resolvedMinimumHeight),
                horizontalArrangement = Arrangement.spacedBy(
                    HORIZONTAL_VIDEO_CARD_COVER_INFO_GAP_DP.dp
                ),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = coverModifier
                        .width(coverWidth)
                        .aspectRatio(effectiveAspectRatio)
                        .clip(AppShapes.mediaCover())
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    coverContent()
                    if (coverOverlayContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(coverOverlayModifier),
                            content = coverOverlayContent,
                        )
                    }
                }

                Column(
                    modifier = infoModifier
                        .weight(1f)
                        .heightIn(min = resolvedMinimumHeight)
                        .padding(
                            top = infoPadding,
                            bottom = infoPadding,
                            end = infoEndPadding,
                        ),
                    verticalArrangement = infoArrangement,
                    content = infoContent,
                )
            }
        }

        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(HORIZONTAL_VIDEO_CARD_ACTION_INSET_DP.dp)
                    .widthIn(min = HORIZONTAL_VIDEO_CARD_ACTION_SIZE_DP.dp)
                    .heightIn(min = HORIZONTAL_VIDEO_CARD_ACTION_SIZE_DP.dp),
                contentAlignment = Alignment.Center,
                content = trailingContent,
            )
        }
    }
}
