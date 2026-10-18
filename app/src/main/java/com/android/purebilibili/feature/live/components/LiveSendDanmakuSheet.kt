package com.android.purebilibili.feature.live.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.purebilibili.core.ui.AppContentColors
import com.android.purebilibili.core.ui.AppShapes
import com.android.purebilibili.core.ui.AppSpacingTokens
import com.android.purebilibili.core.ui.AppSurfaceTokens
import com.android.purebilibili.core.ui.ContainerLevel
import com.android.purebilibili.core.ui.LocalNavigationBackHandler
import com.android.purebilibili.core.ui.components.AppButton
import com.android.purebilibili.core.ui.components.AppFilterChip
import com.android.purebilibili.core.ui.components.AppIcon
import com.android.purebilibili.core.ui.components.AppIconButton
import com.android.purebilibili.core.ui.components.AppSurface
import com.android.purebilibili.core.ui.components.AppText
import com.android.purebilibili.data.repository.LiveDanmakuPermission
import com.android.purebilibili.feature.live.LiveDanmakuItem

/** 弹幕默认颜色（白色） */
private const val LIVE_DANMAKU_DEFAULT_COLOR = 16777215
/** 弹幕默认模式（1 = 滚动） */
private const val LIVE_DANMAKU_DEFAULT_MODE = 1
/** 颜色选择器最多展示的色块数量 */
private const val LIVE_DANMAKU_MAX_COLOR_SWATCHES = 8

