package com.android.purebilibili.feature.video.ui.components

import com.android.purebilibili.data.model.response.Page
import com.android.purebilibili.data.model.response.UgcEpisode
import com.android.purebilibili.data.model.response.UgcSeason

internal sealed interface PlayerEpisodeMenuTarget {
    data class Part(val pageIndex: Int) : PlayerEpisodeMenuTarget
    data class Episode(val episode: UgcEpisode) : PlayerEpisodeMenuTarget
}

internal data class PlayerEpisodeMenuEntry(
    val indexLabel: String,
    val title: String,
    val durationSeconds: Long,
    val target: PlayerEpisodeMenuTarget,
    val cover: String = "",
)

internal data class PlayerEpisodeMenuSection(
    val title: String,
    val entries: List<PlayerEpisodeMenuEntry>,
)

internal data class PlayerEpisodeMenuContent(
    val sections: List<PlayerEpisodeMenuSection>,
    val currentSectionIndex: Int = -1,
    val currentEntryIndex: Int = -1,
)

/** Player panel projection; below-video collection and part selectors remain independent. */
internal fun resolvePlayerEpisodeMenuContent(
    pages: List<Page>,
    currentPageIndex: Int,
    ugcSeason: UgcSeason?,
    currentBvid: String,
    currentCid: Long,
): PlayerEpisodeMenuContent {
    if (pages.size > 1) {
        return PlayerEpisodeMenuContent(
            sections = listOf(PlayerEpisodeMenuSection("选集", pages.mapIndexed { index, page ->
                PlayerEpisodeMenuEntry(
                    indexLabel = "P${page.page.takeIf { it > 0 } ?: (index + 1)}",
                    title = page.part.ifBlank { "第${index + 1}P" },
                    durationSeconds = page.duration,
                    target = PlayerEpisodeMenuTarget.Part(index),
                    cover = page.firstFrame,
                )
            })),
            currentSectionIndex = if (currentPageIndex in pages.indices) 0 else -1,
            currentEntryIndex = currentPageIndex.takeIf { it in pages.indices } ?: -1,
        )
    }

    val sections = ugcSeason?.sections.orEmpty().mapIndexedNotNull { sectionIndex, section ->
        if (section.episodes.isEmpty()) return@mapIndexedNotNull null
        PlayerEpisodeMenuSection(
            title = section.title.ifBlank { "分区 ${sectionIndex + 1}" },
            entries = buildList {
                section.episodes.forEachIndexed { episodeIndex, episode ->
                    val title = episode.title.ifBlank { episode.arc?.title.orEmpty() }
                        .ifBlank { "第${episodeIndex + 1}集" }
                    if (episode.pages.isEmpty()) {
                        add(PlayerEpisodeMenuEntry(
                            indexLabel = "${episodeIndex + 1}",
                            title = title,
                            durationSeconds = episode.arc?.duration?.toLong() ?: 0L,
                            cover = episode.arc?.pic.orEmpty().ifBlank { ugcSeason?.cover.orEmpty() },
                            target = PlayerEpisodeMenuTarget.Episode(episode),
                        ))
                    } else {
                        episode.pages.forEachIndexed { partIndex, page ->
                            val partNumber = page.page.takeIf { it > 0 } ?: (partIndex + 1)
                            add(PlayerEpisodeMenuEntry(
                                indexLabel = if (episode.pages.size > 1) {
                                    "${episodeIndex + 1} · P$partNumber"
                                } else {
                                    "${episodeIndex + 1}"
                                },
                                title = if (episode.pages.size > 1) {
                                    "$title · ${page.part.ifBlank { "第${partNumber}P" }}"
                                } else {
                                    title
                                },
                                durationSeconds = page.duration,
                                cover = page.firstFrame.ifBlank { episode.arc?.pic.orEmpty().ifBlank { ugcSeason?.cover.orEmpty() } },
                                target = PlayerEpisodeMenuTarget.Episode(
                                    if (page.cid > 0L && page.cid != episode.cid) {
                                        episode.copy(cid = page.cid)
                                    } else {
                                        episode
                                    },
                                ),
                            ))
                        }
                    }
                }
            },
        )
    }
    // Find an exact part across every section before falling back to the video's first row.
    // This avoids selecting P1 (or a duplicate bvid in an earlier section) while P2 is playing.
    fun findCurrent(exactPart: Boolean): PlayerEpisodeMenuContent? {
        if (currentBvid.isBlank()) return null
        sections.forEachIndexed { sectionIndex, section ->
            val entryIndex = section.entries.indexOfFirst { entry ->
                val episode = (entry.target as PlayerEpisodeMenuTarget.Episode).episode
                episode.bvid == currentBvid && (!exactPart || episode.cid == currentCid)
            }
            if (entryIndex >= 0) return PlayerEpisodeMenuContent(sections, sectionIndex, entryIndex)
        }
        return null
    }
    return (if (currentCid > 0L) findCurrent(exactPart = true) else null)
        ?: findCurrent(exactPart = false)
        ?: PlayerEpisodeMenuContent(sections)
}

/** Keep a season video's parts in its nested 45dp part strip rather than duplicating covers. */
internal fun groupPlayerEpisodeEntries(entries: List<PlayerEpisodeMenuEntry>): List<List<PlayerEpisodeMenuEntry>> {
    val groups = mutableListOf<MutableList<PlayerEpisodeMenuEntry>>()
    entries.forEach { entry ->
        val episode = (entry.target as? PlayerEpisodeMenuTarget.Episode)?.episode
        val previous = groups.lastOrNull()?.lastOrNull()?.target as? PlayerEpisodeMenuTarget.Episode
        if (episode != null && previous != null && episode.copy(cid = 0) == previous.episode.copy(cid = 0)) {
            groups.last().add(entry)
        } else {
            groups.add(mutableListOf(entry))
        }
    }
    return groups
}

/** Window coordinates. Inline panels occupy detail space, never the playing video. */
internal fun resolvePlayerPanelBounds(
    safe: androidx.compose.ui.unit.IntRect,
    player: androidx.compose.ui.unit.IntRect?,
    isFullscreen: Boolean,
    landscape: Boolean,
    maxSideWidth: Int,
    maxBottomHeight: Int? = null,
): androidx.compose.ui.unit.IntRect {
    if (!isFullscreen && player != null) {
        if (landscape && player.right < safe.right) {
            val left = player.right.coerceIn(safe.left, safe.right)
            if (left < safe.right) return androidx.compose.ui.unit.IntRect(left, safe.top, safe.right, safe.bottom)
        }
        if (!landscape && player.bottom < safe.bottom) {
            val heightLimit = maxBottomHeight?.coerceIn(0, safe.height) ?: safe.height
            val top = maxOf(player.bottom, safe.bottom - heightLimit).coerceIn(safe.top, safe.bottom)
            if (top < safe.bottom) return androidx.compose.ui.unit.IntRect(safe.left, top, safe.right, safe.bottom)
        }
    }
    return if (landscape) {
        val width = minOf(safe.width / 2, maxSideWidth.coerceAtLeast(0))
        androidx.compose.ui.unit.IntRect(safe.right - width, safe.top, safe.right, safe.bottom)
    } else {
        val height = minOf((safe.height * 0.7f).toInt(), maxBottomHeight?.coerceIn(0, safe.height) ?: safe.height)
        androidx.compose.ui.unit.IntRect(safe.left, safe.bottom - height, safe.right, safe.bottom)
    }
}
