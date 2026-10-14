package com.android.purebilibili.feature.video.ui.components

internal const val DANMAKU_SEND_VIP_GRADUAL_COLOR = -1

internal data class DanmakuSendOption<T>(
    val value: T,
    val label: String
)

internal fun shouldUseInlineDanmakuComposer(isFullscreenMode: Boolean): Boolean = isFullscreenMode

/** Sending stays pinned in fullscreen; inline cinema retains its roomy-width entry. */
internal fun shouldShowDanmakuSendInControlBar(
    isFullscreen: Boolean,
    widthDp: Int
): Boolean = isFullscreen || widthDp >= 720

internal fun resolveDanmakuInputPlaceholder(isLoggedIn: Boolean): String {
    return if (isLoggedIn) {
        "发个友善的弹幕见证当下"
    } else {
        "登录后发送弹幕"
    }
}

internal fun resolveDanmakuComposerLoginBlockedMessage(): String = "请先登录后再发送弹幕"

internal fun danmakuSendColorOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(16777215, "白色"),
    DanmakuSendOption(16646914, "红色"),
    DanmakuSendOption(16740868, "橙色"),
    DanmakuSendOption(16755202, "金色"),
    DanmakuSendOption(52224, "绿色"),
    DanmakuSendOption(41430, "蓝色"),
    DanmakuSendOption(13369971, "紫色"),
    DanmakuSendOption(2236962, "黑色"),
    DanmakuSendOption(DANMAKU_SEND_VIP_GRADUAL_COLOR, "会员渐变")
)

internal fun danmakuSendModeOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(1, "滚动"),
    DanmakuSendOption(5, "顶部"),
    DanmakuSendOption(4, "底部")
)

internal fun danmakuSendFontSizeOptions(): List<DanmakuSendOption<Int>> = listOf(
    DanmakuSendOption(18, "小"),
    DanmakuSendOption(25, "中"),
    DanmakuSendOption(36, "大")
)