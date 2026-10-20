package com.android.purebilibili.data.repository

import com.android.purebilibili.data.model.response.PlayUrlData
import kotlinx.coroutines.CancellationException

/** Actual URLs, rather than advertised tiers or cached account flags, define availability. */
fun PlayUrlData.playableVideoQualityIds(): List<Int> = dash?.video.orEmpty()
    .filter { it.id > 0 && it.getValidUrl().isNotBlank() }
    .map { it.id }
    .distinct()
    .sortedDescending()

fun PlayUrlData.advertisedVideoQualityIds(): List<Int> =
    (supportFormats.orEmpty().map { it.quality } + acceptQuality)
        .filter { it > 0 }
        .distinct()
        .sortedDescending()

fun missingPlaybackQualityBelowHighest(data: PlayUrlData): Int? {
    val available = data.playableVideoQualityIds().toSet()
    val highest = available.maxOrNull() ?: return null
    return data.supportFormats.orEmpty()
        .map { it.quality }
        .filter { it > 0 && it < highest && it !in available }
        .maxOrNull()
}

/**
 * PiliPlus supplements once, at the highest missing tier below the returned maximum.
 * Keep the first response's audio, language, progress and catalogue metadata intact.
 * Deduplicate by quality AND codec so AVC/HEVC/AV1 variants remain selectable.
 */
suspend fun supplementPlaybackQualities(
    initial: PlayUrlData,
    fetch: suspend (quality: Int) -> PlayUrlData?
): PlayUrlData {
    val quality = missingPlaybackQualityBelowHighest(initial) ?: return initial
    val supplement = try {
        fetch(quality)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        return initial
    } ?: return initial
    val dash = initial.dash ?: return initial
    val videos = (dash.video + supplement.dash?.video.orEmpty())
        .filter { it.id > 0 && it.getValidUrl().isNotBlank() }
        .distinctBy { Triple(it.id, it.codecid, it.codecs) }
        .sortedByDescending { it.id }
    return initial.copy(dash = dash.copy(video = videos))
}
