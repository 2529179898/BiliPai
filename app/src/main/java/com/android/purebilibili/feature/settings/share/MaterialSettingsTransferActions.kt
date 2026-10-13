@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.android.purebilibili.feature.settings.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AdaptivePlainTooltipBox

@Composable
internal fun MaterialSettingsTransferActions(
    enabled: Boolean,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onImport: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = onExport, enabled = enabled) {
                    Text("导出到文件")
                }
            },
            trailingButton = {
                AdaptivePlainTooltipBox(text = "分享导出文件") {
                    SplitButtonDefaults.TrailingButton(onClick = onShare, enabled = enabled) {
                        Icon(Icons.Outlined.Share, contentDescription = "分享导出文件")
                    }
                }
            },
        )
        ButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            overflowIndicator = { menuState ->
                ButtonGroupDefaults.OverflowIndicator(menuState = menuState, enabled = enabled)
            },
        ) {
            clickableItem(label = "从文件导入", onClick = onImport, enabled = enabled, weight = 1f)
            clickableItem(label = "保存当前配置", onClick = onSave, enabled = enabled, weight = 1f)
        }
    }
}
