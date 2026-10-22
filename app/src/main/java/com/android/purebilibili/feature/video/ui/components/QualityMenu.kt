package com.android.purebilibili.feature.video.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.purebilibili.core.store.SettingsManager

@Composable
fun QualitySelectionMenu(
    qualities: List<String>, qualityIds: List<Int> = emptyList(),
    switchableQualityIds: List<Int> = emptyList(), currentQuality: String,
    isLoggedIn: Boolean = false, isVip: Boolean = false,
    onQualitySelected: (Int) -> Unit, onDismiss: () -> Unit,
    @Suppress("UNUSED_PARAMETER") useDialog: Boolean = false,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
    anchorBounds: IntRect? = null,
) {
    PlayerSelectionPopup(
        title = "画质选择", showTitle = false,
        onDismissRequest = onDismiss, placement = placement,
        anchorBounds = anchorBounds,
    ) {
        qualities.forEachIndexed { index, quality ->
            val qualityId = qualityIds.getOrNull(index) ?: 0
            val selected = quality == currentQuality
            val hint = qualityUnavailableReason(
                qualityId = qualityId,
                playableQualityIds = switchableQualityIds,
                isLoggedIn = isLoggedIn,
                isVip = isVip
            )
            PlayerSelectionItem(
                modifier = Modifier.playerPopupSelectedOption(selected),
                text = quality, summary = hint, selected = selected,
                index = index, optionCount = qualities.size,
                enabled = hint == null && !selected,
                onClick = { onQualitySelected(index) },
            )
        }
    }
}

enum class SpeedSelectionMenuPlacement { CENTER, RIGHT_SIDE, ANCHOR_END }

@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float, onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit, placement: SpeedSelectionMenuPlacement,
    anchorBounds: IntRect? = null,
) {
    val context = LocalContext.current
    val speedOptions by SettingsManager.getPlaybackSpeedOptions(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val options = remember(speedOptions, currentSpeed) {
        if (currentSpeed in speedOptions) speedOptions
        else (speedOptions + currentSpeed).sorted()
    }
    PlayerSelectionPopup(
        title = "播放速度", showTitle = false, onDismissRequest = onDismiss,
        placement = when (placement) {
            SpeedSelectionMenuPlacement.RIGHT_SIDE -> PlayerListPopupPlacement.END
            SpeedSelectionMenuPlacement.CENTER -> PlayerListPopupPlacement.CENTER
            SpeedSelectionMenuPlacement.ANCHOR_END -> PlayerListPopupPlacement.ANCHOR_END
        },
        anchorBounds = anchorBounds,
    ) {
        options.forEachIndexed { index, speed ->
            val selected = speed == currentSpeed
            PlayerSelectionItem(
                modifier = Modifier.playerPopupSelectedOption(selected),
                text = "${speed}X", selected = selected,
                index = index, optionCount = options.size,
                enabled = !selected,
                onClick = { onSpeedSelected(speed) },
            )
        }
    }
}

@Composable
fun SpeedSelectionMenuDialog(
    currentSpeed: Float, onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    placement: SpeedSelectionMenuPlacement = SpeedSelectionMenuPlacement.CENTER,
    anchorBounds: IntRect? = null,
) = SpeedSelectionMenu(currentSpeed, onSpeedSelected, onDismiss, placement, anchorBounds)
