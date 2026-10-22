package com.android.purebilibili.feature.home.components.liquid

import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertNotSame

class InnerShadowBlurEffectCacheTest {
    @Test
    fun subpixelChangesAndRecoilReuseFilters() {
        val cache = InnerShadowBlurEffectCache()
        val first = cache.resolve(8f)
        assertSame(first, cache.resolve(8.1f))
        assertNotSame(first, cache.resolve(8.2f))
        cache.resolve(12f)
        assertSame(first, cache.resolve(8f))
    }

    @Test
    fun cacheIsBoundedAndKeepsRecentlyUsedRadii() {
        val cache = InnerShadowBlurEffectCache(capacity = 2)
        val first = cache.resolve(4f)
        val second = cache.resolve(8f)
        assertSame(first, cache.resolve(4f))
        cache.resolve(12f)
        assertSame(first, cache.resolve(4f))
        assertNotSame(second, cache.resolve(8f))
        cache.clear()
        assertNotSame(first, cache.resolve(4f))
    }

    @Test
    fun invisibleRadiiDoNotReuseVisibleFilters() {
        val cache = InnerShadowBlurEffectCache()
        val visible = cache.resolve(8f)
        for (radius in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(cache.resolve(radius))
        }
        assertSame(visible, cache.resolve(8f))
    }
}
