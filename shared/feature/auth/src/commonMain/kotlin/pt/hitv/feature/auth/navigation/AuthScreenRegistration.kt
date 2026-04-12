package pt.hitv.feature.auth.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

fun registerAuthScreens() {
    ScreenRegistry.register(HitvScreen.LOGIN) { args ->
        val onLoginSuccess = args as? (() -> Unit) ?: {}
        LoginVoyagerScreen(onLoginSuccess)
    }
}
