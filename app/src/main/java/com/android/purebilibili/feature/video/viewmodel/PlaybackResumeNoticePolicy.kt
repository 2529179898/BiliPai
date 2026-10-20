package com.android.purebilibili.feature.video.viewmodel

import com.android.purebilibili.core.util.FormatUtils

/** One notice belongs to the exact load that restored saved progress. */
internal data class PlaybackResumeNotice(
    val requestToken: Long,
    val bvid: String,
    val cid: Long,
    val positionMs: Long,
) {
    val mediaKey: String get() = "$bvid:$cid"
}

internal fun shouldAnnounceSavedPlaybackPosition(
    cachedPositionMs: Long,
    startPositionMs: Long,
    fallbackResumePositionMs: Long,
    ignoreSavedProgress: Boolean,
    skipPlayerPrepare: Boolean,
): Boolean = !ignoreSavedProgress && !skipPlayerPrepare && fallbackResumePositionMs <= 0L &&
    cachedPositionMs >= 1_000L && startPositionMs == cachedPositionMs

internal fun hasReachedSavedPlaybackPosition(
    targetPositionMs: Long,
    currentPositionMs: Long,
    playerReady: Boolean,
): Boolean = playerReady && targetPositionMs >= 1_000L &&
    currentPositionMs >= (targetPositionMs - 1_000L).coerceAtLeast(0L) &&
    currentPositionMs <= targetPositionMs + 5_000L

internal fun buildPlaybackResumeNoticeMessage(positionMs: Long): String =
    "欢迎回来，已为您定位至 ${FormatUtils.formatDuration(positionMs)}"