/**
 * 直播专用键盘输入栏：不创建对话框窗口、遮罩或默认展开的样式面板。
 * 草稿与回复由调用方持有，关闭输入仅释放焦点；颜色、模式与表情按需展开。
 * 此布局适用于全部直播材质，不改变通用 MD3 / Miuix 组件默认值。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveSendDanmakuSheet(
    onDismiss: () -> Unit,
    onSend: (String, Int, Int) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    permission: LiveDanmakuPermission = LiveDanmakuPermission(),
    replyTarget: LiveDanmakuItem? = null,
    onOpenEmote: (() -> Unit)? = null,
    isSending: Boolean = false,
    selectedColor: Int = LIVE_DANMAKU_DEFAULT_COLOR,
    selectedMode: Int = LIVE_DANMAKU_DEFAULT_MODE,
    onColorSelected: (Int) -> Unit = {},
    onModeSelected: (Int) -> Unit = {},
    onClearReply: () -> Unit = {},
    emoticonContent: (@Composable () -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val showingEmoticons = emoticonContent != null
    var showStyle by rememberSaveable { mutableStateOf(false) }
    val maxLength = permission.maxLength.takeIf { it > 0 } ?: 40
    val effectiveColor = permission.availableColors.firstOrNull { it.color == selectedColor }?.color
        ?: permission.availableColors.firstOrNull()?.color
        ?: LIVE_DANMAKU_DEFAULT_COLOR
    val effectiveMode = permission.availableModes.firstOrNull { it.mode == selectedMode }?.mode
        ?: permission.availableModes.firstOrNull()?.mode
        ?: LIVE_DANMAKU_DEFAULT_MODE
    val canSend = !isSending && permission.canSend &&
        message.trim().isNotEmpty() && message.length <= maxLength
    val dismiss = {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        onDismiss()
    }
    val send = {
        if (canSend) onSend(message.trim(), effectiveColor, effectiveMode)
    }

    LaunchedEffect(showingEmoticons) {
        if (showingEmoticons) {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        } else {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }
    LaunchedEffect(effectiveColor, effectiveMode) {
        if (selectedColor != effectiveColor) onColorSelected(effectiveColor)
        if (selectedMode != effectiveMode) onModeSelected(effectiveMode)
    }
    LocalNavigationBackHandler(enabled = true) { dismiss() }

    // IME 消费返回键后不自动重获焦点；若输入栏仍在，再次返回或点外关闭。
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .imePadding()
            .navigationBarsPadding()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val advancedMaxHeight = maxHeight * 0.4f
        // 透明命中层只负责点外关闭，不给直播画面添加半屏遮罩。
        Box(
            Modifier.fillMaxSize().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "关闭输入，保留草稿",
                onClick = dismiss,
            )
        )
        AppSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            color = AppSurfaceTokens.surface(),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = AppSpacingTokens.Small, vertical = AppSpacingTokens.ExtraSmall),
            ) {
                if (replyTarget != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppText(
                            text = "回复 @${replyTarget.uname.ifBlank { replyTarget.uid.toString() }}",
                            modifier = Modifier.weight(1f).padding(start = AppSpacingTokens.Small),
                            color = AppContentColors.secondaryText(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AppIconButton(onClick = onClearReply, enabled = !isSending) {
                            AppIcon(Icons.Outlined.Close, "取消回复，保留正文")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = { if (!isSending) onMessageChange(it.take(maxLength)) },
                        modifier = Modifier.weight(1f)
                            .onFocusChanged {
                                if (it.isFocused && showingEmoticons) onOpenEmote?.invoke()
                            }
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppContentColors.primaryText()),
                        cursorBrush = SolidColor(AppContentColors.accent()),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = AppSpacingTokens.Small),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (message.isEmpty()) {
                                    AppText(
                                        text = if (!permission.canSend) permission.statusText
                                            else if (replyTarget == null) "发个弹幕…" else "输入回复…",
                                        color = AppContentColors.secondaryText(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    if (onOpenEmote != null) {
                        AppIconButton(
                            enabled = !isSending,
                            onClick = {
                                showStyle = false
                                onOpenEmote()
                            },
                        ) {
                            AppIcon(
                                if (showingEmoticons) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                                if (showingEmoticons) "切换键盘" else "表情",
                            )
                        }
                    }
                    AppIconButton(
                        enabled = !isSending,
                        onClick = {
                            showStyle = !showStyle
                            if (showingEmoticons) onOpenEmote?.invoke()
                        },
                    ) {
                        AppIcon(Icons.Outlined.MoreHoriz, if (showStyle) "收起弹幕设置" else "弹幕设置与字数")
                    }
                    AppButton(enabled = canSend, onClick = { send() }) {
                        AppText(if (isSending) "发送中…" else "发送")
                    }
                }
                if (!permission.canSend) {
                    AppText(
                        permission.statusText,
                        color = AppContentColors.secondaryText(),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (showStyle && !showingEmoticons) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = advancedMaxHeight)
                            .verticalScroll(rememberScrollState())
                            .padding(AppSpacingTokens.Small),
                        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                    ) {
                        AppText(
                            "${permission.statusText} · ${message.length}/$maxLength",
                            color = AppContentColors.secondaryText(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (permission.availableColors.isNotEmpty()) {
                            DanmakuColorSelector(
                                options = permission.availableColors,
                                selectedColor = effectiveColor,
                                onColorSelected = { if (!isSending) onColorSelected(it) },
                            )
                        }
                        if (permission.availableModes.isNotEmpty()) {
                            DanmakuModeSelector(
                                options = permission.availableModes,
                                selectedMode = effectiveMode,
                                onModeSelected = { if (!isSending) onModeSelected(it) },
                            )
                        }
                        com.android.purebilibili.core.ui.components.AppTextButton(onClick = dismiss) {
                            AppText("关闭输入，保留草稿")
                        }
                    }
                }
                if (showingEmoticons) {
                    Box(Modifier.fillMaxWidth().heightIn(max = advancedMaxHeight)) {
                        emoticonContent?.invoke()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DanmakuColorSelector(
    options: List<com.android.purebilibili.data.repository.LiveDanmakuColorOption>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
    ) {
        AppText(
            text = "弹幕颜色",
            color = AppContentColors.secondaryText(),
            style = MaterialTheme.typography.labelMedium
        )
        // FlowRow：窄屏自动换行，避免 8 个色块横向溢出
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small)
        ) {
            options.take(LIVE_DANMAKU_MAX_COLOR_SWATCHES).forEach { option ->
                val argb = (0xFF000000L or option.color.toLong()).toInt()
                val isSelected = selectedColor == option.color
                val swatchColor = Color(argb)
                // 外层 48dp 触摸目标（满足最小交互尺寸），内层 24dp 视觉色块
                Box(
                    modifier = Modifier
                        .size(AppSpacingTokens.TripleExtraLarge)
                        .clickable(
                            role = Role.RadioButton,
                            onClickLabel = option.name,
                            onClick = { onColorSelected(option.color) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = option.name
                            this.selected = isSelected
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(AppSpacingTokens.ExtraLarge)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) {
                                    AppSpacingTokens.Micro
                                } else {
                                    AppSurfaceTokens.OutlineWidth
                                },
                                color = if (isSelected) {
                                    AppContentColors.accent()
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            AppIcon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                // 浅色色块用黑色勾，深色用白色勾，保证对比度
                                tint = if (swatchColor.luminance() > 0.5f) {
                                    Color.Black
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(AppSpacingTokens.Large)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DanmakuModeSelector(
    options: List<com.android.purebilibili.data.repository.LiveDanmakuModeOption>,
    selectedMode: Int,
    onModeSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Only render a selector when the room actually offers a choice.
    if (options.size == 1) {
        AppText(
            text = "弹幕模式：${options.single().name}",
            modifier = modifier,
            color = AppContentColors.secondaryText(),
            style = MaterialTheme.typography.labelMedium,
        )
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
        ) {
            AppText(
                text = "弹幕模式",
                color = AppContentColors.secondaryText(),
                style = MaterialTheme.typography.labelMedium,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(AppSpacingTokens.Small),
                verticalArrangement = Arrangement.spacedBy(AppSpacingTokens.ExtraSmall),
            ) {
                options.forEach { option ->
                    AppFilterChip(
                        selected = selectedMode == option.mode,
                        onClick = { onModeSelected(option.mode) },
                        label = { AppText(option.name) },
                    )
                }
            }
        }
    }
}
