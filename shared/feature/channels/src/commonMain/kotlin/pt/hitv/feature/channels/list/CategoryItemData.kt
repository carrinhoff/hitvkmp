package pt.hitv.feature.channels.list

/**
 * Data class for category items used in channel category lists.
 */
data class CategoryItemData(
    val id: String,
    val name: String,
    var count: String = "..."
)
