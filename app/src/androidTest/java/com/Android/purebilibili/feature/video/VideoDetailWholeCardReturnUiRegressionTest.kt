package com.android.purebilibili.feature.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.purebilibili.feature.video.screen.videoDetailWholeCardReturnLayer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.launch

@RunWith(AndroidJUnit4::class)
class VideoDetailWholeCardReturnUiRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun nativeCoverBadgeAndInfoKeepOneGeometryThroughReturnAndCancellation() {
        val ratio = mutableFloatStateOf(4f / 3f)
        val alpha = mutableFloatStateOf(0f)
        val frozen = mutableStateOf<ImageBitmap?>(null)
        val coverColor = mutableStateOf(Color.Blue)
        val infoColor = mutableStateOf(Color.Yellow)
        lateinit var freeze: () -> Unit
        rule.setContent {
            val layer = rememberGraphicsLayer()
            val scope = rememberCoroutineScope()
            SideEffect {
                freeze = { scope.launch { frozen.value = layer.toImageBitmap() }; Unit }
            }
            Column {
                // Record the complete stationary card, including its cover badge and info plate.
                Column(Modifier.size(120.dp, (120f / ratio.floatValue + 30f).dp).drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }) {
                    Box(Modifier.fillMaxWidth().height((120f / ratio.floatValue).dp).background(coverColor.value)) {
                        Box(Modifier.align(Alignment.TopEnd).size(12.dp).background(Color.Green))
                    }
                    Box(Modifier.fillMaxWidth().height(30.dp).background(infoColor.value))
                }
                Box(Modifier.size(240.dp, 280.dp).background(Color.Magenta)
                    .videoDetailWholeCardReturnLayer(bitmap = frozen.value) { alpha.floatValue }.testTag("native-return")) {
                    Box(Modifier.fillMaxSize().background(Color.Red))
                }
            }
        }
        for (cardRatio in listOf(4f / 3f, 16f / 10f, 16f / 9f)) {
            rule.runOnIdle {
                ratio.floatValue = cardRatio
                coverColor.value = Color.Blue
                infoColor.value = Color.Yellow
                frozen.value = null
            }
            rule.runOnIdle { freeze() }
            rule.waitUntil(timeoutMillis = 5_000) { frozen.value != null }
            // A later source redraw must not mutate the already captured return pixels.
            rule.runOnIdle { coverColor.value = Color.Cyan; infoColor.value = Color.Gray }
            for (opacity in listOf(1f, 0.5f, 0f, 0.5f, 1f)) {
                rule.runOnIdle { alpha.floatValue = opacity }
                val pixels = rule.onNodeWithTag("native-return").captureToImage().toPixelMap()
                val density = pixels.width / 240f
                fun pixel(x: Float, y: Float) = pixels[(x * density).toInt(), (y * density).toInt()]
                if (opacity == 1f) {
                    val coverBottom = 240f / cardRatio
                    val cover = pixel(120f, coverBottom - 4f)
                    val info = pixel(120f, coverBottom + 20f)
                    val badge = pixel(230f, 10f)
                    val below = pixel(120f, coverBottom + 70f)
                    assertTrue("Cover cropped or changed at ratio=$cardRatio",
                        cover.blue > 0.9f && cover.red < 0.1f && cover.green < 0.1f)
                    assertTrue("Info detached at ratio=$cardRatio", info.red > 0.9f && info.green > 0.9f)
                    assertTrue("Badge did not follow card", badge.green > 0.9f && badge.blue < 0.1f)
                    assertTrue("Detail leaked below card", below.red > 0.9f && below.blue > 0.9f)
                } else if (opacity == 0f) {
                    val restored = pixel(120f, 50f)
                    assertTrue("Cancelled return did not restore detail", restored.red > 0.9f && restored.blue < 0.1f)
                }
            }
        }
    }
}
