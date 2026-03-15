package pt.hitv.core.model

/**
 * Enum representing the type of content
 */
enum class ContentType {
    CHANNELS,
    MOVIES,
    SERIES
}

/**
 * UI model representing a category with its pin/hide preferences.
 * This unifies EntityCategory, EntityCategoryVod, and EntityCategoryTvShow
 * into a single model for the ManageCategories UI.
 */
data class CategoryPreference(
    val categoryId: String,
    val categoryName: String,
    val contentType: ContentType,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isDefault: Boolean = false
)
