package com.android.purebilibili.feature.search

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.android.purebilibili.core.ui.LocalAnimatedVisibilityScope
import com.android.purebilibili.core.ui.LocalSharedTransitionScope
import com.android.purebilibili.core.ui.transition.SharedCardContainerHost
import com.android.purebilibili.core.ui.transition.sharedCardContainerBounds

internal fun searchUpContainerKey(mid: Long) = "search-up-container:$mid"
internal fun searchBangumiContainerKey(seasonId: Long) = "search-bangumi-container:$seasonId"

internal data class SearchCardTarget(val isUp: Boolean, val id: Long, val sourceKey: String)
private val SearchCardTargetSaver = Saver<SearchCardTarget?, List<Any>>(
    save = { it?.let { target -> listOf(target.isUp, target.id, target.sourceKey) } ?: emptyList() },
    restore = { if (it.isEmpty()) null else SearchCardTarget(it[0] as Boolean, it[1] as Long, it[2] as String) },
)

@OptIn(ExperimentalSharedTransitionApi::class)
private val LocalSearchResultSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
private val LocalSearchResultVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Reuse Bangumi's exact host. Result expansion does not push a second navigation entry. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun SearchResultContainerHost(
    content: @Composable ((Long, String) -> Unit, (Long, String) -> Unit, Boolean) -> Unit,
    destination: @Composable (SearchCardTarget, () -> Unit, (SearchCardTarget) -> Unit, Boolean, Modifier) -> Unit,
) {
    val parentSharedScope = LocalSharedTransitionScope.current
    val parentVisibilityScope = LocalAnimatedVisibilityScope.current
    SharedCardContainerHost<SearchCardTarget>(
        saver = SearchCardTargetSaver,
        keyOf = { it.sourceKey },
        content = { open, visible ->
            CompositionLocalProvider(
                LocalSearchResultSharedScope provides LocalSharedTransitionScope.current,
                LocalSearchResultVisibilityScope provides LocalAnimatedVisibilityScope.current,
                // Existing video cards keep exactly the scopes they had before this host.
                LocalSharedTransitionScope provides parentSharedScope,
                LocalAnimatedVisibilityScope provides parentVisibilityScope,
            ) {
                content({ id, key -> open(SearchCardTarget(true, id, key)) },
                    { id, key -> open(SearchCardTarget(false, id, key)) }, visible)
            }
        },
        destination = destination,
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun Modifier.searchResultContainerBounds(key: String): Modifier = sharedCardContainerBounds(
    modifier = this,
    key = key,
    sharedScope = LocalSearchResultSharedScope.current,
    visibilityScope = LocalSearchResultVisibilityScope.current,
)
