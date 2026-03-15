package pt.hitv.core.model.enums

/**
 * Represents the main navigation tabs in the application.
 *
 * This enum is resource-independent - it uses string keys that can be
 * mapped to actual string resources in the UI layer.
 *
 * @property id Unique identifier for the tab
 * @property titleKey String key for resolving the tab title (maps to R.string.*)
 * @property iconKey String key for resolving the tab icon
 */
enum class MainTab(
    val id: String,
    val titleKey: String,
    val iconKey: String
) {
    TV(
        id = "tv",
        titleKey = "live_tv",
        iconKey = "icon_tv"
    ),
    MOVIES(
        id = "movies",
        titleKey = "movies",
        iconKey = "icon_movie"
    ),
    SERIES(
        id = "series",
        titleKey = "tv_shows",
        iconKey = "icon_video_library"
    ),
    PREMIUM(
        id = "premium",
        titleKey = "premium_tab",
        iconKey = "icon_star"
    ),
    MORE(
        id = "more",
        titleKey = "options_more_label",
        iconKey = "icon_more_horiz"
    );

    companion object {
        /**
         * Finds a MainTab by its id.
         * @param id The tab id to search for
         * @return The matching MainTab, or null if not found
         */
        fun fromId(id: String): MainTab? = entries.find { it.id == id }

        /**
         * Returns all tabs except the specified ones.
         * Useful for filtering out tabs based on conditions (e.g., hiding Premium for subscribers).
         */
        fun entriesExcluding(vararg tabs: MainTab): List<MainTab> =
            entries.filter { it !in tabs }
    }
}
