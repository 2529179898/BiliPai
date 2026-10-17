package com.android.purebilibili.core.util

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HomeCoverReturnPrefetchPolicyTest {

    private fun entry(bvid: String, url: String, cacheKey: String) =
        HomeCoverReturnPrefetchEntry(bvid = bvid, url = url, cacheKey = cacheKey)

    @BeforeTest
    fun setUp() {
        HomeCoverReturnPrefetchRegistry.clearForTest()
    }

    // --- resolveHomeCoverReturnPrefetchCandidates ---

    @Test
    fun candidates_putsSourceCardFirst() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_SOURCE", "url_s", "cover_BV_SOURCE_n"),
            entry("BV_B", "url_b", "cover_BV_B_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_SOURCE",
        )

        assertEquals("cover_BV_SOURCE_n", result.first().cacheKey)
        assertEquals(3, result.size)
    }

    @Test
    fun candidates_deduplicatesByCacheKey() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_A", "url_a2", "cover_BV_A_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = null,
        )

        assertEquals(1, result.size)
    }

    @Test
    fun candidates_skipsBlankUrlOrCacheKey() {
        val entries = listOf(
            entry("BV_A", "", "cover_BV_A_n"),
            entry("BV_B", "url_b", ""),
            entry("BV_C", "url_c", "cover_BV_C_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = null,
        )

        assertEquals(1, result.size)
        assertEquals("cover_BV_C_n", result.single().cacheKey)
    }

    @Test
    fun candidates_respectsMaxCount() {
        val entries = (0 until 10).map { entry("BV_$it", "url_$it", "cover_BV_${it}_n") }

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_9",
            maxCount = 3,
        )

        assertEquals(3, result.size)
        // 源卡优先占一个名额
        assertEquals("cover_BV_9_n", result.first().cacheKey)
    }

    @Test
    fun candidates_maxCountZeroReturnsEmpty() {
        val entries = listOf(entry("BV_A", "url_a", "cover_BV_A_n"))

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "BV_A",
            maxCount = 0,
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun candidates_emptySourceBvidFallsBackToVisibilityOrder() {
        val entries = listOf(
            entry("BV_A", "url_a", "cover_BV_A_n"),
            entry("BV_B", "url_b", "cover_BV_B_n"),
        )

        val result = resolveHomeCoverReturnPrefetchCandidates(
            visibleEntries = entries,
            sourceBvid = "  ",
        )

        assertEquals(2, result.size)
        assertEquals("cover_BV_A_n", result.first().cacheKey)
    }

    // --- HomeCoverReturnPrefetchRegistry ---

    @Test
    fun registry_keepsSnapshotAfterDisposeLikeSemantics() {
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "cover_BV_B_n"))

        // 详情打开时首页 scene dispose，注册表不清空——返回必须仍能取到离开前可视快照。
        assertEquals(2, HomeCoverReturnPrefetchRegistry.snapshot().size)
    }

    @Test
    fun registry_duplicateReportOnlyRefreshesOrder() {
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "cover_BV_B_n"))
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_A", "url_a", "cover_BV_A_n"))

        assertEquals(2, HomeCoverReturnPrefetchRegistry.snapshot().size)
        // 最新可见的 BV_A 排到最前（LRU 语义）
        assertEquals("cover_BV_A_n", HomeCoverReturnPrefetchRegistry.snapshot().first().cacheKey)
    }

    @Test
    fun registry_skipsBlankEntriesAndCapsSize() {
        repeat(100) { index ->
            HomeCoverReturnPrefetchRegistry.onCardVisible(
                entry("BV_$index", "url_$index", "cover_BV_${index}_n")
            )
        }
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_EMPTY", "", ""))

        assertEquals(64, HomeCoverReturnPrefetchRegistry.snapshot().size)
    }

    @Test
    fun registry_refreshesDecodeSizeWithoutDuplicatingOrMutatingEarlierSnapshot() {
        val first = entry("BV_A", "url_a", "key_a").copy(decodeWidthPx = 320, decodeHeightPx = 180)
        HomeCoverReturnPrefetchRegistry.onCardVisible(first)
        val earlier = HomeCoverReturnPrefetchRegistry.snapshot()
        HomeCoverReturnPrefetchRegistry.onCardVisible(entry("BV_B", "url_b", "key_b"))
        val updated = first.copy(decodeWidthPx = 640, decodeHeightPx = 360)
        HomeCoverReturnPrefetchRegistry.onCardVisible(updated)

        assertEquals(listOf(first), earlier)
        val current = HomeCoverReturnPrefetchRegistry.snapshot()
        assertEquals(2, current.size)
        assertEquals(updated, current.first())
        assertEquals(updated, resolveHomeCoverReturnPrefetchCandidates(current, "BV_A").first())
    }

    @Test
    fun invalidSourceEntryDoesNotDisplaceAValidSourceCover() {
        val valid = entry("BV_A", "url_a", "key_a")
        assertEquals(
            listOf(valid),
            resolveHomeCoverReturnPrefetchCandidates(listOf(valid.copy(url = ""), valid), "BV_A", 1),
        )
    }
}
