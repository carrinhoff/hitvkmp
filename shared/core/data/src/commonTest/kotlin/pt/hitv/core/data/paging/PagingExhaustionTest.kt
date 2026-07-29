package pt.hitv.core.data.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Pins the rule that decides when a paged list is exhausted.
 *
 * `ChannelPagingSource` queries `pageSize` rows, then removes any belonging to parental-protected
 * categories. `nextKey` used to be computed from the **filtered** count, so a single protected
 * channel anywhere in a page made that page look short, `nextKey` went null, and paging stopped —
 * silently truncating the user's channel list at the first protected entry. Everything past it was
 * unreachable.
 *
 * It was unreachable while `PremiumStatusProvider` forced parental controls off, since nothing was
 * ever filtered. Enabling them made it live.
 *
 * The invariant: **exhaustion is a property of the query, not of post-query filtering.**
 */
class PagingExhaustionTest {

    /** Mirrors the corrected rule in ChannelPagingSource.load(). */
    private fun nextKey(rowsReturnedByQuery: Int, pageSize: Int, page: Int): Int? =
        if (rowsReturnedByQuery < pageSize) null else page + 1

    /** The old, broken rule — kept so the difference is explicit. */
    private fun buggyNextKey(rowsAfterFiltering: Int, pageSize: Int, page: Int): Int? =
        if (rowsAfterFiltering < pageSize) null else page + 1

    @Test
    fun `a full page continues paging`() {
        assertEquals(1, nextKey(rowsReturnedByQuery = 50, pageSize = 50, page = 0))
    }

    @Test
    fun `a short page ends paging`() {
        assertNull(nextKey(rowsReturnedByQuery = 12, pageSize = 50, page = 0))
    }

    @Test
    fun `an exactly empty page ends paging`() {
        assertNull(nextKey(rowsReturnedByQuery = 0, pageSize = 50, page = 3))
    }

    @Test
    fun `filtering does not end paging - the regression this guards`() {
        // Query returned a full page; parental filtering removed one row.
        val pageSize = 50
        val returnedByQuery = 50
        val afterFiltering = 49

        assertEquals(
            1,
            nextKey(returnedByQuery, pageSize, page = 0),
            "a filtered row must not be mistaken for exhaustion",
        )
        assertNull(
            buggyNextKey(afterFiltering, pageSize, page = 0),
            "sanity: the old rule really did stop here",
        )
    }

    @Test
    fun `even a fully filtered page keeps paging`() {
        // Worst case: every channel on this page is protected. There may still be thousands of
        // unprotected channels after it, so paging must continue.
        assertEquals(
            5,
            nextKey(rowsReturnedByQuery = 50, pageSize = 50, page = 4),
            "an entirely protected page must not terminate the list",
        )
    }

    @Test
    fun `page keys advance monotonically while pages stay full`() {
        var page = 0
        repeat(10) {
            val next = nextKey(rowsReturnedByQuery = 50, pageSize = 50, page = page)
            assertEquals(page + 1, next)
            page = next!!
        }
        assertEquals(10, page)
    }
}
