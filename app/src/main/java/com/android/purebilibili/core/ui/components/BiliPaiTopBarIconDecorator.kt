package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.feature.home.components.BottomBarMatchedReusableLiquidDock
import com.android.purebilibili.feature.home.components.resolveFloatingDockGeometryScale

/** Same circular dock material used by floating chrome, without sampling its own icon. */
object BiliPaiTopBarIconDecorator : AppTopBarIconDecorator {
    @Composable
    override fun Render(modifier: Modifier, enabled: Boolean, content: @Composable (Modifier) -> Unit) {
        val ownsDrag = remember { mutableStateOf(false) }
        BottomBarMatchedReusableLiquidDock(
            shape = CircleShape,
            modifier = modifier
                .size(48.dp)
                .then(if (enabled) Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { ownsDrag.value = true },
                        onDragEnd = { ownsDrag.value = false },
                        onDragCancel = { ownsDrag.value = false },
                        onDrag = { change, _ -> change.consume() },
                    )
                } else Modifier),
            backdrop = LocalAppTopBarIconBackdrop.current,
            reuseEnabled = true,
            elasticFeedbackEnabled = enabled,
            allowConsumedDrag = { ownsDrag.value },
            useNeutralLiquidContainer = true,
            shellLensIntensity = resolveFloatingDockGeometryScale(48f),
        ) { glassSurfaceProvided ->
            CompositionLocalProvider(LocalAppIconButtonGlassSurfaceProvided provides glassSurfaceProvided) {
                Box(Modifier.size(48.dp)) { content(Modifier.matchParentSize()) }
            }
        }
    }
}
