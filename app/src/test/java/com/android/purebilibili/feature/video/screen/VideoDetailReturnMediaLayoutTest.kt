package com.android.purebilibili.feature.video.screen

import androidx.compose.ui.geometry.Rect
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoDetailReturnMediaLayoutTest {
    @Test
    fun stackedCardUsesMeasuredCoverGeometry() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(100f, 200f, 500f, 700f),
            sourceCoverBounds = Rect(100f, 200f, 500f, 440f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )

        assertTrue(layout.canRender)
        assertEquals(240f, layout.coverHeightPx)
        assertEquals(260f, layout.infoHeightPx)
    }

    @Test
    fun horizontalCardUsesMeasuredFullCardHeight() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(40f, 100f, 840f, 360f),
            sourceCoverBounds = Rect(40f, 100f, 320f, 300f),
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
        )

        assertTrue(layout.canRender)
        assertEquals(260f, layout.infoHeightPx)
        assertEquals(280f, layout.coverWidthPx)
    }

    @Test
    fun invalidBoundsDoNotProduceReturnGeometry() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = null,
            sourceCoverBounds = null,
        )

        assertFalse(layout.canRender)
    }

    @Test
    fun entryCoverMatchesManualStartViewportBelowStatusBar() {
        for (topInset in listOf(0, 72)) {
            val frame = resolveVideoDetailReturnMediaLayoutFrame(
                containerWidthPx = 1088,
                containerHeightPx = 612 + topInset,
                landingLayout = null,
                handoffProgress = 0f,
                contentTopInsetPx = topInset,
            )
            assertEquals(0, frame.offsetXPx)
            assertEquals(topInset, frame.offsetYPx)
            assertEquals(1088, frame.widthPx)
            assertEquals(612, frame.heightPx)
        }
    }

    @Test
    fun statusBarInsetFadesOutOfReturnGeometryWithoutChangingCardLanding() {
        val landing = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1088f,
            sourceBounds = Rect(0f, 0f, 544f, 420f),
            sourceCoverBounds = Rect(0f, 0f, 544f, 306f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )
        fun frame(progress: Float, inset: Int) = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1088,
            containerHeightPx = 684,
            landingLayout = landing,
            handoffProgress = progress,
            contentTopInsetPx = inset,
        )
        assertEquals(72, frame(0f, 72).offsetYPx)
        assertEquals(612, frame(0f, 72).heightPx)
        assertEquals(36, frame(0.5f, 72).offsetYPx)
        assertEquals(frame(1f, 0), frame(1f, 72))
    }

    @Test
    fun coverInsetCannotProduceEmptyOrNegativeMediaSize() {
        for (inset in listOf(-20, 1000)) {
            val frame = resolveVideoDetailReturnMediaLayoutFrame(
                containerWidthPx = 1088,
                containerHeightPx = 100,
                landingLayout = null,
                handoffProgress = 0f,
                contentTopInsetPx = inset,
            )
            assertTrue(frame.heightPx >= 1)
            assertEquals(100, frame.offsetYPx + frame.heightPx)
        }
    }

    @Test
    fun mediaFrameInterpolatesToMeasuredCover() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(0f, 0f, 500f, 300f),
            sourceCoverBounds = Rect(0f, 0f, 200f, 180f),
            sourceLayout = VideoCardSourceLayout.SIDE_BY_SIDE,
        )
        val frame = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
        )

        assertEquals(400, frame.widthPx)
        // Side-by-side media overdraws one source pixel below the landing edge. The outer
        // shared-bounds clip removes the excess and prevents the black player layer peeking out.
        assertEquals(362, frame.heightPx)
    }

    @Test
    fun mediaFrameUsesCurrentInverseYSoCoverTracksTheClip() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(0f, 0f, 500f, 300f),
            sourceCoverBounds = Rect(0f, 0f, 500f, 240f),
            sourceLayout = VideoCardSourceLayout.STACKED,
        )
        val frozen = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
        )
        val clipTracked = resolveVideoDetailReturnMediaLayoutFrame(
            containerWidthPx = 1000,
            containerHeightPx = 600,
            landingLayout = layout,
            handoffProgress = 1f,
            inverseScaleX = 2f,
            inverseScaleY = 2.4f,
        )

        assertEquals(1000, frozen.widthPx)
        assertEquals(480, frozen.heightPx)
        assertEquals(1000, clipTracked.widthPx)
        assertEquals(576, clipTracked.heightPx)
    }

    @Test
    fun playerLayoutResolvesTheMediaFrameOncePerNodeSize() {
        val source = File(
            "src/main/java/com/android/purebilibili/feature/video/screen/" +
                "VideoDetailReturnMediaLayout.kt"
        ).readText()
        val player = source.substringAfter("internal fun Modifier.videoDetailReturnPlayerLayout(")
        // layout 与 draw 在同一帧各解析一次 frame 会重复插值、逆缩放读取与对象分配；
        // 返回手势期间逐帧命中，因此必须按节点尺寸与输入做记忆化。
        assertTrue(player.contains("var memoFrame: VideoDetailReturnMediaLayoutFrame? = null"))
        assertTrue(player.contains("nativeBounds == memoNativeBounds"))
        assertTrue(player.contains("return cached"))
        assertTrue(player.contains("memoFrame = resolved"))
    }

    @Test
    fun stackedCardPrefersMeasuredInfoBandOverSubtraction() {
        val layout = resolveVideoDetailReturnSourceCardLayout(
            viewportWidthPx = 1000f,
            sourceBounds = Rect(100f, 200f, 500f, 900f),
            sourceCoverBounds = Rect(100f, 200f, 500f, 640f),
            sourceLayout = VideoCardSourceLayout.STACKED,
            sourceInfoBounds = Rect(100f, 640f, 500f, 880f),
        )

        assertTrue(layout.canRender)
        // 实测信息面 240px；「卡底 900 − 封面底 640」的减法推导会得到 260px，
        // 差的这 20px 就是卡槽里没被盖住的那段近白带。
        assertEquals(240f, layout.infoHeightPx)
        // infoTop 相对卡片左上角 = 640 − 200 = 440；sourceScale = 400 / 1000 = 0.4。
        assertEquals(1100f, layout.infoAnchorYInViewportPx, 0.001f)
    }

    @Test
    fun stackedCardFallsBackWhenMeasuredInfoBandIsDirty() {
        // 压在封面里 / 窄于整卡 / 越出卡底：任一脏数据都必须回退减法推导。
        for (dirty in listOf(
            Rect(120f, 300f, 300f, 500f),
            Rect(120f, 640f, 260f, 880f),
            Rect(100f, 640f, 500f, 1000f),
        )) {
            val layout = resolveVideoDetailReturnSourceCardLayout(
                viewportWidthPx = 1000f,
                sourceBounds = Rect(100f, 200f, 500f, 900f),
                sourceCoverBounds = Rect(100f, 200f, 500f, 640f),
                sourceLayout = VideoCardSourceLayout.STACKED,
                sourceInfoBounds = dirty,
            )

            assertTrue(layout.canRender)
            assertEquals(260f, layout.infoHeightPx)
            assertEquals(440f / 0.4f, layout.infoAnchorYInViewportPx, 0.001f)
        }
    }
}
