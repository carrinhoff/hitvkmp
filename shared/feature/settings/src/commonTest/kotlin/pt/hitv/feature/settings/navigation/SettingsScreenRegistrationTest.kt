package pt.hitv.feature.settings.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Regression guard for the class of bug that shipped two reachable crashes.
 *
 * `ScreenRegistry.create` ends in `?: error("No screen factory registered for $screen")`, so a
 * `HitvScreen` that some `Navigator.navigateToX()` pushes but nobody registers is an
 * `IllegalStateException` the moment the user taps the row — on iOS an uncaught Kotlin/Native
 * exception that kills the process. `HitvScreen.FEEDBACK` and `HitvScreen.LIVE_EPG` were both in
 * that state, reachable straight from the More Options list.
 *
 * These tests assert the registry contents directly, so the failure surfaces here rather than in
 * a manual tap-through of every menu row.
 */
class SettingsScreenRegistrationTest {

    @BeforeTest
    fun setUp() {
        registerSettingsScreens()
    }

    /**
     * Every settings screen More Options can navigate to must resolve. FEEDBACK is the one that
     * used to crash; the rest guard against the same regression elsewhere in the menu.
     */
    @Test
    fun `all settings screens reachable from More Options are registered`() {
        val required = listOf(
            HitvScreen.MORE_OPTIONS,
            HitvScreen.THEME_SETTINGS,
            HitvScreen.PARENTAL_CONTROL,
            HitvScreen.PARENTAL_PIN_SETUP,
            HitvScreen.PARENTAL_CATEGORY_LOCK,
            HitvScreen.MANAGE_CATEGORIES,
            HitvScreen.BACKGROUND_SYNC_SETTINGS,
            HitvScreen.TIPS_AND_FEATURES,
            HitvScreen.ABOUT,
            HitvScreen.FEEDBACK,
        )
        for (screen in required) {
            // Throws IllegalStateException if unregistered — that IS the production crash.
            val created = ScreenRegistry.create(screen)
            assertTrue(created.key.isNotEmpty(), "$screen produced a screen with no key")
        }
    }

    @Test
    fun `FEEDBACK resolves to the feedback screen`() {
        val screen = ScreenRegistry.create(HitvScreen.FEEDBACK)
        assertTrue(
            screen is FeedbackVoyagerScreen,
            "expected FeedbackVoyagerScreen, got ${screen::class.simpleName}",
        )
    }

    /**
     * LIVE_EPG is registered by `registerChannelsScreens()` in the channels module, not by
     * `registerSettingsScreens()` — the grid needs `StreamViewModel`. This test therefore asserts
     * it is *absent* here, which documents the ownership split and keeps the two registration
     * functions from silently double-registering the same route.
     *
     * The user-facing guarantee — that the "Live with EPG" row resolves rather than crashing —
     * is covered by `LiveEpgRegistrationTest` in the channels module, which registers both.
     */
    @Test
    fun `LIVE_EPG is not owned by the settings module`() {
        assertFailsWith<IllegalStateException> {
            ScreenRegistry.create(HitvScreen.LIVE_EPG)
        }
    }
}
