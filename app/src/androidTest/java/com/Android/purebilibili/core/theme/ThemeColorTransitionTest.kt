package com.android.purebilibili.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeColorTransitionTest {
    @get:Rule
    val rule = createComposeRule()

    private fun palette(dark: Boolean): AppThemePalette {
        val material = if (dark) darkColorScheme() else lightColorScheme()
        return AppThemePalette(
            material = material,
            miuix = resolveMiuixColorsFromMaterialBridge(createMiuixMaterialBridge(material), dark),
            liquidAccent = if (dark) Color(0xFF34C759) else Color(0xFF007AFF),
        )
    }

    @Test
    fun colorsShareProgressAndReverseToExactEndpointWithoutRemountingContent() {
        val light = palette(false)
        val dark = palette(true)
        val target = mutableStateOf(light)
        lateinit var visible: AppThemePalette
        var mounts = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val frame = rememberAnimatedAppThemePalette(target.value, enabled = true)
            androidx.compose.runtime.DisposableEffect(Unit) {
                mounts++
                onDispose { }
            }
            SideEffect { visible = frame }
        }
        rule.runOnIdle {
            assertEquals(light.material.background, visible.material.background)
            assertEquals(light.liquidAccent, visible.liquidAccent)
            target.value = dark
        }
        rule.mainClock.advanceTimeBy(96)
        rule.runOnIdle {
            assertNotEquals(light.material.background, visible.material.background)
            assertNotEquals(dark.material.background, visible.material.background)
            // Matching endpoint roles must remain identical on every intermediate frame.
            assertEquals(visible.material.primary, visible.miuix.primary)
            assertNotEquals(light.liquidAccent, visible.liquidAccent)
            assertNotEquals(dark.liquidAccent, visible.liquidAccent)
            target.value = light
        }
        rule.mainClock.advanceTimeBy(APP_THEME_COLOR_TRANSITION_DURATION_MS.toLong() + 64)
        rule.runOnIdle {
            assertEquals(light.material.background, visible.material.background)
            assertEquals(light.material.onSurface, visible.material.onSurface)
            assertEquals(light.miuix.surface, visible.miuix.surface)
            assertEquals(light.liquidAccent, visible.liquidAccent)
            assertEquals(1, mounts)
        }
    }

    @Test
    fun disablingMotionDuringTransitionImmediatelyExposesExactTarget() {
        val light = palette(false)
        val dark = palette(true)
        val target = mutableStateOf(light)
        val enabled = mutableStateOf(true)
        lateinit var visible: AppThemePalette
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val frame = rememberAnimatedAppThemePalette(target.value, enabled.value)
            SideEffect { visible = frame }
        }
        rule.runOnIdle { target.value = dark }
        rule.mainClock.advanceTimeBy(80)
        rule.runOnIdle { enabled.value = false }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle {
            assertEquals(dark, visible)
            target.value = light
        }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { assertEquals(light, visible) }
    }
}
