package pt.hitv.feature.channels.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

fun registerChannelsScreens() {
    ScreenRegistry.register(HitvScreen.CHANNELS) { ChannelsVoyagerScreen() }
}
