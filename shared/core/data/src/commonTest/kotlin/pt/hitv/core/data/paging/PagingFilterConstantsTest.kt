package pt.hitv.core.data.paging

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The movie and TV-show filter constants are two parallel sets holding identical strings, and the
 * repositories branch on whichever name they imported. `SeriesScreen` passes
 * [FILTER_CONTINUE_WATCHING] while `TvShowRepositoryImpl` matches `MOVIE_FILTER_CONTINUE_WATCHING`;
 * that only works because the values agree.
 *
 * The aliasing hid a real bug — the series repository had no Continue Watching branch, so "See All"
 * on that row opened an empty screen while the movie equivalent worked. If the two sets ever drift,
 * every screen wired through the other name empties out silently, with nothing to catch it.
 *
 * So: pin them.
 */
class PagingFilterConstantsTest {

    @Test
    fun `movie and series filter constants stay in lockstep`() {
        assertEquals(MOVIE_FILTER_FAVORITES, FILTER_FAVORITES)
        assertEquals(MOVIE_FILTER_RECENTLY_VIEWED, FILTER_RECENTLY_VIEWED)
        assertEquals(MOVIE_FILTER_LAST_ADDED, FILTER_LAST_ADDED)
        assertEquals(MOVIE_FILTER_TOP_RATED, FILTER_TOP_RATED)
        assertEquals(MOVIE_FILTER_CONTINUE_WATCHING, FILTER_CONTINUE_WATCHING)
        assertEquals(MOVIE_FILTER_ALL, FILTER_ALL)
    }

    @Test
    fun `filter values are distinct from one another`() {
        // Two filters sharing a value would make one of them unreachable.
        val values = listOf(
            MOVIE_FILTER_FAVORITES,
            MOVIE_FILTER_RECENTLY_VIEWED,
            MOVIE_FILTER_LAST_ADDED,
            MOVIE_FILTER_TOP_RATED,
            MOVIE_FILTER_CONTINUE_WATCHING,
            MOVIE_FILTER_ALL,
        )
        assertEquals(values.size, values.toSet().size, "duplicate filter value: $values")
    }
}
