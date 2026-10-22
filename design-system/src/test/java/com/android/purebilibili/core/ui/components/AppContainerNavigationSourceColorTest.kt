package com.android.purebilibili.core.ui.components

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AppContainerNavigationSourceColorTest {
    @Test
    fun `transparent preference rows inherit the actual enclosing card tone`() {
        for (card in listOf(Color(0xFF242424), Color.White, Color.White.copy(alpha = 0.74f))) {
            assertEquals(card, resolveAppContainerNavigationSourceColor(Color.Unspecified, card))
        }
    }

    @Test
    fun `explicit colors including transparent override inherited card color`() {
        for (explicit in listOf(Color.Transparent, Color.Red, Color.White)) {
            assertEquals(explicit, resolveAppContainerNavigationSourceColor(explicit, Color(0xFF242424)))
        }
        assertEquals(Color.Unspecified,
            resolveAppContainerNavigationSourceColor(Color.Unspecified, Color.Unspecified))
    }
}
