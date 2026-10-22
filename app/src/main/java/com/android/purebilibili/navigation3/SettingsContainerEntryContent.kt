package com.android.purebilibili.navigation3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import com.android.purebilibili.core.ui.components.AppContainerNavigationSource

/** The page keeps one composition slot, including when its container boundary is released. */
@Composable
internal fun SettingsContainerEntryContent(
    source: AppContainerNavigationSource?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pageLayer = rememberGraphicsLayer()
    SideEffect { source?.pageLayer = pageLayer }
    DisposableEffect(source, pageLayer) {
        onDispose {
            if (source != null && source.pageLayer === pageLayer) source.pageLayer = null
        }
    }
    Box(
        modifier = modifier.fillMaxSize().drawWithContent {
            if (source == null) drawContent()
            else {
                pageLayer.record { this@drawWithContent.drawContent() }
                if (!source.recordPageOnly) drawLayer(pageLayer)
            }
        },
    ) { content() }
}
