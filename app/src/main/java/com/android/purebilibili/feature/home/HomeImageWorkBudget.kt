package com.android.purebilibili.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal const val HOME_CARD_AUXILIARY_WORK_SETTLE_MS = 180L
internal const val HOME_COVER_PRELOAD_SETTLE_MS = 360L

/** Visible covers resume immediately; optional work waits for a quiet page. */
@Composable
internal fun rememberHomeCardAuxiliaryWorkAllowed(
    pageActive: Boolean,
    isScrolling: Boolean,
): Boolean {
    // New motion invalidates readiness synchronously, before effects are cancelled.
    // A subsequent idle period always gets its own cancellable settling window.
    var ready by remember(pageActive, isScrolling) { mutableStateOf(false) }
    LaunchedEffect(pageActive, isScrolling) {
        if (pageActive && !isScrolling) {
            delay(HOME_CARD_AUXILIARY_WORK_SETTLE_MS)
            ready = true
        }
    }
    return pageActive && !isScrolling && ready
}
