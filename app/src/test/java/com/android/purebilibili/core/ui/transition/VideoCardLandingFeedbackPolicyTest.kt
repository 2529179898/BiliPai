package com.android.purebilibili.core.ui.transition

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoCardLandingFeedbackPolicyTest {
    private val stacked = VideoCardSourceLayout.STACKED
    private val horizontal = VideoCardSourceLayout.SIDE_BY_SIDE

    @Test
    fun horizontalMotionIsIndependentAndOtherProfilesKeepTheirValues() {
        val row = resolveVideoCardLandingMotion(horizontal)
        assertEquals(0.84f, row.dampingRatio)
        assertEquals(320f, row.stiffness)
        for (layout in listOf(stacked, VideoCardSourceLayout.COVER_ONLY)) {
            val motion = resolveVideoCardLandingMotion(layout)
            assertEquals(0.80f, motion.dampingRatio)
            assertEquals(400f, motion.stiffness)
        }
        for (density in listOf(1f, 2.75f, 3f, 4f)) {
            val scale = resolveVideoCardLandingScale(380f * density, 110f * density, density, 1.25f, horizontal)
            assertEquals(1.25f, (1f - scale) * 380f / 2f, 0.0001f)
            assertEquals(0.3618421f, (1f - scale) * 110f / 2f, 0.0001f)
        }
    }

    @Test
    fun dualColumnTileHasPerceptibleTravelWhileWideCardsStayRestrained() {
        val tile = resolveVideoCardLandingScale(180f, 240f, 1f, 1.1f, stacked)
        val wide = resolveVideoCardLandingScale(400f, 120f, 1f, 1.25f, horizontal)
        val tablet = resolveVideoCardLandingScale(800f, 200f, 1f, 1.25f, horizontal)
        assertEquals(0.9908333f, tile, 0.00001f)
        assertEquals(1.25f, (1f - wide) * 400f / 2f, 0.0001f)
        assertEquals(1.25f, (1f - tablet) * 800f / 2f, 0.0001f)
        assertTrue(tile < wide && wide < tablet)
    }

    @Test
    fun densityAndGeometryDoNotChangeTheLayoutSpecificTravelBudget() {
        val expected = resolveVideoCardLandingScale(180f, 240f, 1f, 1.1f, stacked)
        for (density in listOf(1f, 2f, 2.75f, 3f, 4f)) {
            assertEquals(expected, resolveVideoCardLandingScale(180f * density, 240f * density, density, 1.1f, stacked))
        }
        assertEquals(0.988f, resolveVideoCardLandingScale(100f, 60f, 1f, 1.1f, stacked), 0.0001f)
        assertEquals(0.991f, resolveVideoCardLandingScale(100f, 60f, 1f, 1.25f, horizontal), 0.0001f)
    }

    @Test
    fun impulseReachesItsBudgetWithoutSecondEnlargementOrLongVisibleTail() {
        for (layout in VideoCardSourceLayout.entries) {
            val motion = resolveVideoCardLandingMotion(layout)
            val omega = sqrt(motion.stiffness)
            val decay = motion.dampingRatio * omega
            val dampedOmega = omega * sqrt(1f - motion.dampingRatio * motion.dampingRatio)
            for (width in listOf(100f, 180f, 400f, 800f)) {
                val height = if (layout == stacked) width * 4f / 3f else width / 3f
                val budget = resolveVideoCardLandingEdgeCompressionDp(width, height, 1f, layout)
                val velocity = resolveVideoCardLandingImpulseVelocity(budget, layout)
                var peak = 0f
                for (ms in 0..500) {
                    val time = ms / 1000f
                    val edge = velocity * exp(-decay * time) * sin(dampedOmega * time) / dampedOmega
                    peak = maxOf(peak, edge)
                    val scale = resolveVideoCardLandingScale(width, height, 1f, edge, layout)
                    assertTrue(scale in 0.988f..1f)
                    if (ms >= 300) assertTrue(abs(edge) < 0.02f)
                }
                assertEquals(budget, peak, 0.001f)
            }
        }
    }

    @Test
    fun invalidGeometryAndNegativeSpringLobeStayAtIdentity() {
        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(1f, resolveVideoCardLandingScale(invalid, 100f, 1f, 1.1f, stacked))
            assertEquals(1f, resolveVideoCardLandingScale(200f, 100f, invalid, 1.1f, stacked))
        }
        assertEquals(1f, resolveVideoCardLandingScale(200f, 100f, 1f, -0.01f, stacked))
        assertEquals(1f, resolveVideoCardLandingScale(200f, 100f, 1f, Float.NaN, stacked))
    }
}
