package com.android.purebilibili.core.ui.transition

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.scaleToBounds
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.motion.rememberSystemReduceMotion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

internal const val SHARED_CARD_CONTAINER_DURATION_MS = 360

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun sharedCardContainerBounds(
    modifier: Modifier,
    key: String,
    sharedScope: SharedTransitionScope? = LocalSharedTransitionScope.current,
    visibilityScope: AnimatedVisibilityScope? = LocalAnimatedVisibilityScope.current,
): Modifier {
    if (sharedScope == null || visibilityScope == null) return modifier
    return with(sharedScope) {
        modifier.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
            enter = fadeIn(tween(SHARED_CARD_CONTAINER_DURATION_MS, easing = LinearEasing)),
            exit = fadeOut(tween(SHARED_CARD_CONTAINER_DURATION_MS, easing = LinearEasing)),
            boundsTransform = { _, _ -> tween(SHARED_CARD_CONTAINER_DURATION_MS, easing = LinearEasing) },
            // Cards and detail pages have very different aspect ratios. Stretching the whole
            // subtree to the animated bounds distorts avatars, posters and glyphs on both sides.
            // Keep each endpoint's layout and scale it uniformly while the container morphs.
            resizeMode = scaleToBounds(ContentScale.Fit),
            clipInOverlayDuringTransition = OverlayClip(AppShapes.container(ContainerLevel.Card)),
        )
    }
}

/** The one container host used by Bangumi and non-video search results. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun <T : Any> SharedCardContainerHost(
    saver: Saver<T?, out Any>,
    keyOf: (T) -> String,
    content: @Composable ((T) -> Unit, Boolean) -> Unit,
    destination: @Composable (T, () -> Unit, (T) -> Unit, Boolean, Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val savedHub = rememberSaveableStateHolder()
    val reduceMotion = rememberSystemReduceMotion()
    val duration = if (reduceMotion) 0 else SHARED_CARD_CONTAINER_DURATION_MS
    var opened by rememberSaveable(stateSaver = saver) {
        mutableStateOf<T?>(null)
    }
    val transitionState = remember { SeekableTransitionState(opened) }
    var detailContentReady by remember { mutableStateOf(opened != null) }
    LaunchedEffect(transitionState.currentState, transitionState.targetState) {
        if (transitionState.currentState != null &&
            transitionState.currentState == transitionState.targetState
        ) {
            detailContentReady = true
        }
    }
    val detailVisible = opened != null || transitionState.currentState != null || transitionState.targetState != null
    val closeDetail: () -> Unit = {
        scope.launch {
            transitionState.animateTo(null, tween(duration, easing = LinearEasing))
            opened = null
        }
    }
    val openDetail: (T) -> Unit = { target ->
        if (!detailVisible) {
            detailContentReady = false
            opened = target
            scope.launch {
                transitionState.animateTo(target, tween(duration, easing = LinearEasing))
                detailContentReady = true
            }
        }
    }
    val replaceDetail: (T) -> Unit = { target ->
        opened = target
        scope.launch { transitionState.snapTo(target) }
    }
    PredictiveBackHandler(enabled = detailVisible) { progress ->
        var fraction = 0f
        try {
            progress.collect { event ->
                fraction = event.progress.coerceIn(0f, 1f)
                transitionState.seekTo(if (reduceMotion) 0f else fraction, null)
            }
            // Continue the existing seek direction and let Compose use its remaining time.
            transitionState.animateTo(null)
            opened = null
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                opened?.let { detail ->
                    // Changing target to detail here creates a NEW transition while Compose
                    // also settles the interrupted initial values. That produces a second
                    // whole-page movement after release. Rewind the same shared bounds first.
                    val restoreFraction = transitionState.fraction
                    val restoreNanos = (duration * restoreFraction).toLong() * 1_000_000L
                    if (restoreNanos > 0L && !reduceMotion) {
                        val start = withFrameNanos { it }
                        do {
                            val elapsed = withFrameNanos { it } - start
                            val progress = (elapsed.toFloat() / restoreNanos).coerceIn(0f, 1f)
                            transitionState.seekTo(restoreFraction * (1f - progress), null)
                        } while (progress < 1f)
                    }
                    // Both endpoints now match the original detail pixels; reset state
                    // without starting another bounds/fade animation.
                    transitionState.snapTo(detail)
                }
            }
            throw cancelled
        }
    }
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sharedScope = this
        val transition = rememberTransition(transitionState, label = "shared-card-container")
        transition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            contentKey = { if (it == null) "source" else "destination" },
            // Shared bounds own geometry; AnimatedContent must not add its default size spring.
            transitionSpec = { (EnterTransition.None togetherWith ExitTransition.None).using(null) },
        ) { target ->
            CompositionLocalProvider(
                LocalSharedTransitionScope provides if (reduceMotion) null else sharedScope,
                LocalAnimatedVisibilityScope provides this,
            ) {
                if (target == null) {
                    savedHub.SaveableStateProvider("source") { content(openDetail, !detailVisible) }
                } else {
                    destination(target, closeDetail, replaceDetail, detailContentReady,
                        sharedCardContainerBounds(Modifier.fillMaxSize(), keyOf(target)))
                }
            }
        }
    }
}
