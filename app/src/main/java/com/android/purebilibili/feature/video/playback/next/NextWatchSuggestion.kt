package com.android.purebilibili.feature.video.playback.next

/** An immutable, clickable snapshot. Looking ahead never advances the playback queue. */
internal data class NextWatchSuggestion(
    val sourceBvid: String,
    val sourceCid: Long,
    val bvid: String,
    val cid: Long,
    val title: String,
    val cover: String,
    val label: String,
    val pageIndex: Int? = null,
    val playlistIndex: Int? = null,
)

internal fun isNextWatchWindow(durationMs: Long, positionMs: Long): Boolean =
    durationMs > 5_000 && positionMs >= 0 && durationMs - positionMs in 1..5_000

/** Media seconds, rounded up so the hint never shows zero before playback ends. */
internal fun nextWatchRemainingSeconds(durationMs: Long, positionMs: Long): Int? {
    if (!isNextWatchWindow(durationMs, positionMs)) return null
    return ((durationMs - positionMs + 999) / 1_000).toInt()
}

/** Same collection lookahead for the hint and end-of-playback navigation. */
internal fun resolveCollectionNextWatch(
    info: com.android.purebilibili.data.model.response.ViewInfo
): NextWatchSuggestion? {
    val step = PiliPlaybackOrder.step(info)
    val pageIndex = info.pages.indexOfFirst { it.cid == info.cid }
    val nextPageIndex = pageIndex + step
    if (pageIndex >= 0 && nextPageIndex in info.pages.indices) {
        val next = info.pages[nextPageIndex]
        return NextWatchSuggestion(info.bvid, info.cid, info.bvid, next.cid,
            next.part, info.pic, "下一 P", pageIndex = nextPageIndex)
    }
    val episodes = info.ugc_season?.sections?.flatMap { it.episodes }.orEmpty()
    val index = com.android.purebilibili.feature.video.viewmodel.resolveUgcSeasonEpisodeIndex(
        episodes, info.bvid, info.cid)
    val nextEpisodeIndex = index + step
    if (index >= 0 && nextEpisodeIndex in episodes.indices) {
        val next = episodes[nextEpisodeIndex]
        if (next.bvid.isNotBlank() && (next.bvid != info.bvid || next.cid != info.cid)) {
            return NextWatchSuggestion(info.bvid, info.cid, next.bvid, next.cid,
                next.title, next.arc?.pic.orEmpty(), "下一集")
        }
    }
    return null
}
