package pt.hitv.feature.premium.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

fun registerPremiumScreens() {
    ScreenRegistry.register(HitvScreen.PREMIUM_SUBSCRIPTION) { PremiumVoyagerScreen() }
}
