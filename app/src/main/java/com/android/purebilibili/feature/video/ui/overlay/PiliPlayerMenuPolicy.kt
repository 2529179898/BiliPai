package com.android.purebilibili.feature.video.ui.overlay

import java.net.URI
import kotlin.math.log10
import kotlin.math.roundToInt

internal fun normalizePlayerAmplification(value: Float): Float =
    if (value.isFinite()) (value.coerceIn(1f, 3f) * 20f).roundToInt() / 20f else 1f

/** LoudnessEnhancer accepts millibels, not a linear amplitude percentage. */
internal fun playerAmplificationMillibels(value: Float): Int =
    (2000.0 * log10(normalizePlayerAmplification(value).toDouble())).roundToInt()

internal fun isEditablePlaybackUrl(value: String, allowEmpty: Boolean = false): Boolean {
    if (value.isBlank()) return allowEmpty
    val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
    return (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank()
}

internal fun matchesPiliPlaybackIdentity(
    expectedBvid: String, expectedCid: Long, currentBvid: String, currentCid: Long
): Boolean = expectedBvid == currentBvid && expectedCid == currentCid
