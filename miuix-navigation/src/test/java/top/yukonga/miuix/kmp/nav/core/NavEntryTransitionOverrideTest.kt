package top.yukonga.miuix.kmp.nav.core

import androidx.compose.ui.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitionScope

class NavEntryTransitionOverrideTest {
    private data class Page(val id: Int) : NavKey

    @Test
    fun nestedInstancesResolveTheirOwnTransitionAndRetainItAfterRemoval() {
        val first = Page(1)
        val second = Page(2)
        fun transition() = object : NavTransition {
            override fun Modifier.transformEntry(scope: NavTransitionScope): Modifier = this
        }
        val default = transition()
        val one = transition()
        val two = transition()
        val overrides = mutableMapOf(first to one, second to two)
        val provider = entryProvider {
            transitionOverrides(swipeDismiss = NavSwipeDirection.LeftToRight) { key ->
                (key as? Page)?.let(overrides::get)
            }
            entry<Page>(transition = default, swipeDismiss = NavSwipeDirection.None) { }
        }
        val firstEntry = provider(first)
        val secondEntry = provider(second)
        assertSame(one, firstEntry.metadata[NAV_TRANSITION_METADATA_KEY])
        assertSame(two, secondEntry.metadata[NAV_TRANSITION_METADATA_KEY])
        assertEquals(NavSwipeDirection.LeftToRight, secondEntry.metadata[NAV_SWIPE_DISMISS_METADATA_KEY])
        overrides.remove(second)
        // A departing entry retains its original metadata while a fresh lookup uses the fallback.
        assertSame(two, secondEntry.metadata[NAV_TRANSITION_METADATA_KEY])
        assertSame(default, provider(second).metadata[NAV_TRANSITION_METADATA_KEY])
        assertEquals(NavSwipeDirection.None, provider(second).metadata[NAV_SWIPE_DISMISS_METADATA_KEY])
        assertSame(one, provider(first).metadata[NAV_TRANSITION_METADATA_KEY])
    }
}
