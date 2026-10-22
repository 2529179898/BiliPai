package com.android.purebilibili.core.ui.animation

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

val LocalLiquidIndicatorWobbleEnabled = compositionLocalOf { true }

/** Tap selections deform along their travel; manual dragging keeps its original shape until release. */
internal class LiquidIndicatorWobble(initialPosition: Float) {
    private val spring = FloatSpringSpec(dampingRatio = 0.4f, stiffness = 100f, visibilityThreshold = 0.001f)
    private var position = initialPosition
    private var peakSpeed = 0f
    private var distance = 0f
    private var armed = false
    private var rate = 0f
    private var filteredSpeed = 0f
    private var filteredBraking = 0f
    private var releaseMotion = false
    private var releaseElapsedNanos = 0L
    var velocityWeight = 1f
        private set
    private var selectionMotion = false
    var stretch = 0f
        private set
    val isSelectionMotion: Boolean get() = selectionMotion

    /** A changed selection arms one recoil even when its position jumps between frames. */
    fun onSelectionChanged(continuous: Boolean = false, preserveRelease: Boolean = false) {
        // 拖动提交的选中回执只确认落点，不重新驱动已开始的松手回弹。
        // 新的点按仍可接管当前形变。
        if (preserveRelease && releaseMotion) return
        if (continuous) {
            // Retargeting keeps the shape and its velocity. Even a selection that
            // lands between samples receives one small, continuous response.
            if (!selectionMotion) {
                rate = 1.8f
                releaseElapsedNanos = 0L
            }
            selectionMotion = true
            armed = false
            distance = 0f
            peakSpeed = 0f
            releaseMotion = false
            return
        }
        selectionMotion = false
        armed = true
        peakSpeed = max(peakSpeed, 1.5f)
        stretch = 0f
        rate = 0f
    }

    fun onInteractionStarted() {
        velocityWeight = 1f
        releaseElapsedNanos = 0L
        selectionMotion = false
        releaseMotion = false
        stretch = 0f
        rate = 0f
        filteredSpeed = 0f
        filteredBraking = 0f
        distance = 0f
        peakSpeed = 0f
        armed = false
    }

    /** Release joins the position's settling phase, without waiting for press/velocity springs. */
    fun onInteractionReleased(nextPosition: Float = position) {
        // A short drag can finish before the frame worker samples its last delta.
        val pendingDistance = if (nextPosition.isFinite()) abs(nextPosition - position) else 0f
        if (!armed && distance + pendingDistance < 0.02f) return
        selectionMotion = true
        releaseMotion = true
        velocityWeight = 1f
        releaseElapsedNanos = 0L
        armed = false
        distance = 0f
        rate = recoilVelocity(max(peakSpeed, pendingDistance * 60f))
        filteredSpeed = 0f
        filteredBraking = 0f
        peakSpeed = 0f
    }

    fun advance(
        nextPosition: Float,
        elapsedNanos: Long,
        canSettle: Boolean = true,
        manualInteraction: Boolean = false,
        holdSelectionMotion: Boolean = false,
    ): Boolean {
        if (!nextPosition.isFinite()) {
            reset(position)
            return false
        }
        if (elapsedNanos <= 0L) return false
        // A short busy frame during page selection must not clear the visible shape.
        // Only a suspended window discards motion; cap spring advancement after a stall.
        if (elapsedNanos > 500_000_000L) {
            reset(nextPosition)
            return false
        }
        val springNanos = elapsedNanos.coerceAtMost(50_000_000L)
        val seconds = elapsedNanos / 1_000_000_000f
        val signedVelocity = ((nextPosition - position) / seconds).coerceIn(-30f, 30f)
        val delta = abs(nextPosition - position)
        position = nextPosition
        val moving = delta > 0.0001f
        if (manualInteraction) {
            selectionMotion = false
            releaseMotion = false
        }
        if (selectionMotion) {
            if (velocityWeight > 0f) {
                // Keep the release frame identical to the held shape, then hand
                // velocity deformation over without delaying the original recoil.
                releaseElapsedNanos += springNanos
                val progress = (releaseElapsedNanos / 120_000_000f).coerceIn(0f, 1f)
                velocityWeight = 1f - progress * progress * (3f - 2f * progress)
            }
            fun saturate(value: Float) = 1f - 2f / (exp(2f * value) + 1f)
            // Smooth speed before differentiating it. Coalesced pager updates and
            // direction reversals must not inject a fresh collision on each frame.
            val previousSpeed = filteredSpeed
            val blend = 1f - exp(-seconds / 0.04f)
            filteredSpeed += (abs(signedVelocity) - filteredSpeed) * blend
            val braking = max(0f, (previousSpeed - filteredSpeed) / seconds)
            filteredBraking += (braking - filteredBraking) * blend
            val target = if (releaseMotion) 0f else
                0.18f * saturate(filteredSpeed / 3f) - 0.14f * saturate(filteredBraking / 25f)
            val initial = stretch
            stretch = spring.getValueFromNanos(springNanos, initial, target, rate)
            rate = spring.getVelocityFromNanos(springNanos, initial, target, rate)
            val active = moving || holdSelectionMotion || abs(stretch) > 0.001f || abs(rate) > 0.01f ||
                (!releaseMotion && (filteredSpeed > 0.01f || filteredBraking > 0.05f))
            if (!active) {
                selectionMotion = false
                releaseMotion = false
                stretch = 0f
                rate = 0f
                filteredSpeed = 0f
                filteredBraking = 0f
            }
            // No separate release impulse: the same shape spring returns naturally to zero.
            return active
        }
        if (moving) {
            peakSpeed = max(peakSpeed, (delta * 1_000_000_000f / elapsedNanos).coerceAtMost(30f))
            distance += delta
            armed = armed || distance >= 0.02f
        }
        if (moving || !canSettle) {
            // Original drag, velocity stretch and press transforms own this entire phase.
            // New input cancels an already playing recoil, without restarting it on release.
            if (!moving && rate != 0f) {
                armed = false
                distance = 0f
                peakSpeed = 0f
            }
            stretch = 0f
            rate = 0f
            return moving
        }
        if (armed) {
            armed = false
            distance = 0f
            // Begin at zero displacement so the extra animation has no entry jump.
            rate = recoilVelocity(peakSpeed)
            peakSpeed = 0f
        }
        val initial = stretch
        stretch = spring.getValueFromNanos(springNanos, initial, 0f, rate)
        rate = spring.getVelocityFromNanos(springNanos, initial, 0f, rate)
        val active = abs(stretch) > 0.001f || abs(rate) > 0.01f
        if (!active) {
            stretch = 0f
            rate = 0f
        }
        return active
    }

