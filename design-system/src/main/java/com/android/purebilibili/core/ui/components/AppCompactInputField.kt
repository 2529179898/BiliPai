package com.android.purebilibili.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.theme.AppUiStyle
import com.android.purebilibili.core.theme.LocalAppUiStyle
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppSurfaceTokens

/** Compact inline input without clipping a native field's 56dp decoration/minimum height. */
@Composable
fun AppCompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    height: Dp = 40.dp,
    containerColor: Color = AppSurfaceTokens.searchContainer(),
    shape: Shape = if (LocalAppUiStyle.current == AppUiStyle.MIUIX) RoundedCornerShape(12.dp) else CircleShape,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    focusRequester: FocusRequester? = null,
    autoFocusEnabled: Boolean = false,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val requester = focusRequester ?: remember { FocusRequester() }
    LaunchedEffect(requester, autoFocusEnabled) {
        if (autoFocusEnabled) requester.requestFocus()
    }
    val textColor = AppContentColors.primaryText()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = height)
            .clip(shape)
            .background(containerColor)
            .focusRequester(requester),
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        cursorBrush = SolidColor(AppContentColors.accent()),
        interactionSource = interactionSource,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(Modifier.size(height), contentAlignment = Alignment.Center) { leadingIcon() }
                }
                Box(
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        AppText(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppContentColors.secondaryText(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
                if (trailingIcon != null) {
                    Box(Modifier.size(height), contentAlignment = Alignment.Center) { trailingIcon() }
                }
            }
        },
    )
}
