package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize

/** The entry stays composed in its source slot; the transition retains only its drawing layer. */
class AppContainerNavigationSource internal constructor() {
    var retained by mutableStateOf(false)
    var transitionOwnsContent: (() -> Boolean)? by mutableStateOf(null)
    val destinationOwnsContent by derivedStateOf {
        retained && transitionOwnsContent?.invoke() == true
    }
    var boundsInWindow: Rect = Rect.Zero
    var measuredSize: IntSize = IntSize.Zero
    var heldSize: IntSize = IntSize.Zero
    var attached = false
        private set
    var closedColor: Color = Color.Unspecified
    var closedColorInherited = false
    var closedShape: RoundedCornerShape? = null
    var closedLayer: GraphicsLayer? = null
        private set
    var pageLayer: GraphicsLayer? = null
    var click: () -> Unit = {}
    private var graphicsContext: GraphicsContext? = null

    internal fun attach(context: GraphicsContext) {
        if (graphicsContext !== context) releaseDrawingLayer()
        graphicsContext = context
        if (closedLayer == null) closedLayer = context.createGraphicsLayer()
        attached = true
    }

    internal fun detach() {
        attached = false
        if (!retained) releaseDrawingLayer()
    }

    /** A culled lazy item may keep its last recorded drawing until its boundary is removed. */
    fun releaseRetainedContent() {
        retained = false
        transitionOwnsContent = null
        if (!attached) releaseDrawingLayer()
    }

    private fun releaseDrawingLayer() {
        closedLayer?.let { graphicsContext?.releaseGraphicsLayer(it) }
        closedLayer = null
        graphicsContext = null
    }
}

val LocalAppContainerNavigationCapture =
    compositionLocalOf<((AppContainerNavigationSource, () -> Unit) -> Unit)?> { null }

/** Entry-local identity retains the drawing when deeper navigation culls its source page. */
val LocalAppContainerNavigationSources =
    compositionLocalOf<MutableMap<Any, AppContainerNavigationSource>?> { null }

/** The surface behind a transparent source row, supplied by the container that paints it. */
internal val LocalAppContainerNavigationSurfaceColor = compositionLocalOf { Color.Unspecified }

internal fun resolveAppContainerNavigationSourceColor(explicit: Color, inherited: Color): Color =
    if (explicit != Color.Unspecified) explicit else inherited

@Composable
fun AppContainerNavigationSource(
    sourceId: Any,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    closedColor: Color = Color.Unspecified,
    closedShape: RoundedCornerShape? = null,
    content: @Composable (onClick: (() -> Unit)?) -> Unit,
) {
    val capture = LocalAppContainerNavigationCapture.current
    if (capture == null || onClick == null) {
        if (modifier == Modifier) content(onClick)
        else Box(modifier = modifier, propagateMinConstraints = true) { content(onClick) }
        return
    }
    val registry = LocalAppContainerNavigationSources.current
    val identity = sourceId to currentCompositeKeyHash
    val source = remember(registry, identity) {
        registry?.getOrPut(identity) { AppContainerNavigationSource() }
            ?: AppContainerNavigationSource()
    }
    val latestClick by rememberUpdatedState(onClick)
    val latestCapture by rememberUpdatedState(capture)
    val graphicsContext = LocalGraphicsContext.current
    val inheritedColor = LocalAppContainerNavigationSurfaceColor.current
    val sourceColor = resolveAppContainerNavigationSourceColor(
        explicit = closedColor,
        inherited = inheritedColor,
    )
    SideEffect {
        source.closedColor = sourceColor
        source.closedColorInherited = closedColor == Color.Unspecified && inheritedColor != Color.Unspecified
        source.closedShape = closedShape
        source.click = {
            latestCapture?.invoke(source) { latestClick?.invoke() } ?: latestClick?.invoke()
        }
    }
    DisposableEffect(source, registry, identity, graphicsContext) {
        source.attach(graphicsContext)
        onDispose {
            source.detach()
            registry?.let { if (!source.retained && it[identity] === source) it.remove(identity) }
        }
    }
    Box(
        propagateMinConstraints = true,
        modifier = modifier.onGloballyPositioned { coordinates ->
            val visible = coordinates.boundsInWindow()
            source.boundsInWindow = if (
                kotlin.math.abs(visible.width - coordinates.size.width) <= 1f &&
                kotlin.math.abs(visible.height - coordinates.size.height) <= 1f
            ) visible else Rect.Zero
            source.measuredSize = coordinates.size
        }.graphicsLayer {
            // Visibility belongs to a layer property, so cached row drawing cannot
            // retain an empty display list after the return boundary disappears.
            alpha = if (source.destinationOwnsContent) 0f else 1f
        }.drawWithContent {
            val layer = source.closedLayer
            if (layer == null) drawContent()
            else {
                layer.record { this@drawWithContent.drawContent() }
                drawLayer(layer)
            }
        },
    ) {
        CompositionLocalProvider(
            LocalAppContainerNavigationCapture provides null,
        ) { content { source.click() } }
    }
}
