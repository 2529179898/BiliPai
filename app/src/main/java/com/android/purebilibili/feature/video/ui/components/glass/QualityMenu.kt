package com.android.purebilibili.feature.video.ui.components.glass

import androidx.compose.runtime.Composable
import com.android.purebilibili.feature.video.ui.components.PlayerListPopupPlacement
import com.android.purebilibili.feature.video.ui.components.QualitySelectionMenu as NativeQualitySelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenu as NativeSpeedSelectionMenu
import com.android.purebilibili.feature.video.ui.components.SpeedSelectionMenuPlacement as NativeSpeedSelectionMenuPlacement

/** 页面保留玻璃控制栏，选择菜单统一按当前主题呈现。 */
@Composable
fun QualitySelectionMenu(
    qualities: List<String>, qualityIds: List<Int> = emptyList(),
    switchableQualityIds: List<Int> = emptyList(), currentQuality: String,
    isLoggedIn: Boolean = false, isVip: Boolean = false,
    onQualitySelected: (Int) -> Unit, onDismiss: () -> Unit,
    useDialog: Boolean = false,
    placement: PlayerListPopupPlacement = PlayerListPopupPlacement.CENTER,
) = NativeQualitySelectionMenu(
    qualities = qualities,
    qualityIds = qualityIds,
    switchableQualityIds = switchableQualityIds,
    currentQuality = currentQuality,
    isLoggedIn = isLoggedIn,
    isVip = isVip,
    onQualitySelected = onQualitySelected,
    onDismiss = onDismiss,
    useDialog = useDialog,
    placement = placement,
)

enum class SpeedSelectionMenuPlacement { CENTER, RIGHT_SIDE }

@Composable
fun SpeedSelectionMenu(
    currentSpeed: Float, onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit, placement: SpeedSelectionMenuPlacement,
) = NativeSpeedSelectionMenu(
    currentSpeed = currentSpeed,
    onSpeedSelected = onSpeedSelected,
    onDismiss = onDismiss,
    placement = when (placement) {
        SpeedSelectionMenuPlacement.CENTER -> NativeSpeedSelectionMenuPlacement.CENTER
        SpeedSelectionMenuPlacement.RIGHT_SIDE -> NativeSpeedSelectionMenuPlacement.RIGHT_SIDE
    },
)

@Composable
fun SpeedSelectionMenuDialog(
    currentSpeed: Float, onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit,
    placement: SpeedSelectionMenuPlacement = SpeedSelectionMenuPlacement.CENTER,
) = SpeedSelectionMenu(currentSpeed, onSpeedSelected, onDismiss, placement)