    private fun recoilVelocity(speed: Float): Float =
        -(2.3f + 0.6f * (1f - 2f / (exp(2f * speed / 3f) + 1f)))

    fun reset(nextPosition: Float) {
        velocityWeight = 1f
        releaseElapsedNanos = 0L
        position = nextPosition
        peakSpeed = 0f
        distance = 0f
        armed = false
        rate = 0f
        filteredSpeed = 0f
        filteredBraking = 0f
        releaseMotion = false
        selectionMotion = false
        stretch = 0f
    }
}

class LiquidIndicatorWobbleState internal constructor(val enabled: Boolean) : State<Float> {
    private val stretch = mutableFloatStateOf(0f)
    override val value: Float get() = stretch.floatValue
    var isSelectionMotion by mutableStateOf(false)
        internal set
    var velocityWeight by mutableFloatStateOf(1f)
        internal set
    internal var floatValue: Float
        get() = stretch.floatValue
        set(value) { stretch.floatValue = value }
}

private data class LiquidWobbleInput(
    val position: Float,
    val canSettle: Boolean,
    val selectionKey: Any?,
    val manual: Boolean,
    val selectionMotion: Boolean?,
)

/** One frame worker per indicator; retargets preserve the current shape and its velocity. */
@Composable
fun rememberLiquidIndicatorWobble(
    enabled: Boolean,
    geometryKey: Any,
    canSettle: () -> Boolean = { true },
    selectionKey: () -> Any? = { null },
    interactionActive: () -> Boolean = { false },
    selectionMotionActive: () -> Boolean? = { null },
    position: () -> Float,
): LiquidIndicatorWobbleState {
    val latestPosition = rememberUpdatedState(position)
    val latestCanSettle = rememberUpdatedState(canSettle)
    val latestSelectionKey = rememberUpdatedState(selectionKey)
    val latestInteractionActive = rememberUpdatedState(interactionActive)
    val latestSelectionMotion = rememberUpdatedState(selectionMotionActive)
    val motionEnabled = enabled && LocalLiquidIndicatorWobbleEnabled.current &&
        com.android.purebilibili.core.ui.LocalComponentMotionEnabled.current &&
        !com.android.purebilibili.core.ui.LocalSystemReduceMotion.current
    val result = remember(motionEnabled, geometryKey) { LiquidIndicatorWobbleState(motionEnabled) }
    LaunchedEffect(result) {
        if (!motionEnabled) return@LaunchedEffect
        val wobble = LiquidIndicatorWobble(latestPosition.value())
        fun input() = LiquidWobbleInput(
            latestPosition.value(), latestCanSettle.value(), latestSelectionKey.value(),
            latestInteractionActive.value(), latestSelectionMotion.value(),
        )
        coroutineScope {
            var worker: Job? = null
            // A layout may mount this state during an already active selection.
            // Preserve the explicit start without animating an ordinary idle mount.
            var observed = input().copy(selectionMotion = false, manual = false)
            snapshotFlow { input() }.collect { next ->
                if (next.position.isFinite() && next != observed) {
                    val selectionChanged = next.selectionKey != observed.selectionKey
                    val pressed = !observed.manual && next.manual
                    val released = observed.manual && !next.manual
                    val explicitTapStarted = next.selectionMotion == true && observed.selectionMotion != true
                    val continuous = !next.manual && !released && (wobble.isSelectionMotion || next.selectionMotion == true ||
                        (selectionChanged && !observed.manual))
                    if (pressed) {
                        wobble.onInteractionStarted()
                    } else if (released) {
                        wobble.onInteractionReleased(next.position)
                    } else if (!next.manual && (selectionChanged || explicitTapStarted)) {
                        wobble.onSelectionChanged(continuous, preserveRelease = !explicitTapStarted)
                    }
                    result.isSelectionMotion = wobble.isSelectionMotion
                    result.velocityWeight = wobble.velocityWeight
                    // Only manual input suppresses the new shape while moving.
                    if (next.manual) result.floatValue = 0f
                    observed = next
                    if (worker?.isActive != true) worker = launch {
                        // Process the first delivered frame too; waiting for a second
                        // frame added 16–33ms and could miss a short tap's travel entirely.
                        var previous: Long? = null
                        do {
                            val now = withFrameNanos { it }
                            val durationScale = currentCoroutineContext()[MotionDurationScale]?.scaleFactor ?: 1f
                            val current = input()
                            val active = if (durationScale > 0f) {
                                wobble.advance(
                                    current.position, ((previous?.let { now - it } ?: 16_666_667L) / durationScale).toLong(),
                                    current.canSettle, current.manual, current.selectionMotion == true,
                                )
                            } else {
                                wobble.reset(current.position)
                                false
                            }
                            result.isSelectionMotion = wobble.isSelectionMotion
                            result.velocityWeight = wobble.velocityWeight
                            result.floatValue = if (current.manual) 0f else wobble.stretch.coerceIn(-0.18f, 0.18f)
                            previous = now
                        } while (active)
                    }
                }
            }
        }
    }
    return result
}

