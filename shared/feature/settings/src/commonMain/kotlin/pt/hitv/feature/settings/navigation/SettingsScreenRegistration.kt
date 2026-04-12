package pt.hitv.feature.settings.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

fun registerSettingsScreens() {
    ScreenRegistry.register(HitvScreen.MORE_OPTIONS) { MoreOptionsVoyagerScreen() }
}
