package pt.hitv.core.data.util

/**
 * Utility functions for search operations in repositories.
 */
object SearchUtils {

    /**
     * Normalizes a search query by extracting individual words,
     * lowercasing, and stripping non-alphanumeric characters.
     *
     * @return List of cleaned search words
     */
    fun normalizeSearchWords(searchQuery: String): List<String> {
        return searchQuery.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
    }

    /**
     * Creates an FTS query string from a search query.
     * Multi-word queries get each word suffixed with * for prefix matching.
     */
    fun createFtsQuery(searchQuery: String): String {
        return if (searchQuery.contains(" ")) {
            searchQuery.split(" ").joinToString(" ") { "$it*" }
        } else {
            "$searchQuery*"
        }
    }

    /**
     * Creates a LIKE pattern for flexible search.
     */
    fun createLikePattern(word: String): String = "%$word%"

    /**
     * Number of word slots the `*Flexible` SQLDelight queries expose.
     *
     * The original builds its WHERE clause dynamically and so has no limit; SQLDelight has no
     * dynamic SQL, so the slots are fixed and [overflowSearchWords] carries anything past them.
     * Must match the slot count in `Channel.sq`.
     */
    const val FLEXIBLE_SEARCH_SLOTS = 6

    /**
     * Splits [searchQuery] into exactly [FLEXIBLE_SEARCH_SLOTS] LIKE patterns for the
     * word-order-independent search queries.
     *
     * Real words become `%word%`; unused slots become `""`, which the queries' `= ''` guard
     * treats as "no condition". Words beyond the slot count are returned by [overflowSearchWords]
     * instead of being dropped, so the match never becomes *looser* than the original's.
     */
    fun flexibleLikeSlots(searchQuery: String): List<String> {
        val words = normalizeSearchWords(searchQuery).take(FLEXIBLE_SEARCH_SLOTS)
        return List(FLEXIBLE_SEARCH_SLOTS) { i -> words.getOrNull(i)?.let { createLikePattern(it) } ?: "" }
    }

    /**
     * The words that did not fit in [flexibleLikeSlots], to be applied in Kotlin via
     * [matchesOverflowWords].
     *
     * Empty for any query of [FLEXIBLE_SEARCH_SLOTS] words or fewer, which is every realistic
     * search — a seventh word only ever narrows an already near-empty result set.
     */
    fun overflowSearchWords(searchQuery: String): List<String> =
        normalizeSearchWords(searchQuery).drop(FLEXIBLE_SEARCH_SLOTS)

    /**
     * True when [name] contains every word in [overflow] (case-insensitive), matching the
     * `LOWER(name) LIKE '%word%'` the SQL slots would have applied.
     */
    fun matchesOverflowWords(name: String?, overflow: List<String>): Boolean {
        if (overflow.isEmpty()) return true
        val lowered = name?.lowercase() ?: return false
        return overflow.all { lowered.contains(it) }
    }
}
