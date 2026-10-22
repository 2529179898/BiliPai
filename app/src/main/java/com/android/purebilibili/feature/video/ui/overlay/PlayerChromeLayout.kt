package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared fullscreen edges and circular targets, measured inside display-cutout insets. */
internal object PlayerChromeLayout {
    const val EdgePaddingDp = 12
    // Visual/layout sizes shared across glass, MD3 and Miuix player chrome.
    const val CircularButtonSizeDp = 40
    const val SeekRowHeightDp = 32
    const val ActionGapDp = 8
    const val SeekEdgePaddingDp = 20
    const val FullscreenHeightScale = 0.9f
}

/**
 * PiliPlus reads Flutter's per-side `viewPadding`, so a landscape cutout on one edge shifts
 * its whole chrome. Mirror the larger side instead: the camera keeps its clearance and the
 * bars stay centered on screen, which avoids the "everything drifted right" look.
 */
@Composable
internal fun resolveSymmetricPlayerSideInset(
    insets: WindowInsets,
    minimum: Dp,
): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val left = with(density) { insets.getLeft(density, layoutDirection).toDp() }
    val right = with(density) { insets.getRight(density, layoutDirection).toDp() }
    return maxOf(left, right, minimum, 0.dp)
}
