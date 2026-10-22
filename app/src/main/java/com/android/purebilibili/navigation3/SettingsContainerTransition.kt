// SPDX-License-Identifier: GPL-3.0-only
package com.android.purebilibili.navigation3

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import com.android.purebilibili.core.ui.components.AppContainerNavigationSource
import com.android.purebilibili.core.ui.transition.resolvePredictiveBackMaxBlurRadiusPx
import com.android.purebilibili.navigation3.predictiveback.MiuixCoveredBlurRenderEffectCache
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.transition.NavMotion
import top.yukonga.miuix.kmp.nav.transition.NavSettlePhase
import top.yukonga.miuix.kmp.nav.transition.NavSettleSpec
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope
import kotlin.math.roundToInt

internal const val SETTINGS_CONTAINER_DURATION_MILLIS = 300
internal const val SETTINGS_CONTAINER_DRAG_SETTLE_MILLIS = 350

/** Two normalized cubics through a common midpoint, used only for drag completion/cancellation. */
internal val SettingsContainerDragEasing: Easing = run {
    val x = 0.198f
    val y = 0.541f
    val first = CubicBezierEasing(0.056f / x, 0.024f / y, 0.108f / x, 0.3085f / y)
    val second = CubicBezierEasing(
        (0.3655f - x) / (1f - x), (1f - y) / (1f - y),
        (0.5465f - x) / (1f - x), (0.989f - y) / (1f - y),
    )
    Easing { t ->
        if (t < x) y * first.transform(t / x)
        else y + (1f - y) * second.transform((t - x) / (1f - x))
    }
}

/** Both slots derive ownership from the same source-entry driver, including boundary frames. */
internal fun settingsContainerDestinationOwnsContent(
    sourceDepth: Float,
    geometryValid: Boolean,
): Boolean = geometryValid && sourceDepth > 0f

internal fun settingsContainerDragActive(gestureActive: Boolean, settlePhase: NavSettlePhase?): Boolean =
    gestureActive || settlePhase == NavSettlePhase.Commit || settlePhase == NavSettlePhase.Cancel

private fun NavTransitionScope.containerDragActive(): Boolean =
    settingsContainerDragActive(gesture != null, settle?.phase)

/** Only the covered page follows blur; the expanding container stays sharp. */
internal fun containerBackgroundBlurProgress(depth: Float, running: Boolean, enabled: Boolean): Float {
    if (!enabled || !running) return 0f
    val covered = depth.coerceIn(0f, 1f)
    return covered * covered
}

internal fun settingsContainerProgress(raw: Float, closing: Boolean): Float {
    val t = raw.coerceIn(0f, 1f)
    return if (closing) 1f - FastOutSlowInEasing.transform(1f - t)
        else FastOutSlowInEasing.transform(t)
}

internal fun settingsContainerContentAlpha(raw: Float, closing: Boolean): Float =
    ((settingsContainerProgress(raw, closing) - 0.1f) / 0.7f).coerceIn(0f, 1f)

/** Uniform content growth follows the measured container width and reaches identity at landing. */
internal fun settingsContainerPageScale(containerWidth: Float, pageWidth: Float): Float {
    if (!containerWidth.isFinite() || !pageWidth.isFinite() || pageWidth <= 0f || containerWidth <= 0f) return 1f
    return (containerWidth / pageWidth).coerceAtMost(1f)
}

/** A direction reversal mid-animation retains the forward curve, including its content interval. */
internal class SettingsContainerFrame {
    var progress = 0f
        private set
    var alpha = 0f
        private set
    private var direction: Boolean? = null
    private var previousRaw = 0f
    private var interrupted = false

    fun reset() { direction = null; interrupted = false }

    fun update(raw: Float, returning: Boolean) {
        val t = raw.coerceIn(0f, 1f)
        if (direction != null && direction != returning && previousRaw > 0f && previousRaw < 1f) {
            interrupted = true
        }
        val closing = returning && !interrupted
        progress = settingsContainerProgress(t, closing)
        alpha = settingsContainerContentAlpha(t, closing)
        direction = returning
        previousRaw = t
    }
}

internal fun settingsContainerTarget(
    before: List<BiliPaiNavKey>,
    after: List<BiliPaiNavKey>,
): BiliPaiNavKey? = after.lastOrNull()?.takeIf {
    after.size == before.size + 1 && after.dropLast(1) == before &&
        settingsContainerDestination(it)
}

internal fun settingsContainerDestination(target: BiliPaiNavKey): Boolean =
    com.android.purebilibili.feature.settings.isSettingsSubtreeNavKey(target)

