package pt.hitv.core.domain.manager

import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.ParentalControl

/**
 * Interface for parental control operations.
 *
 * Provides centralized access to PIN validation, category protection,
 * and session management to reduce PIN entry annoyance.
 *
 * Implementations should handle:
 * - PIN setup, validation, and clearing
 * - Session-based authentication with configurable timeout
 * - Per-category protection management
 * - Premium subscription gating
 */
interface ParentalControlManager {

    /**
     * Check if parental control is enabled globally.
     * Requires both local settings to be enabled AND an active premium subscription.
     */
    fun isParentalControlEnabled(): Boolean

    /**
     * Check if current session is still authenticated (within timeout period).
     * This prevents users from having to re-enter PIN constantly.
     */
    fun isSessionAuthenticated(): Boolean

    /**
     * Set session timeout in minutes.
     */
    fun setSessionTimeout(minutes: Int)

    /**
     * Get session timeout in minutes.
     * Returns -1 for "until app closes", -2 for "always ask", or the configured timeout, or default (30 min).
     */
    fun getSessionTimeoutMinutes(): Int

    /**
     * Validate if the provided PIN matches the stored PIN.
     * On successful validation, updates the session unlock time.
     */
    fun validatePin(pin: String): Boolean

    /**
     * Manually end the current session (e.g., when user logs out or locks the app).
     */
    fun endSession()

    /**
     * Clear session if timeout is set to "until app closes".
     * This should be called when the app starts to ensure session doesn't persist across app restarts.
     */
    fun clearSessionOnAppStart()

    /**
     * Get remaining session time in minutes.
     * Returns -2 for "always ask", -1 for "until app closes" (unlimited session).
     */
    fun getRemainingSessionTime(): Long

    /**
     * Set or update the parental control PIN.
     */
    fun setPin(pin: String)

    /**
     * Clear the parental control PIN and disable parental control.
     */
    fun clearPin()

    /**
     * Check if a specific category is protected.
     */
    suspend fun isCategoryProtected(categoryId: Int, userId: Int): Boolean

    /**
     * Get list of all protected category IDs for a user.
     * Returns empty list if parental control is disabled or session is authenticated.
     */
    suspend fun getProtectedCategoryIds(userId: Int): List<String>

    /**
     * Set protection status for a category.
     */
    suspend fun setCategoryProtection(categoryId: Int, categoryName: String, userId: Int, isProtected: Boolean)

    /**
     * Get all parental controls for a user.
     */
    fun getAllParentalControls(userId: Int): Flow<List<ParentalControl>>

    /**
     * Get parental control for a specific category.
     */
    fun getParentalControlByCategory(categoryId: Int, userId: Int): Flow<ParentalControl?>

    /**
     * Get count of protected categories.
     */
    fun getProtectedCategoriesCount(userId: Int): Flow<Int>

    /**
     * Remove protection from a category.
     */
    suspend fun removeProtection(categoryId: Int, userId: Int)

    /**
     * Delete all parental controls for a user.
     */
    suspend fun deleteAllParentalControls(userId: Int)

    /**
     * Check if PIN is required to access a category.
     * Returns false if session is still authenticated.
     */
    suspend fun requiresPinForCategory(categoryId: Long, userId: Int): Boolean
}
