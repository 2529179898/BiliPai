package com.android.purebilibili.feature.video.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import com.android.purebilibili.core.ui.transition.VideoCardSourceLayout
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.core.ui.transition.normalizeSharedElementSourceRoute
import com.android.purebilibili.core.util.CardPositionManager

/** 仅约束飞行详情内的独立封面/角标层，不影响静止卡片和普通页面布局。 */
internal val LocalVideoDetailWholeCardReturnActive = compositionLocalOf<() -> Boolean> { { false } }

internal fun resolveVideoDetailWholeCardReturnAlpha(
    depth: Float,
    returning: Boolean,
    followProgress: Boolean,
    committedIdle: Boolean,
): Float {
    if (committedIdle) return 1f
    if (!returning) return 0f
    val progress = 1f - depth.coerceIn(0f, 1f)
    if (!followProgress) return if (progress >= 1f) 1f else 0f
    // 前段保留实时画面；整卡在末段统一接管，最后一小段已经是原卡片的完整像素。
    val fraction = ((progress - 0.55f) / (0.98f - 0.55f)).coerceIn(0f, 1f)
    return fraction * fraction * (3f - 2f * fraction)
}

internal class VideoDetailWholeCardReturn(
    val active: () -> Boolean,
    val modifier: Modifier,
)

/**
 * 手机、大屏及所有材质共用同一份点击时整卡像素快照。只做等比缩放，不重新布局封面和文字。
 * 录制资源按详情 entry 的视频与来源身份保留，直到 entry 退出；嵌套父页不得消费子页图层。
 */
@Composable
internal fun rememberVideoDetailWholeCardReturn(
    bvid: String,
    sourceRoute: String?,
    ownsTransition: Boolean,
    committedReturn: Boolean,
    keepLoadedContentForBackPreview: Boolean,
    sourceBounds: Rect?,
    sourceLayout: VideoCardSourceLayout,
    isNowPlayingBar: Boolean,
    depthProvider: () -> Float,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    gestureProvider: () -> Boolean,
    followProgressProvider: () -> Boolean,
): VideoDetailWholeCardReturn {
    var retained by remember(bvid, sourceRoute) { mutableStateOf<ImageBitmap?>(null) }
    val expectedKey = normalizeSharedElementSourceRoute(sourceRoute)?.let { "$it:${bvid.trim()}" }
    val bitmap = CardPositionManager.lastClickedWholeCardReturnBitmap
    val candidate = if (ownsTransition && !isNowPlayingBar &&
        sourceLayout != VideoCardSourceLayout.COVER_ONLY && expectedKey != null &&
        expectedKey == CardPositionManager.lastClickedVideoSourceKey &&
        bitmap != null && bitmap.width > 1 && bitmap.height > 1 &&
        sourceBounds != null && sourceBounds.width > 1f && sourceBounds.height > 1f
    ) bitmap else null
    SideEffect { if (candidate != null && retained != candidate) retained = candidate }
    val snapshot = candidate ?: retained
    val active: () -> Boolean = {
        snapshot != null && !keepLoadedContentForBackPreview &&
            (ownsTransition || committedReturn) &&
            (committedReturn || gestureProvider() ||
                phaseProvider() == VideoCardTransitionBackgroundPhase.RETURNING)
    }
    val modifier = Modifier.videoDetailWholeCardReturnLayer(bitmap = snapshot) {
        if (!active()) 0f else resolveVideoDetailWholeCardReturnAlpha(
            depth = depthProvider(),
            returning = true,
            followProgress = followProgressProvider(),
            committedIdle = committedReturn && !gestureProvider() &&
                phaseProvider() == VideoCardTransitionBackgroundPhase.IDLE,
        )
    }
    return VideoDetailWholeCardReturn(active, modifier)
}

/** 整卡像素在同一透明度组中绘制，避免封面、角标和信息区各自裁切或各自叠化。 */
internal fun Modifier.videoDetailWholeCardReturnLayer(
    bitmap: ImageBitmap?,
    alphaProvider: () -> Float,
): Modifier {
    val paint = Paint()
    return drawWithContent {
        val alpha = alphaProvider().coerceIn(0f, 1f)
        if (alpha <= 0f || bitmap == null || bitmap.width <= 1 || bitmap.height <= 1) {
            drawContent()
        } else {
            // 两个完整内容组交接；透明卡片区域也能退出详情内容，不残留播放器或第二套标题。
            drawIntoCanvas { canvas ->
                if (alpha < 1f) {
                    paint.alpha = 1f - alpha
                    canvas.saveLayer(Rect(Offset.Zero, size), paint)
                    try { this@drawWithContent.drawContent() } finally { canvas.restore() }
                }
                paint.alpha = alpha
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                try {
                    // FillWidthTop 的导航补偿已统一两轴比例，整卡只按宽度缩放一次。
                    val viewportWidth = size.width
                    val sourcePixelScale = viewportWidth / bitmap.width
                    scale(sourcePixelScale, sourcePixelScale, pivot = Offset.Zero) {
                        drawImage(bitmap)
                    }
                } finally { canvas.restore() }
            }
        }
    }
}
