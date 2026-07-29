package pt.hitv.feature.channels.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

fun registerChannelsScreens() {
    ScreenRegistry.register(HitvScreen.CHANNELS) { ChannelsVoyagerScreen() }
    // The EPG grid lives in this module (it needs StreamViewModel), so its route is registered
    // here rather than in the settings module that hosts the menu row pointing at it.
    ScreenRegistry.register(HitvScreen.LIVE_EPG) { LiveEpgVoyagerScreen() }
}