fun liquidWobbleScaleX(base: Float, stretch: Float): Float = base * (1f + stretch)
fun liquidWobbleScaleY(base: Float, stretch: Float): Float = base * (1f - stretch * 1.4f)

/** Material stays active through the visible shape's return, including spring undershoot. */
fun liquidIndicatorShapeProgress(scaleX: Float, scaleY: Float, bloomScale: Float): Float =
    (max(abs(scaleX - 1f), abs(scaleY - 1f)) / (bloomScale - 1f).coerceAtLeast(0.18f))
        .coerceIn(0f, 1f)

/** Rendering checks interaction state directly so a previous recoil never leaks into a new drag. */
fun resolveLiquidIndicatorRecoilStretch(stretch: Float, canSettle: Boolean): Float =
    if (canSettle) stretch else 0f

/** Logical-axis shift: compress against an endpoint, then peel away as the shape rebounds. */
fun liquidWobbleEdgeTranslationPx(
    stretch: Float,
    position: Float,
    lastIndex: Int,
    extentPx: Float,
    edgeInsetPx: Float,
    baseScale: Float = 1f,
): Float {
    if (lastIndex <= 0 || !position.isFinite() || !stretch.isFinite() || extentPx <= 0f || stretch == 0f) return 0f
    val edgeDistance: Float
    val edge = when {
        position < 0.35f -> { edgeDistance = abs(position); -1f }
        position > lastIndex - 0.35f -> { edgeDistance = abs(position - lastIndex); 1f }
        else -> return 0f
    }
    val proximity = (1f - edgeDistance / 0.35f).coerceIn(0f, 1f)
    val edgeWeight = proximity * proximity * (3f - 2f * proximity)
    val inset = edgeInsetPx.coerceAtLeast(0f)
    // Close the measured gap early enough for both tap braking and release recoil.
    // Compensate the width lost to squash and any remaining press magnification.
    val contact = if (stretch < 0f) (-stretch / 0.02f).coerceAtMost(1f)
        else (stretch / 0.04f).coerceAtMost(1f)
    // Bloom compensation is much larger than the inset. Ramping it with the
    // 0.02 squash threshold made every zero crossing shove a magnified endpoint
    // sideways. Ease that correction over the shape's full collision range;
    // keep squash/rebound amplitude and full-contact geometry unchanged.
    val magnificationProgress = (abs(stretch) / 0.1f).coerceIn(0f, 1f)
    val magnificationContact = magnificationProgress * magnificationProgress *
        (3f - 2f * magnificationProgress)
    val magnificationCorrection = extentPx * (1f - baseScale) * (1f + stretch) / 2f * magnificationContact
    val shift = -stretch * extentPx / 2f + magnificationCorrection +
        (if (stretch < 0f) contact else -contact) * inset
    return edge * shift * edgeWeight
}
