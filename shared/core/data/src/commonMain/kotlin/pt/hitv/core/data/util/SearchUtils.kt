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
}