internal fun settingsContainerLocalBounds(source: Rect, host: Rect): Rect? {
    if (source.width <= 1f || source.height <= 1f || host.width <= 1f || host.height <= 1f) return null
    val local = source.translate(-host.topLeft)
    return local.takeIf { it.left >= 0 && it.top >= 0 && it.right <= host.width && it.bottom <= host.height }
}

internal class SettingsContainerSession(
    val source: AppContainerNavigationSource,
    val sourceScope: NavTransitionScope,
    val parentKey: BiliPaiNavKey?,
    val hostBounds: Rect,
    closedBounds: Rect,
) {
    var closedBounds by mutableStateOf(closedBounds)
    var geometryValid by mutableStateOf(true)
    var returning = false

    fun supports(scope: NavTransitionScope): Boolean = geometryValid &&
        scope.layoutSize.width == hostBounds.width.roundToInt() &&
        scope.layoutSize.height == hostBounds.height.roundToInt() && scope.change !is NavChange.MultiPop
}

/** Keep the source of every enclosing container mounted until its boundary is released.
 * Retaining a GraphicsLayer alone does not retain child layers owned by disposed UI nodes.
 */
internal fun settingsContainerRetainedDepth(
    before: List<BiliPaiNavKey>,
    parentOf: (BiliPaiNavKey) -> BiliPaiNavKey?,
): Float {
    var index = before.lastIndex
    while (index > 0) {
        val parent = parentOf(before[index]) ?: break
        val parentIndex = before.indexOf(parent)
        if (parentIndex < 0 || parentIndex >= index) break
        index = parentIndex
    }
    return (before.size - index).coerceAtLeast(1).toFloat()
}

internal class SettingsContainerNavigationState {
    var hostBoundsInWindow: Rect = Rect.Zero
    var backgroundBlurEnabled by mutableStateOf(false)
    val transitions = mutableStateMapOf<BiliPaiNavKey, NavTransition>()
    private val sessions = mutableStateMapOf<BiliPaiNavKey, SettingsContainerSession>()
    private val rows = mutableMapOf<BiliPaiNavKey, MutableMap<Any, AppContainerNavigationSource>>()
    private val mounted = mutableSetOf<BiliPaiNavKey>()
    private class PendingCapture(
        val source: AppContainerNavigationSource,
        val scope: NavTransitionScope,
        val before: List<BiliPaiNavKey>,
        val fallback: NavTransition,
        val surface: Color,
        val acceptTarget: (BiliPaiNavKey) -> Boolean,
        val createdAt: Long = android.os.SystemClock.uptimeMillis(),
    )
    private var pending: PendingCapture? = null
    var pendingCaptureToken by mutableStateOf<Int?>(null)
        private set
    private var nextPendingCaptureToken = 0

    private fun clearPending() {
        pending = null
        pendingCaptureToken = null
    }

    fun expirePending(token: Int) {
        if (pendingCaptureToken == token) clearPending()
    }

    fun hasActiveBoundary(stack: List<BiliPaiNavKey>): Boolean =
        stack.lastOrNull()?.let(::ownsBoundary) == true ||
            sessions.keys.any { it !in stack && it in mounted && ownsBoundary(it) }

    /** Resolve immediately before publishing a new stack, including deferred settings navigation. */
    fun prepareStackChange(before: List<BiliPaiNavKey>, after: List<BiliPaiNavKey>) {
        val request = pending ?: return
        clearPending()
        if (before != request.before || android.os.SystemClock.uptimeMillis() - request.createdAt > 10_000L) return
        val target = settingsContainerTarget(before, after) ?: return
        if (!request.acceptTarget(target)) return
        install(request, target)
    }

    private fun install(request: PendingCapture, target: BiliPaiNavKey) {
        val source = request.source
        val local = source.takeIf { it.attached }
            ?.let { settingsContainerLocalBounds(it.boundsInWindow, hostBoundsInWindow) } ?: return
        release(target)
        source.heldSize = source.measuredSize
        val session = SettingsContainerSession(source, request.scope, request.before.lastOrNull(), hostBoundsInWindow, local)
        source.transitionOwnsContent = {
            settingsContainerDestinationOwnsContent(request.scope.relativeDepth, session.supports(request.scope))
        }
        sessions[target] = session
        val retainedDepth = settingsContainerRetainedDepth(request.before) { sessions[it]?.parentKey }
        transitions[target] = settingsContainerTransition(session, request.fallback, request.surface, retainedDepth) { backgroundBlurEnabled }
        source.retained = true
    }

    fun ownsBoundary(key: BiliPaiNavKey): Boolean = sessions[key]?.let { it.supports(it.sourceScope) } == true

