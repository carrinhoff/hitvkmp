package pt.hitv.core.designsystem.model

// Sort constants (mirrors core:data values for module independence)
const val SORT_ADDED = "added"
const val SORT_NAME = "name"
const val SORT_RATING = "rating"

/**
 * Shared data model for sorting options across TV and Mobile.
 */
data class SortOption(
    val id: String,
    val label: String,
    val supportsDirection: Boolean = true
)

/**
 * The unified list of sort options used by both Mobile and TV layouts.
 * This is the single source of truth for sorting.
 * Labels are passed as parameters since we don't have Android string resources in commonMain.
 */
fun createUnifiedSortOptions(
    addedLabel: String = "Recently Added",
    nameLabel: String = "Name",
    ratingLabel: String = "Rating"
): List<SortOption> = listOf(
    SortOption(SORT_ADDED, addedLabel),
    SortOption(SORT_NAME, nameLabel),
    SortOption(SORT_RATING, ratingLabel),
)
