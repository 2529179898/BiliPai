/*
 * Avatar fusion hosted from NextAlone/Nagram ProfileGooeyView.java,
 * revision fb56ea9801e5de68c60f0152b444bbe2a0fda520; GPL-3.0.
 * Telegram and Nagram contributors.
 */
package com.android.purebilibili.feature.profile

import android.widget.FrameLayout
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.android.purebilibili.core.ui.LocalComponentMotionEnabled
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import com.android.purebilibili.core.ui.performance.isLowBlurBudgetForced
import com.android.purebilibili.feature.home.UserState

/** Keep native fusion off the page backdrop; only the avatar/cutout enter its buffers. */
@Composable
internal fun ProfileAvatarEffect(
    user: UserState,
    expandedFractionProvider: () -> Float,
    statusBarTopPadding: Dp,
    borderColor: Color,
    listState: LazyListState,
    flingBehavior: FlingBehavior,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val parentComposition = rememberCompositionContext()
    val latestUser = rememberUpdatedState(user)
    val latestBorder = rememberUpdatedState(borderColor)
    val latestClick = rememberUpdatedState(onAvatarClick)
    val latestFling = rememberUpdatedState(flingBehavior)
    val lowBudget = isLowBlurBudgetForced()
    val motionEnabled = LocalComponentMotionEnabled.current && !rememberSystemReduceMotion() && !lowBudget
    val latestMotion = rememberUpdatedState(motionEnabled)
    val latestProvider = rememberUpdatedState(expandedFractionProvider)
    val latestTop = rememberUpdatedState(with(density) { statusBarTopPadding.toPx() })
    val host = remember { arrayOfNulls<ProfileAvatarFusionView>(1) }
    AndroidView(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                host[0]?.setViewportOrigin(position.x, position.y)
            }
            .drawWithContent {
                // Observe scroll in Compose's draw phase, including reverse gestures after disappearance.
                latestProvider.value()
                host[0]?.prepareFrame()
                this@drawWithContent.drawContent()
            },
        factory = { context ->
            ProfileAvatarFusionView(context, !lowBudget).apply {
                host[0] = this
                val avatar = ComposeView(context).apply {
                    setParentCompositionContext(parentComposition)
                    pivotX = 0f
                    pivotY = 0f
                    setContent {
                        val currentUser = latestUser.value
                        AsyncImage(
                            model = currentUser.face,
                            contentDescription = "${currentUser.name}的头像，编辑个人资料",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                                .clip(CircleShape)
                                .border(2.dp, latestBorder.value, CircleShape)
                                .scrollable(listState, Orientation.Vertical, reverseDirection = true, flingBehavior = latestFling.value)
                                .clickable { latestClick.value() },
                        )
                    }
                }
                val baseSize = (100f * context.resources.displayMetrics.density).toInt()
                addView(avatar, FrameLayout.LayoutParams(baseSize, baseSize))
                setFrameUpdater {
                    val expanded = latestProvider.value().coerceIn(0f, 1f)
                    val deviceDensity = context.resources.displayMetrics.density
                    val cutout = notchInfo
                    // Same centered-avatar end size and Y endpoint as the upstream ProfileActivity.
                    val endSize = if (cutout != null && cutout.isLikelyCircle) {
                        0.5f * cutout.bounds.width() / deviceDensity
                    } else 24f
                    val scale = (endSize + (96f - endSize) * expanded) / 100f
                    val endY = if (cutout != null && cutout.isLikelyCircle) cutout.bounds.centerY() else -29f * deviceDensity
                    val startY = latestTop.value + 35f * deviceDensity
                    avatar.scaleX = scale
                    avatar.scaleY = scale
                    avatar.translationX = (width - baseSize * scale) / 2f
                    avatar.translationY = endY + (startY - endY) * expanded
                    setStatusBarHeight(latestTop.value.toInt())
                    updateFrame(1f - expanded, latestMotion.value)
                }
            }
        },
        onRelease = { view ->
            (view.getChildAt(0) as? ComposeView)?.disposeComposition()
            view.release()
            if (host[0] === view) host[0] = null
        },
    )
}
