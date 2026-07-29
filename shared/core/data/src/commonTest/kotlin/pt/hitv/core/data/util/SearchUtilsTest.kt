package pt.hitv.core.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the word-order-independent channel search.
 *
 * The original ANDs one `LOWER(name) LIKE '%word%'` per word (`SearchUtils.createFlexibleSearchQuery`
 * feeding `DAOChannel.searchChannelsFlexible`), so the words may appear in any order. The port had
 * collapsed that into a single `%word1%word2%` pattern, which silently requires the *typed* order —
 * "sports hd" then failed to find "HD Sports". Multi-word channel names are the norm in IPTV
 * playlists, so this missed matches constantly while looking like a working search.
 *
 * These tests cover the Kotlin half: slot packing, the overflow tail, and the `matchesOverflowWords`
 * predicate that stands in for slots the fixed-arity SQL cannot provide. The SQL half is the
 * `(:wN = '' OR LOWER(name) LIKE :wN)` guard in `Channel.sq`, which relies on the empty string this
 * class asserts for unused slots.
 */
class SearchUtilsTest {

    // ---- slot packing -------------------------------------------------------------------

    @Test
    fun `every word becomes its own LIKE pattern`() {
        val slots = SearchUtils.flexibleLikeSlots("sports hd")
        assertEquals("%sports%", slots[0])
        assertEquals("%hd%", slots[1])
    }

    @Test
    fun `unused slots are empty strings so the SQL guard disables them`() {
        val slots = SearchUtils.flexibleLikeSlots("sports hd")
        // Not "%%" — the query tests `:wN = ''`, and "%%" would match every row rather than
        // being skipped. Getting this wrong is silent: the query still runs and still returns
        // rows, just not the right ones.
        assertEquals(List(SearchUtils.FLEXIBLE_SEARCH_SLOTS - 2) { "" }, slots.drop(2))
    }

    @Test
    fun `slot list is always exactly the arity the query expects`() {
        // The generated SQLDelight function takes a fixed number of word parameters; the call
        // sites index slots[0]..slots[N-1] positionally, so a short list is an exception at
        // runtime, not a compile error.
        listOf("", "one", "one two three", "a b c d e f g h i j").forEach { query ->
            assertEquals(
                SearchUtils.FLEXIBLE_SEARCH_SLOTS,
                SearchUtils.flexibleLikeSlots(query).size,
                "wrong slot count for \"$query\"",
            )
        }
    }

    @Test
    fun `a blank query disables every slot`() {
        assertEquals(List(SearchUtils.FLEXIBLE_SEARCH_SLOTS) { "" }, SearchUtils.flexibleLikeSlots("   "))
    }

    @Test
    // No comma in the name: Kotlin/Native rejects it, and this file is commonTest.
    fun `punctuation is split into separate words - matching the original`() {
        // normalizeSearchWords replaces non-alphanumerics with spaces, so "sport-tv" is two words.
        val slots = SearchUtils.flexibleLikeSlots("sport-tv!")
        assertEquals("%sport%", slots[0])
        assertEquals("%tv%", slots[1])
        assertEquals("", slots[2])
    }

    @Test
    fun `words are lowercased because the query only lowercases the column`() {
        // The SQL is `LOWER(name) LIKE :wN` — the pattern itself is never lowered, so an
        // uppercase pattern would match nothing.
        assertEquals("%sports%", SearchUtils.flexibleLikeSlots("SPORTS").first())
    }

    // ---- overflow beyond the fixed slot count -------------------------------------------

    @Test
    fun `queries within the slot count have no overflow`() {
        val query = List(SearchUtils.FLEXIBLE_SEARCH_SLOTS) { "w$it" }.joinToString(" ")
        assertTrue(SearchUtils.overflowSearchWords(query).isEmpty())
    }

    @Test
    fun `words past the slot count overflow rather than being dropped`() {
        // Dropping them would make the port *looser* than the original — returning rows the
        // original excludes. Overflow keeps the match strict.
        val query = List(SearchUtils.FLEXIBLE_SEARCH_SLOTS + 2) { "w$it" }.joinToString(" ")
        assertEquals(
            listOf("w${SearchUtils.FLEXIBLE_SEARCH_SLOTS}", "w${SearchUtils.FLEXIBLE_SEARCH_SLOTS + 1}"),
            SearchUtils.overflowSearchWords(query),
        )
    }

    @Test
    fun `overflow words are applied as substring matches like the SQL slots`() {
        assertTrue(SearchUtils.matchesOverflowWords("Sport TV 1 HD Portugal", listOf("portugal")))
        assertTrue(SearchUtils.matchesOverflowWords("Sport TV 1 HD Portugal", listOf("portugal", "hd")))
        assertFalse(SearchUtils.matchesOverflowWords("Sport TV 1 HD Portugal", listOf("brasil")))
    }

    @Test
    fun `overflow matching is case-insensitive on the column side`() {
        assertTrue(SearchUtils.matchesOverflowWords("SPORT TV", listOf("sport")))
    }

    @Test
    fun `an empty overflow list keeps every row`() {
        assertTrue(SearchUtils.matchesOverflowWords("anything", emptyList()))
        // ...including rows whose name is null, which the schema allows.
        assertTrue(SearchUtils.matchesOverflowWords(null, emptyList()))
    }

    @Test
    fun `a null name cannot satisfy a non-empty overflow`() {
        assertFalse(SearchUtils.matchesOverflowWords(null, listOf("sport")))
    }

    // ---- the regression itself ----------------------------------------------------------

    @Test
    fun `word order does not change the slot set - the regression`() {
        // The whole point: both orderings produce the same AND-ed conditions, so both find
        // "HD Sports". Under the old `%sports%hd%` pattern only one ordering matched.
        assertEquals(
            SearchUtils.flexibleLikeSlots("sports hd").toSet(),
            SearchUtils.flexibleLikeSlots("hd sports").toSet(),
        )
    }
}
