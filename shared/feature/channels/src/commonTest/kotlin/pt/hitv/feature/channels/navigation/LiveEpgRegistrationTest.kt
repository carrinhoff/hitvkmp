package pt.hitv.feature.channels.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The "Live with EPG" row in More Options pushes `HitvScreen.LIVE_EPG`, and
 * `ScreenRegistry.create` throws `IllegalStateException` for an unregistered route — which used
 * to kill the app straight from the main menu, and on iOS as an uncaught Kotlin/Native exception.
 *
 * The row is only shown because this route resolves, so this test guards the pairing: if the
 * registration is ever dropped while `MobileMoreOptionsScreen(showEpgEntry = true)` stays, the
 * crash comes back.
 */
class LiveEpgRegistrationTest {

    @Test
    fun `LIVE_EPG resolves to the EPG screen`() {
        registerChannelsScreens()

        // Throws if unregistered — that IS the production crash.
        val screen = ScreenRegistry.create(HitvScreen.LIVE_EPG)
        assertTrue(
            screen is LiveEpgVoyagerScreen,
            "expected LiveEpgVoyagerScreen, got ${screen::class.simpleName}",
        )
    }

    @Test
    fun `channels route still resolves alongside it`() {
        registerChannelsScreens()
        assertTrue(ScreenRegistry.create(HitvScreen.CHANNELS).key.isNotEmpty())
    }
}
