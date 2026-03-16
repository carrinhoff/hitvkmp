package pt.hitv.feature.settings.navigation

/**
 * Navigation interface for settings feature module.
 * Implemented by the app module to provide navigation to destinations
 * outside the settings feature.
 */
interface SettingsNavigator {
    fun navigateFromOptionsToMoreOptions()
    fun navigateFromOptionsToThemeSettings()
    fun navigateFromOptionsToSwitchAccount()
    fun navigateFromOptionsToLive()
    fun navigateFromOptionsToMovies()
    fun navigateFromOptionsToSeries()
    fun navigateFromOptionsToEpg()
    fun navigateFromOptionsToFeedback()
    fun navigateFromMoreOptionsToFeedback()
    fun navigateFromMoreOptionsToManageCategories()
    fun navigateFromMoreOptionsToParentalControl()
    fun navigateFromMoreOptionsToThemeSettings()
    fun navigateFromMoreOptionsToPremium()
    fun navigateFromMoreOptionsToSwitchAccount()
    fun navigateFromMoreOptionsToEpg()
    fun navigateFromMoreOptionsToLive()
    fun navigateFromMoreOptionsToMovies()
    fun navigateFromMoreOptionsToSeries()
    fun navigateFromMoreOptionsBack()
    fun navigateFromManageCategoriesBack()
    fun navigateFromParentalControlBack()
    fun navigateFromThemeSettingsToPremium()
    fun navigateFromThemeSettingsBack()
}
