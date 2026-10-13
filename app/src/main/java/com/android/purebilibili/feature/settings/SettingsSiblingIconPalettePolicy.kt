package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color

/** Legacy sibling assignments are neutral requests, resolved by the preference renderer. */
internal val SettingsSiblingIconPalette: List<Color> = List(16) { Color.Unspecified }

@Suppress("UNUSED_PARAMETER")
internal fun resolveSettingsSiblingIconTint(
    siblingIndex: Int,
    paletteOffset: Int = 0,
): Color = Color.Unspecified

@Suppress("UNUSED_PARAMETER")
internal fun resolveSettingsSiblingIconTints(
    siblingCount: Int,
    paletteOffset: Int = 0,
): List<Color> {
    require(siblingCount >= 0)
    return List(siblingCount) { Color.Unspecified }
}