    fun ownsPage(key: BiliPaiNavKey, scope: NavTransitionScope): Boolean =
        sessions[key]?.let { it.source.retained && it.supports(scope) && scope.relativeDepth <= 0f } == true

    fun isCoveredByContainer(key: BiliPaiNavKey): Boolean =
        sessions.values.any { it.parentKey == key && it.supports(it.sourceScope) }

    fun registryFor(key: BiliPaiNavKey): MutableMap<Any, AppContainerNavigationSource> =
        rows.getOrPut(key) { mutableMapOf() }
    fun sourceFor(key: BiliPaiNavKey): AppContainerNavigationSource? = sessions[key]?.source
    fun mount(key: BiliPaiNavKey) { mounted.add(key) }
    fun unmount(key: BiliPaiNavKey) { mounted.remove(key) }

    fun releaseUnretained(stack: List<BiliPaiNavKey>) {
        sessions.keys.filter { it !in stack && it !in mounted }.forEach(::release)
        rows.keys.filter { it !in stack && it !in mounted }.forEach { rows.remove(it) }
    }

    /** Called after source layout has had a frame to restore its insets and scroll position. */
    fun remeasureReturns(stack: List<BiliPaiNavKey>): Boolean {
        sessions.filterKeys { it !in stack }.values.forEach { session ->
            if (!session.returning) {
                val measured = session.source.takeIf { it.attached }
                    ?.let { settingsContainerLocalBounds(it.boundsInWindow, hostBoundsInWindow) }
                // A temporarily detached source retains its last valid geometry throughout exit.
                // Switching renderers after pop would change the path in the middle of the settle.
                if (measured != null) {
                    session.returning = true
                    session.source.heldSize = session.source.measuredSize
                    session.closedBounds = measured
                }
            }
        }
        return sessions.any { (key, session) -> key !in stack && !session.returning }
    }

    fun releaseDisabledModes(enabled: Boolean) {
        if (!enabled) releaseAll()
    }

    fun releaseAll() {
        clearPending()
        sessions.keys.toList().forEach(::release)
    }

    fun capture(
        source: AppContainerNavigationSource,
        sourceScope: NavTransitionScope,
        stack: List<BiliPaiNavKey>,
        fallback: NavTransition,
        surface: Color,
        acceptTarget: (BiliPaiNavKey) -> Boolean = { true },
        navigate: () -> Unit,
    ) {
        // A newer click supersedes any deferred navigation from the previous source.
        clearPending()
        val before = stack.toList()
        val bounds = hostBoundsInWindow
        val local = settingsContainerLocalBounds(source.boundsInWindow, bounds)
        if (local == null || !source.closedDrawingReady || source.measuredSize.width <= 0 || source.measuredSize.height <= 0) {
            navigate()
            return
        }
        val request = PendingCapture(source, sourceScope, before, fallback, surface, acceptTarget)
        pending = request
        pendingCaptureToken = ++nextPendingCaptureToken
        try {
            navigate()
            // Standalone hosts can complete synchronous navigation without the stack bridge.
            // Otherwise keep the request until the actual stack write or its timeout.
            if (pending === request) {
                if (before != stack.toList()) prepareStackChange(before, stack.toList())
            }
        } catch (failure: Throwable) {
            if (pending === request) clearPending()
            throw failure
        }
    }

    fun release(key: BiliPaiNavKey) {
        sessions.remove(key)?.source?.let { source ->
            source.releaseRetainedContent()
        }
        transitions.remove(key)
        rows.values.forEach { registry ->
            registry.entries.removeAll { !it.value.retained && !it.value.attached }
        }
    }
}

