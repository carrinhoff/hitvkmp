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
    ScreenRegistry.register(HitvScreen.FEEDBACK) { FeedbackVoyagerScreen() }
    // HitvScreen.LIVE_EPG is deliberately NOT registered: the full EPG grid
    // (the original's EpgScreenMobile) has not been ported yet, so there is no screen to
    // create. The More Options row that navigated here is hidden behind
    // MobileMoreOptionsScreen's `showEpgEntry` flag for the same reason — re-enable both
    // together. See KMP_MIGRATION_AUDIT.md P0 #8.
}
