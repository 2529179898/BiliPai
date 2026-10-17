package com.android.purebilibili.core.ui.blur

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

/**
 * Shared app-shell backdrop for chrome drawn outside the navigation capture subtree.
 * Page-local controls must use a separate sibling source: this recording includes them,
 * so sampling it from inside a page can create a RenderNode cycle.
 */
val LocalFloatingChromeBackdrop = staticCompositionLocalOf<Backdrop?> { null }
