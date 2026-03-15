package pt.hitv.core.domain.repositories

import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.CategoryPreference
import pt.hitv.core.model.ContentType

/**
 * Repository for managing category preferences (pin, hide, default states).
 * Abstracts the underlying data sources (channel, movie, series categories and custom groups).
 */
interface CategoryPreferenceRepository {

    /**
     * Get all category preferences as a Flow, combining channels, movies, series, and custom groups.
     */
    fun getAllCategoryPreferences(): Flow<List<CategoryPreference>>

    /**
     * Update the pin status for a category.
     * @param categoryId The category ID (or "custom_group_{id}" for custom groups)
     * @param contentType The content type (CHANNELS, MOVIES, SERIES)
     * @param isPinned The new pin status
     */
    suspend fun updateCategoryPinStatus(categoryId: String, contentType: ContentType, isPinned: Boolean)

    /**
     * Update the hide status for a category.
     * @param categoryId The category ID (or "custom_group_{id}" for custom groups)
     * @param contentType The content type (CHANNELS, MOVIES, SERIES)
     * @param isHidden The new hide status
     */
    suspend fun updateCategoryHideStatus(categoryId: String, contentType: ContentType, isHidden: Boolean)

    /**
     * Update hide status for all categories of a content type.
     */
    suspend fun updateAllCategoriesHideStatus(contentType: ContentType, isHidden: Boolean)

    /**
     * Update pin status for all categories of a content type.
     */
    suspend fun updateAllCategoriesPinStatus(contentType: ContentType, isPinned: Boolean)

    /**
     * Set a category as the default for its content type.
     * Automatically clears previous default.
     * @param categoryId The category ID (or "custom_group_{id}" for custom groups)
     * @param contentType The content type (CHANNELS, MOVIES, SERIES)
     */
    suspend fun setDefaultCategory(categoryId: String, contentType: ContentType)

    /**
     * Clear the default category for a content type.
     */
    suspend fun clearDefaultCategory(contentType: ContentType)

    /**
     * Reset all preferences (pin and hide) for all content types.
     */
    suspend fun resetAllPreferences()
}
