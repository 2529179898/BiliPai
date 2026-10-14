package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.core.store.PlayerProgressPlacement

data class BottomControlBarLayoutPolicy(
    val bottomPaddingDp: Int,
    val progressSpacingDp: Int,
    val horizontalPaddingDp: Int,
    val playButtonSizeDp: Int,
    val playIconSizeDp: Int,
    val timeFontSp: Int,
    val afterTimeSpacingDp: Int,
    val danmakuIconSizeDp: Int,
    val rightActionSpacingDp: Int,
    val actionChipVerticalPaddingDp: Int,
    val actionTextFontSp: Int,
    val fullscreenIconSizeDp: Int
)

/**
 * Keeps the visible gap between time text and an icon near one text size. The
 * actual layout slot contributes blank space around its glyph; touch expansion
 * is independent of the compact row's measured size.
 */
internal fun resolveTimeToIconSpacingDp(
    timeFontSp: Int,
    iconSizeDp: Int,
    slotSizeDp: Int
): Int {
    val iconInsetDp = ((slotSizeDp - iconSizeDp) / 2).coerceAtLeast(0)
    return (timeFontSp - iconInsetDp).coerceAtLeast(0)
}

internal fun resolveBottomControlBarBottomPaddingDp(
    defaultBottomPaddingDp: Int,
    progressPlacement: PlayerProgressPlacement
): Int {
    return if (progressPlacement == PlayerProgressPlacement.BOTTOM_EDGE) {
        0
    } else {
        defaultBottomPaddingDp
    }
}

/**
 * Keeps the inline detail player's scrubber anchored to the video edge.
 *
 * The phone detail player is always drawn edge-to-edge, including when the transparent system
 * status bar remains visible. Placing the progress bar above the controls makes it appear in
 * the middle of the video frame.
 */
internal fun resolveVideoDetailProgressPlacement(
    requestedPlacement: PlayerProgressPlacement,
    isFullscreen: Boolean
): PlayerProgressPlacement {
    return if (!isFullscreen) {
        PlayerProgressPlacement.BOTTOM_EDGE
    } else {
        requestedPlacement
    }
}

fun resolveBottomControlBarLayoutPolicy(
    widthDp: Int,
    compact: Boolean = false
): BottomControlBarLayoutPolicy {
    if (widthDp >= 1600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 7,
            progressSpacingDp = if (compact) 5 else 8,
            horizontalPaddingDp = if (compact) 10 else 20,
            playButtonSizeDp = 48,
            playIconSizeDp = 28,
            timeFontSp = 14,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(timeFontSp = 14, iconSizeDp = 28, slotSizeDp = 48),
            danmakuIconSizeDp = 28,
            rightActionSpacingDp = 16,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 16,
            fullscreenIconSizeDp = 26
        )
    }

    if (widthDp >= 840) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 5,
            progressSpacingDp = if (compact) 4 else 6,
            horizontalPaddingDp = if (compact) 8 else 16,
            playButtonSizeDp = 40,
            playIconSizeDp = 26,
            timeFontSp = 12,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(timeFontSp = 12, iconSizeDp = 24, slotSizeDp = 40),
            danmakuIconSizeDp = 24,
            rightActionSpacingDp = 14,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 14,
            fullscreenIconSizeDp = 22
        )
    }

    if (widthDp >= 600) {
        return BottomControlBarLayoutPolicy(
            bottomPaddingDp = 5,
            progressSpacingDp = if (compact) 3 else 5,
            horizontalPaddingDp = if (compact) 6 else 12,
            playButtonSizeDp = 36,
            playIconSizeDp = 24,
            timeFontSp = 12,
            afterTimeSpacingDp = resolveTimeToIconSpacingDp(timeFontSp = 12, iconSizeDp = 22, slotSizeDp = 36),
            danmakuIconSizeDp = 22,
            rightActionSpacingDp = 14,
            actionChipVerticalPaddingDp = 2,
            actionTextFontSp = 13,
            fullscreenIconSizeDp = 20
        )
    }

    return BottomControlBarLayoutPolicy(
        bottomPaddingDp = 4,
        progressSpacingDp = if (compact) 3 else 0,
        horizontalPaddingDp = if (compact) 4 else 6,
        playButtonSizeDp = 32,
        playIconSizeDp = 22,
        timeFontSp = 11,
        afterTimeSpacingDp = resolveTimeToIconSpacingDp(timeFontSp = 11, iconSizeDp = 20, slotSizeDp = 32),
        danmakuIconSizeDp = 20,
        rightActionSpacingDp = 8,
        actionChipVerticalPaddingDp = 2,
        actionTextFontSp = 12,
        fullscreenIconSizeDp = 18
    )
}
