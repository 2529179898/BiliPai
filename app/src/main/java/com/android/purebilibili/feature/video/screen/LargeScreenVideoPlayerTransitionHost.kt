package com.android.purebilibili.feature.video.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.transition.MiuixVideoCardTransitionState
import com.android.purebilibili.core.ui.transition.VideoCardTransitionBackgroundPhase
import com.android.purebilibili.navigation3.predictiveback.resolveMiuixVideoCardInverseScaleForDepth
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun Modifier.videoDetailTransitionChrome(alphaProvider: () -> Float): Modifier =
    graphicsLayer { alpha = alphaProvider().coerceIn(0f, 1f) }

/** Measurements are consumed in layout/draw, never in composition. */
internal class LargeScreenVideoTransitionGeometry {
    private var root: LayoutCoordinates? = null
    private var player: LayoutCoordinates? = null
    val playerOrigin = mutableStateOf(Offset.Zero)

    fun recordRoot(coordinates: LayoutCoordinates) {
        root = coordinates
        updateOrigin()
    }

    fun recordPlayer(coordinates: LayoutCoordinates) {
        player = coordinates
        updateOrigin()
    }

    private fun updateOrigin() {
        val ancestor = root?.takeIf { it.isAttached } ?: return
        val coordinates = player?.takeIf { it.isAttached } ?: return
        val next = ancestor.localBoundingBoxOf(coordinates, clipBounds = false).topLeft
        val previous = playerOrigin.value
        if (abs(previous.x - next.x) > 0.5f || abs(previous.y - next.y) > 0.5f) {
            playerOrigin.value = next
        }
    }
}

/** Retains the live player's measurement while its cover lands in entry-space source geometry. */
@Composable
internal fun LargeScreenVideoPlayerTransitionHost(
    geometry: LargeScreenVideoTransitionGeometry,
    transition: MiuixVideoCardTransitionState,
    phaseProvider: () -> VideoCardTransitionBackgroundPhase,
    isReturningProvider: () -> Boolean,
    isCommittedReturn: Boolean,
    coverSource: VideoDetailResidentCoverSource?,
    modifier: Modifier = Modifier,
    content: @Composable (Dp) -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fallbackWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val fallbackHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val landing = if (transition.enabled) resolveVideoDetailReturnSourceCardLayout(
        viewportWidthPx = transition.layoutWidthProvider().takeIf { it > 1f } ?: fallbackWidthPx,
        sourceBounds = transition.sourceBoundsProvider(),
        sourceCoverBounds = transition.sourceCoverBoundsProvider(),
        sourceLayout = transition.sourceLayout,
        sourceInfoBounds = transition.sourceInfoBoundsProvider(),
    ).takeIf { it.canRender } else null
    val request = remember(context, coverSource) {
        coverSource?.let { source ->
            ImageRequest.Builder(context).data(source.url).apply {
                if (source.decodeWidthPx > 0 && source.decodeHeightPx > 0) {
                    size(source.decodeWidthPx, source.decodeHeightPx)
                }
            }.placeholderMemoryCacheKey(source.cacheKey)
                .memoryCacheKey(source.cacheKey).diskCacheKey(source.cacheKey)
                .crossfade(false).build()
        }
    }
    val isReturning = { isReturningProvider() }
    val progress: () -> Float = {
        resolveLargeScreenVideoReturnMediaProgress(
            hasLandingGeometry = landing != null,
            phase = phaseProvider(),
            isReturning = isReturning(),
            isCommittedReturn = isCommittedReturn,
            depth = transition.progressProvider(),
            sourceLayout = transition.sourceLayout,
        )
    }
    val inverseScale = {
        val sourceHeight = transition.sourceBoundsProvider()?.height ?: 1f
        val viewportHeight = transition.layoutHeightProvider().takeIf { it > 1f } ?: fallbackHeightPx
        resolveMiuixVideoCardInverseScaleForDepth(
            sourceScaleX = landing?.sourceScale ?: 1f,
            sourceScaleY = (sourceHeight / viewportHeight.coerceAtLeast(1f))
                .coerceIn(0.01f, 1f),
            depth = transition.progressProvider(),
        )
    }
    val origin = { geometry.playerOrigin.value }
    val corner = AppShapes.mediaCoverCornerDp()
    val followsRelatedSidebarDrag = transition.enabled &&
        transition.sourceLayout == com.android.purebilibili.core.ui.transition.VideoCardSourceLayout.SIDE_BY_SIDE
    val playerViewportModifier = modifier
        .layout { measurable, constraints ->
            val baseWidth = constraints.maxWidth
            val windowWidth = transition.layoutWidthProvider().takeIf { it > 1f }?.roundToInt()
                ?: baseWidth
            val isReturning = isReturningProvider() ||
                phaseProvider() == VideoCardTransitionBackgroundPhase.RETURNING
            val revealProgress = if (followsRelatedSidebarDrag && isReturning) {
                transition.progressProvider()
            } else {
                0f
            }
            val expandedWidth = resolveLargeScreenSidePanelRevealWidthPx(
                baseWidthPx = baseWidth,
                windowWidthPx = windowWidth,
                progress = revealProgress,
            ).coerceAtLeast(baseWidth)
            val placeable = measurable.measure(
                constraints.copy(minWidth = expandedWidth, maxWidth = expandedWidth),
            )
            layout(width = baseWidth, height = placeable.height) {
                // Keep the split slot fixed so the recommendation pane follows its own
                // gesture position; let only the player viewport paint into the revealed gap.
                placeable.placeRelative(0, 0)
            }
        }
        .onGloballyPositioned(geometry::recordPlayer)
    BoxWithConstraints(modifier = playerViewportModifier) {
        val viewportWidth = maxWidth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .videoDetailReturnPlayerLayout(
                landingLayout = landing,
                handoffProgressProvider = progress,
                inverseScaleXProvider = { inverseScale().scaleX },
                inverseScaleYProvider = { inverseScale().scaleY },
                clipCornerDp = corner,
                slotOriginProvider = origin,
            ).graphicsLayer {
                alpha = if (transition.enabled && shouldHideLargeScreenVideoAfterReturn(
                    isCommittedReturn, phaseProvider(),
                )) 0f else 1f
            },
        ) { content(viewportWidth) }
        if (request != null && landing != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .videoDetailReturnMediaLayout(
                    landingLayout = landing,
                    handoffProgressProvider = progress,
                    inverseScaleXProvider = { inverseScale().scaleX },
                    inverseScaleYProvider = { inverseScale().scaleY },
                    slotOriginProvider = origin,
                )
                .zIndex(1f)
                .graphicsLayer {
                    alpha = if (shouldHideLargeScreenVideoAfterReturn(
                        isCommittedReturn, phaseProvider(),
                    )) 0f else progress()
                    clip = progress() > 0f
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
                },
            )
        }
    }
}
