package com.android.purebilibili.feature.settings

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SettingsSiblingIconPalettePolicyTest {
    @Test
    fun siblingNavigationRequestsNeutralIconsRegardlessOfCategoryOrOffset() {
        assertEquals(List(8) { Color.Unspecified }, resolveSettingsSiblingIconTints(8, 5))
        assertEquals(Color.Unspecified, resolveSettingsSiblingIconTint(0))
        assertEquals(Color.Unspecified, resolveSettingsSiblingIconTint(15, 7))
    }

    @Test
    fun longGroupsDoNotIntroduceNewColorsOrAnArbitraryPaletteLimit() {
        assertEquals(List(32) { Color.Unspecified }, resolveSettingsSiblingIconTints(32))
        assertEquals(emptyList(), resolveSettingsSiblingIconTints(0))
        assertFailsWith<IllegalArgumentException> { resolveSettingsSiblingIconTints(-1) }
    }
}
