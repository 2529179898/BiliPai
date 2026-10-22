package com.android.purebilibili.feature.home.components.liquid

import com.android.purebilibili.core.ui.AppSpacingTokens

import com.android.purebilibili.core.ui.OpticalContrastPalette

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Immutable
data class InnerShadow(
    val radius: Dp = AppSpacingTokens.ExtraLarge,
    val offset: DpOffset = DpOffset(AppSpacingTokens.None, radius),
    val color: Color = OpticalContrastPalette.Shadow.copy(alpha = 0.15f),
    val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode,
) {
    companion object {
        @Stable
        val Default: InnerShadow = InnerShadow()
    }
}

fun Modifier.innerShadow(
    shape: Shape,
    shadow: () -> InnerShadow?,
): Modifier = this then InnerShadowElement(shape, shadow)

private class InnerShadowElement(
    val shape: Shape,
    val shadow: () -> InnerShadow?,
) : ModifierNodeElement<InnerShadowNode>() {

    override fun create(): InnerShadowNode = InnerShadowNode(shape, shadow)

    override fun update(node: InnerShadowNode) {
        node.shape = shape
        node.shadow = shadow
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "innerShadow"
        properties["shape"] = shape
        properties["shadow"] = shadow
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InnerShadowElement) return false
        if (shape != other.shape) return false
        if (shadow != other.shadow) return false
        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + shadow.hashCode()
        return result
    }
}

private class InnerShadowNode(
    var shape: Shape,
    var shadow: () -> InnerShadow?,
) : Modifier.Node(),
    DrawModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    private var shadowLayer: GraphicsLayer? = null
    private val paint = Paint()
    private val clipPath = Path()
    private val blurCache = InnerShadowBlurEffectCache()
    private var cachedOutline: Outline? = null
    private var cachedSize = Size.Unspecified
    private var cachedShape: Shape? = null
    private var cachedDirection: LayoutDirection? = null
    private var cachedDensity = Float.NaN
    private var cachedFontScale = Float.NaN
    private var recordedOutline: Outline? = null
    private var recordedColor = Color.Unspecified
    private var recordedOffsetX = Float.NaN
    private var recordedOffsetY = Float.NaN

    override fun ContentDrawScope.draw() {
        drawContent()

        val shadow = shadow() ?: return
        // An invisible tail must not rebuild filters, paths or display lists.
        if (shadow.alpha <= 0f || shadow.color.alpha <= 0f) return
        val layer = shadowLayer ?: return
        val offsetX = shadow.offset.x.toPx()
        val offsetY = shadow.offset.y.toPx()

        val geometryChanged = cachedOutline == null || cachedSize != size || cachedShape != shape ||
            cachedDirection != layoutDirection || cachedDensity != density || cachedFontScale != fontScale
        if (geometryChanged) {
            cachedOutline = shape.createOutline(size, layoutDirection, this)
            cachedSize = size
            cachedShape = shape
            cachedDirection = layoutDirection
            cachedDensity = density
            cachedFontScale = fontScale
            clipPath.reset()
            when (val outline = requireNotNull(cachedOutline)) {
                is Outline.Rectangle -> clipPath.addRect(outline.rect)
                is Outline.Rounded -> clipPath.addRoundRect(outline.roundRect)
                is Outline.Generic -> clipPath.addPath(outline.path)
            }
        }
        val outline = requireNotNull(cachedOutline)
        layer.alpha = shadow.alpha
        layer.blendMode = shadow.blendMode
        val effect = blurCache.resolve(shadow.radius.toPx())
        if (layer.renderEffect !== effect) layer.renderEffect = effect

        // Radius and opacity are layer properties. Reuse the mask while geometry,
        // offset and color stay unchanged, including the whole recoil animation.
        if (geometryChanged || recordedOutline !== outline || recordedColor != shadow.color ||
            recordedOffsetX != offsetX || recordedOffsetY != offsetY
        ) {
            paint.color = shadow.color
            layer.record {
                drawContext.canvas.let { canvas ->
                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawOutline(outline, paint)
                    canvas.translate(offsetX, offsetY)
                    canvas.drawOutline(outline, ShadowMaskPaint)
                    canvas.translate(-offsetX, -offsetY)
                    canvas.restore()
                }
            }
            recordedOutline = outline
            recordedColor = shadow.color
            recordedOffsetX = offsetX
            recordedOffsetY = offsetY
        }

        drawContext.canvas.let { canvas ->
            canvas.save()
            canvas.clipPath(clipPath)
            drawLayer(layer)
            canvas.restore()
        }
    }

    override fun onAttach() {
        shadowLayer = requireGraphicsContext().createGraphicsLayer().apply {
            compositingStrategy = CompositingStrategy.Offscreen
        }
    }

    override fun onDetach() {
        shadowLayer?.let { layer ->
            requireGraphicsContext().releaseGraphicsLayer(layer)
            shadowLayer = null
            recordedOutline = null
            blurCache.clear()
        }
    }
}

private val ShadowMaskPaint: Paint = Paint().apply {
    blendMode = BlendMode.Clear
}

/** Quarter-pixel radii bound filter churn to subpixel steps; recoil reuses earlier radii. */
internal class InnerShadowBlurEffectCache(private val capacity: Int = 128) {
    init { require(capacity > 0) }
    private val effects = LinkedHashMap<Int, BlurEffect>(16, 0.75f, true)

    fun resolve(radiusPx: Float): BlurEffect? {
        if (!radiusPx.isFinite() || radiusPx <= 0f) return null
        val key = (radiusPx * 4f).roundToInt()
        if (key <= 0) return null
        return effects.getOrPut(key) {
            BlurEffect(key / 4f, key / 4f, TileMode.Decal)
        }.also {
            if (effects.size > capacity) effects.remove(effects.keys.first())
        }
    }

    fun clear() { effects.clear() }
}
