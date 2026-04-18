package pt.hitv.feature.settings.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import pt.hitv.feature.settings.options.options.about.AboutScreen
import pt.hitv.feature.settings.options.options.categories.ManageCategoriesVoyagerScreen
import pt.hitv.feature.settings.options.options.sync.BackgroundSyncSettingsVoyagerScreen
import pt.hitv.feature.settings.options.options.tips.TipsAndFeaturesScreen

fun registerSettingsScreens() {
    ScreenRegistry.register(HitvScreen.MORE_OPTIONS) { MoreOptionsVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.TIPS_AND_FEATURES) { TipsAndFeaturesScreen() }
    ScreenRegistry.register(HitvScreen.ABOUT) { AboutScreen() }
    ScreenRegistry.register(HitvScreen.THEME_SETTINGS) { ThemeStudioVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.PARENTAL_CONTROL) { ParentalControlVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.PARENTAL_PIN_SETUP) { ParentalPinSetupVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.PARENTAL_CATEGORY_LOCK) { ParentalCategoryLockVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.MANAGE_CATEGORIES) { ManageCategoriesVoyagerScreen() }
    ScreenRegistry.register(HitvScreen.BACKGROUND_SYNC_SETTINGS) { BackgroundSyncSettingsVoyagerScreen() }
}
