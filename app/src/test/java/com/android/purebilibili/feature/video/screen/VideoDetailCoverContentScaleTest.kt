package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoDetailCoverContentScaleTest {
    @Test
    fun wideCoverFitsCompletelyAtRestAndRestoresListCropContinuously() {
        val source = Size(160f, 90f)
        val slot = Size(160f, 100f)
        var progress = 0f
        val scale = VideoDetailCoverContentScale { progress }
        assertEquals(ContentScale.Fit.computeScaleFactor(source, slot), scale.computeScaleFactor(source, slot))
        progress = 0.5f
        val halfway = scale.computeScaleFactor(source, slot)
        assertEquals((1f + 100f / 90f) / 2f, halfway.scaleX, 0.0001f)
        assertEquals(halfway.scaleX, halfway.scaleY)
        progress = 1f
        assertEquals(ContentScale.Crop.computeScaleFactor(source, slot), scale.computeScaleFactor(source, slot))
    }

    @Test
    fun matchingSourceRatioDoesNotChangeScaleDuringHandoff() {
        val source = Size(320f, 200f)
        val slot = Size(160f, 100f)
        for (progress in listOf(0f, 0.25f, 0.5f, 1f)) {
            val result = VideoDetailCoverContentScale { progress }.computeScaleFactor(source, slot)
            assertEquals(0.5f, result.scaleX)
            assertEquals(result.scaleX, result.scaleY)
        }
    }
}