private fun settingsContainerTransition(
    session: SettingsContainerSession,
    fallback: NavTransition,
    surface: Color,
    retainedDepth: Float,
    blurEnabled: () -> Boolean,
): NavTransition = object : NavTransition {
    override val opaqueDepth: Float = retainedDepth
    private val source = session.source
    private val frame = SettingsContainerFrame()
    override val motion = NavMotion(
        programmatic = NavSettleSpec.Tween(SETTINGS_CONTAINER_DURATION_MILLIS, LinearEasing),
        commit = NavSettleSpec.Tween(SETTINGS_CONTAINER_DRAG_SETTLE_MILLIS, SettingsContainerDragEasing),
        cancel = NavSettleSpec.Tween(SETTINGS_CONTAINER_DRAG_SETTLE_MILLIS, SettingsContainerDragEasing),
    )
    override fun scrimFraction(scope: NavTransitionScope): Float = 0f

    override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier {
        if (!session.supports(scope)) return with(fallback) { this@transformEntry.transformEntry(scope) }
        val blurCache = MiuixCoveredBlurRenderEffectCache()
        return graphicsLayer {
            val blurProgress = containerBackgroundBlurProgress(scope.relativeDepth, scope.isRunning, blurEnabled())
            val radius = resolvePredictiveBackMaxBlurRadiusPx(surface.luminance() > 0.5f) * blurProgress
            renderEffect = if (session.geometryValid) blurCache.resolve(radius) else null
        }.drawWithContent {
            val depth = scope.relativeDepth
            if (!session.geometryValid || !source.retained || depth > 0f) {
                drawContent()
                return@drawWithContent
            }
            if (depth == 0f && !scope.isRunning) {
                frame.reset()
                drawRect(surface)
                drawContent()
                source.pageLayer?.let { layer ->
                    layer.alpha = 1f
                    drawLayer(layer)
                }
                return@drawWithContent
            }
            // Keep the same container path through the gesture and its commit/cancel settle.
            val returning = scope.containerDragActive() || scope.change is NavChange.Pop
            frame.update(1f + depth, returning)
            val alpha = frame.alpha
            val closed = session.closedBounds
            val rect = lerp(closed, Rect(0f, 0f, size.width, size.height), frame.progress)
            // The page records in an isolated layer and suppresses its original slot
            // for the entire boundary lifetime, rather than toggling a per-draw gate.
            drawContent()
            val path = settingsContainerShapePath(source, rect, frame.progress, scope)
            val closedColor = source.closedColor.takeIf { it != Color.Unspecified } ?: surface
            // Surface and content have separate tracks on every material path.
            val color = settingsContainerSurfaceColor(
                closedColor, surface,
                frame.progress,
                inheritedSurface = source.closedColorInherited,
            )
            val paintSlots: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit = {
                drawRect(color, rect.topLeft, rect.size)
                source.closedLayer?.takeIf { source.destinationOwnsContent }?.let { layer ->
                    translate(rect.left, rect.top) {
                        scale(rect.width / closed.width, pivot = Offset.Zero) { drawLayer(layer) }
                    }
                }
                source.pageLayer?.takeIf { alpha > 0f }?.let { layer ->
                    // Keep the page's final layout, but grow its drawing with the container.
                    // At full width this is exactly identity, so the resting page and its
                    // existing landing feedback take over without another size adjustment.
                    val pageScale = settingsContainerPageScale(rect.width, size.width)
                    translate(rect.left, rect.top) {
                        scale(pageScale, pivot = Offset.Zero) {
                            layer.alpha = alpha
                            drawLayer(layer)
                        }
                    }
                }
            }
            if (path == null) clipRect(rect.left, rect.top, rect.right, rect.bottom, block = paintSlots)
            else clipPath(path, block = paintSlots)
        }
    }
}

/** Surface follows geometry; text can fade early without switching the container's background. */
internal fun settingsContainerSurfaceColor(
    closedColor: Color,
    pageColor: Color,
    progress: Float,
    inheritedSurface: Boolean = false,
): Color {
    // The parent still paints its card while the row is exported. Repainting a
    // translucent wallpaper card here would apply its tint twice on return.
    val closedFill = if (inheritedSurface && closedColor.alpha < 1f) Color.Transparent else closedColor
    return androidx.compose.ui.graphics.lerp(closedFill, pageColor, progress.coerceIn(0f, 1f))
}

internal fun containerCornerRadius(sourceRadius: Float, progress: Float): Float =
    sourceRadius.coerceAtLeast(0f) * (1f - progress.coerceIn(0f, 1f))

private fun settingsContainerShapePath(
    source: AppContainerNavigationSource,
    rect: Rect,
    progress: Float,
    scope: NavTransitionScope,
): Path? {
    val shape = source.closedShape ?: return null
    val sourceSize = Size(source.heldSize.width.toFloat(), source.heldSize.height.toFloat())
    val outline = shape.createOutline(sourceSize, scope.layoutDirection, scope.density) as? Outline.Rounded ?: return null
    val round = outline.roundRect
    fun CornerRadius.reduced() = CornerRadius(containerCornerRadius(x, progress), containerCornerRadius(y, progress))
    return Path().apply {
        addRoundRect(RoundRect(
            rect.left, rect.top, rect.right, rect.bottom,
            round.topLeftCornerRadius.reduced(), round.topRightCornerRadius.reduced(),
            round.bottomRightCornerRadius.reduced(), round.bottomLeftCornerRadius.reduced(),
        ))
    }
}
