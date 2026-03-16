package pt.hitv.core.designsystem.theme

/**
 * Interface for providing premium theme ownership status.
 * This allows the design system module to check premium status
 * without depending on the billing module.
 *
 * Implementations should be provided by the app module via Koin.
 */
interface PremiumThemeStatusProvider {
    /**
     * Returns true if the user has access to premium themes.
     * This could be through:
     * - Premium themes bundle purchase
     * - Annual premium subscription
     * - Lifetime premium purchase
     */
    fun hasPremiumThemes(): Boolean
}
