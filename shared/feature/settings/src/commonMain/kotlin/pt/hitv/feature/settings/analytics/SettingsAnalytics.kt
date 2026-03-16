package pt.hitv.feature.settings.analytics

/**
 * Analytics interface for the Settings feature module.
 */
interface SettingsAnalytics {
    fun logScreenView(screenName: String)
    fun logEvent(eventName: String, params: Map<String, Any>? = null)
    fun logThemeChange(themeName: String)
    fun logPurchaseAttempt(productId: String)
}

object NoOpSettingsAnalytics : SettingsAnalytics {
    override fun logScreenView(screenName: String) {}
    override fun logEvent(eventName: String, params: Map<String, Any>?) {}
    override fun logThemeChange(themeName: String) {}
    override fun logPurchaseAttempt(productId: String) {}
}

object SettingsScreenNames {
    const val OPTIONS = "options_screen"
    const val MORE_OPTIONS = "more_options_screen"
    const val THEME_SETTINGS = "theme_settings_screen"
    const val PARENTAL_CONTROL = "parental_control_screen"
    const val PREMIUM = "premium_screen"
    const val CUSTOM_GROUPS = "custom_groups_screen"
    const val FEEDBACK = "feedback_screen"
    const val CATEGORIES = "manage_categories_screen"
}
