package pt.hitv.core.common

import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform locale controller.
 *
 * - Android: delegates to `AppCompatDelegate.setApplicationLocales(...)` for a
 *   hot-swap. [pendingRestart] stays `false` on Android.
 * - iOS: writes the chosen tag into `AppleLanguages` NSUserDefaults and sets
 *   [pendingRestart] to `true` — the UI should prompt the user to reopen the app
 *   since most string resources on iOS only reload at launch.
 *
 * Tags follow BCP-47 language tag format (e.g. `"en"`, `"pt"`, `"pt-PT"`, `"es"`).
 */
expect class LocaleController() {

    /**
     * Currently-applied locale tag. Empty string means "system default".
     */
    val currentLocale: StateFlow<String>

    /**
     * True when the last [applyLocale] call requires an app restart to take full
     * effect (iOS). False on Android.
     */
    val pendingRestart: StateFlow<Boolean>

    /**
     * Apply the given locale [tag]. Empty or blank values reset to "system default".
     */
    fun applyLocale(tag: String)
}
