package pt.hitv.core.model.enums

/**
 * Enum representing custom menu items for content filtering.
 * Used across Movies, Series, and Channels screens.
 *
 * Note: This enum is resource-independent. Use [getTitle] with a resolver
 * function to get localized strings in the UI layer.
 *
 * @property key The string key used for lookups and persistence
 */
enum class MenuCustomItem(val key: String) {
    FAVORITES("favorites"),
    RECENTLY_VIEWED("recently_viewed"),
    LAST_ADDED("last_added"),
    CONTINUE_WATCHING("continue_watching"),
    ALL("all");

    companion object {
        /**
         * Find a MenuCustomItem by its key.
         * @param key The string key to search for
         * @return The matching MenuCustomItem or null if not found
         */
        fun fromKey(key: String): MenuCustomItem? {
            return entries.find { it.key == key }
        }

        /**
         * Find a MenuCustomItem by matching a localized title.
         * @param title The localized title to match
         * @param titleResolver A function that returns the localized title for a MenuCustomItem
         * @return The matching MenuCustomItem or null if not found
         */
        fun fromTitle(title: String, titleResolver: (MenuCustomItem) -> String): MenuCustomItem? {
            return entries.find { titleResolver(it) == title }
        }
    }
}
