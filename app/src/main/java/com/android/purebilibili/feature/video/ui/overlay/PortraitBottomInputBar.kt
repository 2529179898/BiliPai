package com.android.purebilibili.feature.video.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.purebilibili.core.ui.components.AppText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import com.android.purebilibili.feature.video.ui.components.NativeDanmakuToggleButton

/**
 * 竖屏模式底部的输入栏
 * 包含：发弹幕输入框与弹幕开关。
 */
@Composable
fun PortraitBottomInputBar(
    onInputClick: () -> Unit,
    danmakuEnabled: Boolean,
    onDanmakuToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val layoutPolicy = remember(configuration.screenWidthDp) {
        resolvePortraitBottomInputBarLayoutPolicy(
            widthDp = configuration.screenWidthDp
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(
                horizontal = layoutPolicy.horizontalPaddingDp.dp,
                vertical = layoutPolicy.verticalPaddingDp.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val glass = LocalPlayerControlGlassEnabled.current
        PlayerControlGlassDock(
            enabled = true,
            modifier = Modifier
                .weight(1f)
                .height(layoutPolicy.inputHeightDp.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape((layoutPolicy.inputHeightDp / 2).dp))
                    .background(if (glass) Color.Transparent else Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = onInputClick)
                    .padding(horizontal = layoutPolicy.inputHorizontalPaddingDp.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.android.purebilibili.feature.video.ui.components.PiliPlusDanmakuSendIcon(
                        size = 20.dp,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(6.dp))
                    AppText(
                        text = "发弹幕...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(layoutPolicy.afterInputSpacingDp.dp))

        PlayerControlGlassDock(
            enabled = true,
            shape = CircleShape,
            modifier = Modifier.size(layoutPolicy.actionButtonSizeDp.dp),
        ) {
            NativeDanmakuToggleButton(
                enabled = danmakuEnabled,
                onToggle = onDanmakuToggle,
                activeTint = Color.White,
                inactiveTint = Color.White,
                modifier = Modifier.fillMaxSize(),
                iconSize = layoutPolicy.actionIconSizeDp.dp,
            )
        }
    }
}
