package com.android.purebilibili.core.ui.animation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LiquidIndicatorWobbleTest {
    @Test
    fun `release hands velocity deformation over without a release frame cut`() {
        for (velocity in listOf(-2f, 2f)) {
            val wobble = LiquidIndicatorWobble(0f)
            wobble.onInteractionStarted()
            wobble.advance(0.5f, frame, manualInteraction = true)
            val heldX = LiquidIndicatorMotion.scaleX(1.39f, velocity)
            val heldY = LiquidIndicatorMotion.scaleY(1.39f, velocity)
            wobble.onInteractionReleased()
            assertEquals(heldX, LiquidIndicatorMotion.scaleX(1.39f, velocity * wobble.velocityWeight))
            assertEquals(heldY, LiquidIndicatorMotion.scaleY(1.39f, velocity * wobble.velocityWeight))
            var previousWeight = wobble.velocityWeight
            repeat(9) {
                wobble.advance(0.5f, frame)
                assertTrue(wobble.velocityWeight <= previousWeight)
                previousWeight = wobble.velocityWeight
            }
            assertEquals(0f, wobble.velocityWeight)
            assertTrue(wobble.stretch != 0f)
            wobble.onSelectionChanged(continuous = true)
            assertEquals(0f, wobble.velocityWeight)
            wobble.onInteractionStarted()
            assertEquals(1f, wobble.velocityWeight)
        }
    }

    @Test
    fun `material follows final bloom and recoil including zero crossings and undershoot`() {
        val bloom = 1.39f
        for (stretch in listOf(-0.12f, -0.01f, 0f, 0.01f, 0.08f)) {
            val progress = liquidIndicatorShapeProgress(
                liquidWobbleScaleX(bloom, stretch), liquidWobbleScaleY(bloom, stretch), bloom,
            )
            assertTrue(progress > 0f)
        }
        assertEquals(0f, liquidIndicatorShapeProgress(1f, 1f, bloom))
        assertTrue(liquidIndicatorShapeProgress(0.99f, 1.014f, bloom) > 0f)
        assertTrue(liquidIndicatorShapeProgress(1.1f, 0.9f, 1f).isFinite())
    }

    @Test
    fun `busy selection frame keeps its shape instead of clearing and rearming at first tab`() {
        val wobble = LiquidIndicatorWobble(1f)
        wobble.onSelectionChanged(continuous = true)
        repeat(8) {
            wobble.advance(1f - (it + 1) / 10f, frame, holdSelectionMotion = true)
        }
        assertTrue(wobble.stretch != 0f)
        assertTrue(wobble.advance(0f, 150_000_000L, canSettle = false, holdSelectionMotion = true))
        assertTrue(wobble.isSelectionMotion)
        assertTrue(wobble.stretch != 0f)
        repeat(180) { wobble.advance(0f, frame) }
        assertEquals(0f, wobble.stretch)
        repeat(30) { assertFalse(wobble.advance(0f, frame)) }
    }

    @Test
    fun `release starts squash on the next frame while position and bloom still settle`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onInteractionStarted()
        repeat(12) {
            wobble.advance((it + 1) / 20f, frame, canSettle = false, manualInteraction = true)
        }
        wobble.onInteractionReleased()
        assertTrue(wobble.advance(0.7f, frame, canSettle = false))
        assertTrue(wobble.stretch < -0.01f)
        assertTrue(wobble.isSelectionMotion)
        // Continuing the position settle must not cancel or restart the shape spring.
        wobble.advance(0.8f, frame, canSettle = false)
        assertTrue(wobble.stretch < -0.01f)
        repeat(120) { wobble.advance(1f, frame, canSettle = false) }
        assertEquals(0f, wobble.stretch)
        assertFalse(wobble.isSelectionMotion)
        repeat(30) { assertFalse(wobble.advance(1f, frame)) }
    }

    @Test
    fun `short drag released between frame samples still recoils immediately`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onInteractionStarted()
        wobble.onInteractionReleased(nextPosition = 0.2f)
        wobble.advance(0.2f, frame, canSettle = false)
        assertTrue(wobble.stretch < -0.01f)
    }

    @Test
    fun `stationary pointer release has no unsolicited recoil`() {
        val wobble = LiquidIndicatorWobble(1f)
        wobble.onInteractionStarted()
        wobble.advance(1f, frame, canSettle = false, manualInteraction = true)
        wobble.onInteractionReleased()
        repeat(60) {
            assertFalse(wobble.advance(1f, frame))
            assertEquals(0f, wobble.stretch)
        }
    }

    @Test
    fun `tap landing between samples receives one response and returns to rest`() {
        val wobble = LiquidIndicatorWobble(1f)
        wobble.onSelectionChanged(continuous = true)
        assertTrue(wobble.advance(1f, frame, canSettle = false))
        assertTrue(wobble.stretch > 0.005f)
        repeat(120) { wobble.advance(1f, frame) }
        assertEquals(0f, wobble.stretch)
        repeat(30) { assertFalse(wobble.advance(1f, frame)) }
    }

    @Test
    fun `pointer takeover clears old tap motion before the next frame`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged(continuous = true)
        wobble.advance(0.2f, frame, holdSelectionMotion = true)
        assertTrue(wobble.stretch != 0f)
        wobble.onInteractionStarted()
        assertEquals(0f, wobble.stretch)
        assertFalse(wobble.isSelectionMotion)
    }

    @Test
    fun `reversing a tap mid travel preserves its shape and settles once`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged(continuous = true)
        repeat(8) { wobble.advance((it + 1) / 10f, frame, holdSelectionMotion = true) }
        val before = wobble.stretch
        wobble.onSelectionChanged(continuous = true)
        assertEquals(before, wobble.stretch)
        repeat(8) {
            wobble.advance(0.8f - (it + 1) / 10f, frame, holdSelectionMotion = true)
            assertTrue(wobble.stretch.isFinite())
            assertTrue(kotlin.math.abs(wobble.stretch) < 0.3f)
        }
        repeat(180) { wobble.advance(0f, frame) }
        assertEquals(0f, wobble.stretch)
        repeat(30) { assertFalse(wobble.advance(0f, frame)) }
    }

    @Test
    fun `release recoil converges consistently at 60 90 and 120 hz`() {
        val samples = listOf(60, 90, 120).map { fps ->
            val wobble = LiquidIndicatorWobble(0f)
            val step = 1_000_000_000L / fps
            wobble.onInteractionStarted()
            repeat(fps / 5) {
                wobble.advance((it + 1) * 5f / fps, step,
                    canSettle = false, manualInteraction = true)
            }
            wobble.onInteractionReleased()
            var minimum = 0f
            repeat(fps * 2) {
                wobble.advance(1f, step, canSettle = false)
                minimum = minOf(minimum, wobble.stretch)
            }
            assertEquals(0f, wobble.stretch)
            assertFalse(wobble.isSelectionMotion)
            minimum
        }
        assertTrue(samples.max() - samples.min() < 0.005f)
    }

    @Test
    fun `press bloom keeps the original multiplicative squash and stretch`() {
        val base = 78f / 56f
        for (stretch in listOf(-0.18f, -0.1f, 0f, 0.1f, 0.18f)) {
            assertEquals(base * (1f + stretch), liquidWobbleScaleX(base, stretch))
            assertEquals(base * (1f - stretch * 1.4f), liquidWobbleScaleY(base, stretch))
        }
    }

    @Test
    fun `endpoint correction remains continuous across the old contact threshold`() {
        for (base in listOf(1f, 78f / 56f)) {
            for (stretch in listOf(-0.12f, 0.04f)) {
                for (position in listOf(0.02f, 0.35f, 2.65f, 2.98f)) {
                    val before = liquidWobbleEdgeTranslationPx(stretch, position - 0.0001f, 3, 100f, 4f, base)
                    val after = liquidWobbleEdgeTranslationPx(stretch, position + 0.0001f, 3, 100f, 4f, base)
                    assertTrue(kotlin.math.abs(after - before) < 0.1f)
                }
            }
        }
    }

    @Test
    fun `four dp edge contact compensates remaining press magnification on both endpoints`() {
        val extent = 100f
        val gap = 4f
        for (scale in listOf(1f, 1.05f, 1.35f)) {
            for (stretch in listOf(-0.1f, -0.12f, -0.16f)) {
                val shift = liquidWobbleEdgeTranslationPx(stretch, 0f, 3, extent, gap, scale)
                val actualExtent = extent * scale * (1f + stretch)
                val edge = gap + shift + (extent - actualExtent) / 2f
                assertEquals(0f, edge, 0.0001f)
                assertEquals(-shift,
                    liquidWobbleEdgeTranslationPx(stretch, 3f, 3, extent, gap, scale), 0.0001f)
            }
        }
        // Ordinary press/drag remains untouched when no new shape is being drawn.
        assertEquals(0f, liquidWobbleEdgeTranslationPx(0f, 0f, 3, extent, gap, 1.35f))
    }

    @Test
    fun `magnified first and last tabs do not snap sideways as squash crosses zero`() {
        for (position in listOf(0f, 3f)) {
            for (base in listOf(1f, 78f / 56f)) {
                val before = liquidWobbleEdgeTranslationPx(-0.001f, position, 3, 100f, 4f, base)
                val after = liquidWobbleEdgeTranslationPx(0.001f, position, 3, 100f, 4f, base)
                val unscaledBefore = liquidWobbleEdgeTranslationPx(-0.001f, position, 3, 100f, 4f, 1f)
                val unscaledAfter = liquidWobbleEdgeTranslationPx(0.001f, position, 3, 100f, 4f, 1f)
                assertTrue(kotlin.math.abs(before - unscaledBefore) < 0.01f)
                assertTrue(kotlin.math.abs(after - unscaledAfter) < 0.01f)
            }
        }
    }

    @Test
    fun `tap shape travels with the indicator and settles without a second landing impulse`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged(continuous = true)
        var travelledShape = false
        repeat(24) {
            // Ease-out path to an interior tab, with canSettle false throughout travel.
            val t = (it + 1) / 24f
            val position = 1f - (1f - t) * (1f - t) * (1f - t)
            wobble.advance(position, 16_666_667L, canSettle = false, holdSelectionMotion = true)
            travelledShape = travelledShape || kotlin.math.abs(wobble.stretch) > 0.005f
        }
        assertTrue(travelledShape)
        assertTrue(wobble.isSelectionMotion)
        repeat(180) { wobble.advance(1f, 16_666_667L) }
        assertFalse(wobble.isSelectionMotion)
        assertEquals(0f, wobble.stretch)
        repeat(60) { assertFalse(wobble.advance(1f, 16_666_667L)) }
    }

    @Test
    fun `manual drag immediately interrupts continuous tap shape and keeps its original shape`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged(continuous = true)
        repeat(10) { wobble.advance((it + 1) / 20f, 16_666_667L, holdSelectionMotion = true) }
        assertTrue(wobble.stretch != 0f)
        wobble.advance(0.6f, 16_666_667L, canSettle = false, manualInteraction = true)
        assertFalse(wobble.isSelectionMotion)
        assertEquals(0f, wobble.stretch)
        repeat(10) {
            wobble.advance(0.6f + (it + 1) / 100f, 16_666_667L,
                canSettle = false, manualInteraction = true)
            assertEquals(0f, wobble.stretch)
        }
    }

    @Test
    fun `rapid tap retargeting preserves the existing shape rather than resetting it`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged(continuous = true)
        repeat(10) { wobble.advance((it + 1) / 20f, 16_666_667L, holdSelectionMotion = true) }
        val before = wobble.stretch
        assertTrue(before != 0f)
        wobble.onSelectionChanged(continuous = true)
        assertEquals(before, wobble.stretch)
    }

    @Test
    fun `endpoint squash reaches the shell and rebounds away without crossing it`() {
        val width = 100f
        val inset = 3f
        for (stretch in listOf(-0.12f, -0.16f)) {
            val leftShift = liquidWobbleEdgeTranslationPx(stretch, 0f, 3, width, inset)
            val rightShift = liquidWobbleEdgeTranslationPx(stretch, 3f, 3, width, inset)
            assertEquals(0f, inset + leftShift - width * stretch / 2f, 0.0001f)
            assertEquals(-leftShift, rightShift, 0.0001f)
        }
        val rebound = 0.04f
        val reboundShift = liquidWobbleEdgeTranslationPx(rebound, 0f, 3, width, inset)
        assertTrue(inset + reboundShift - width * rebound / 2f > inset)
        assertEquals(0f, liquidWobbleEdgeTranslationPx(-0.15f, 1f, 3, width, inset))
        assertEquals(0f, liquidWobbleEdgeTranslationPx(0f, 0f, 3, width, inset))
    }

    @Test
    fun `slow drag still has a visible landing squash without deforming during movement`() {
        val wobble = LiquidIndicatorWobble(0f)
        repeat(20) {
            wobble.advance((it + 1) * 0.005f, 16_666_667L, canSettle = false)
            assertEquals(0f, wobble.stretch)
        }
        var minimum = 0f
        repeat(90) {
            wobble.advance(0.1f, 16_666_667L)
            minimum = minOf(minimum, wobble.stretch)
        }
        assertTrue(minimum < -0.1f)
    }

    private val frame = 16_666_667L

    @Test
    fun `selection change recoils without requiring sampled drag distance`() {
        // A click can land between two samples, leaving the observer at its final position.
        val wobble = LiquidIndicatorWobble(1f)
        wobble.onSelectionChanged()
        repeat(30) {
            assertFalse(wobble.advance(1f, frame, canSettle = false))
            assertEquals(0f, wobble.stretch)
        }
        var recoiled = false
        repeat(180) {
            wobble.advance(1f, frame)
            recoiled = recoiled || wobble.stretch < -0.001f
        }
        assertTrue(recoiled)
        assertEquals(0f, wobble.stretch)
        repeat(60) { assertFalse(wobble.advance(1f, frame)) }
    }

    @Test
    fun `rapid click selections supersede a playing recoil and wait for the latest landing`() {
        val wobble = LiquidIndicatorWobble(0f)
        wobble.onSelectionChanged()
        repeat(10) { wobble.advance(0f, frame) }
        assertTrue(wobble.stretch != 0f)
        wobble.onSelectionChanged()
        assertEquals(0f, wobble.stretch)
        wobble.advance(0.001f, frame, canSettle = false)
        repeat(10) { wobble.advance(0.001f, frame, canSettle = false) }
        assertEquals(0f, wobble.stretch)
        var recoiled = false
        repeat(180) {
            wobble.advance(0.001f, frame)
            recoiled = recoiled || wobble.stretch < -0.001f
        }
        assertTrue(recoiled)
        assertEquals(0f, wobble.stretch)
    }

    @Test
    fun `rendering a new drag suppresses the previous tail before its worker resumes`() {
        for (tail in listOf(-0.1f, 0.05f)) {
            assertEquals(0f, resolveLiquidIndicatorRecoilStretch(tail, canSettle = false))
            assertEquals(tail, resolveLiquidIndicatorRecoilStretch(tail, canSettle = true))
        }
    }

    @Test
    fun `moving adds no shape and stopping plays one recoil then returns exactly to rest`() {
        val wobble = LiquidIndicatorWobble(0f)
        repeat(18) {
            assertTrue(wobble.advance((it + 1) / 18f, frame))
            assertEquals(0f, wobble.stretch)
            assertEquals(1.2f, liquidWobbleScaleX(1.2f, wobble.stretch))
            assertEquals(0.9f, liquidWobbleScaleY(0.9f, wobble.stretch))
        }
        var compressed = false
        var rebounded = false
        repeat(180) {
            wobble.advance(1f, frame)
            compressed = compressed || wobble.stretch < -0.001f
            rebounded = rebounded || wobble.stretch > 0.001f
        }
        assertTrue(compressed)
        assertTrue(rebounded)
        assertEquals(0f, wobble.stretch)
        repeat(60) { assertFalse(wobble.advance(1f, frame)) }
    }

    @Test
    fun `held gesture and residual original velocity block the recoil`() {
        val wobble = LiquidIndicatorWobble(0f)
        repeat(18) { wobble.advance((it + 1) / 18f, frame, canSettle = false) }
        repeat(60) {
            assertFalse(wobble.advance(1f, frame, canSettle = false))
            assertEquals(0f, wobble.stretch)
        }
        var recoiled = false
        repeat(30) {
            wobble.advance(1f, frame, canSettle = true)
            recoiled = recoiled || wobble.stretch < -0.001f
        }
        assertTrue(recoiled)
    }

    @Test
    fun `new movement or stationary press cancels a playing recoil`() {
        val wobble = LiquidIndicatorWobble(0f)
        repeat(18) { wobble.advance((it + 1) / 18f, frame) }
        repeat(10) { wobble.advance(1f, frame) }
        assertTrue(wobble.stretch != 0f)
        wobble.advance(1.1f, frame)
        assertEquals(0f, wobble.stretch)
        repeat(10) { wobble.advance(1.1f, frame) }
        assertTrue(wobble.stretch != 0f)
        assertFalse(wobble.advance(1.1f, frame, canSettle = false))
        assertEquals(0f, wobble.stretch)
        repeat(60) { assertFalse(wobble.advance(1.1f, frame)) }
    }

    @Test
    fun `both directions recoil equally and idle taps do not arm recoil`() {
        val forward = LiquidIndicatorWobble(0f)
        val backward = LiquidIndicatorWobble(4f)
        repeat(18) {
            val position = (it + 1) / 18f
            forward.advance(position, frame)
            backward.advance(4f - position, frame)
        }
        repeat(120) {
            forward.advance(1f, frame)
            backward.advance(3f, frame)
            assertEquals(forward.stretch, backward.stretch, 0.0001f)
        }
        val idle = LiquidIndicatorWobble(2f)
        repeat(10) { assertFalse(idle.advance(2f, frame, canSettle = false)) }
        repeat(60) { assertFalse(idle.advance(2f, frame)) }
    }

    @Test
    fun `suspension or invalid samples clear the pending recoil`() {
        val wobble = LiquidIndicatorWobble(0f)
        repeat(10) { wobble.advance((it + 1) / 10f, frame) }
        assertFalse(wobble.advance(1f, 1_000_000_000L))
        repeat(60) { assertFalse(wobble.advance(1f, frame)) }
        wobble.advance(2f, frame)
        assertFalse(wobble.advance(Float.NaN, frame))
        assertEquals(0f, wobble.stretch)
        repeat(60) { assertFalse(wobble.advance(2f, frame)) }
    }
}
