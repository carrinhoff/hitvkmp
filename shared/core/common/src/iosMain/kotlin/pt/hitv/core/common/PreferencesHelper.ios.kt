package pt.hitv.core.common

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/** Keychain service name holding this app's sensitive values. */
private const val KEYCHAIN_SERVICE = "pt.hitv.secure"

/**
 * NSUserDefaults suite that previously held the sensitive values. Retained only so existing
 * installs can be migrated out of it — see [migrateLegacyPlaintextSecrets].
 */
private const val LEGACY_PLAINTEXT_SUITE = "pt.hitv.secure"

/**
 * iOS actual for [createEncryptedSettings], backed by the **iOS Keychain**.
 *
 * This used to return `NSUserDefaultsSettings(NSUserDefaults(suiteName = "pt.hitv.secure"))`,
 * i.e. plaintext. The keys routed here are `PreferencesHelper.SENSITIVE_KEYS` — `username`,
 * `password`, `hostUrl` and `parental_control_pin` — so on iOS the IPTV account password and
 * the parental-control PIN sat in a plist readable from any unencrypted device backup. Android
 * has always used `EncryptedSharedPreferences` for the same key set, so this was a platform
 * inconsistency as well as a weakness.
 *
 * `KeychainSettings` ships with multiplatform-settings and implements the same [Settings]
 * interface, so nothing above this function changes.
 */
@OptIn(ExperimentalSettingsApi::class)
actual fun createEncryptedSettings(): Settings {
    val keychain = KeychainSettings(service = KEYCHAIN_SERVICE)
    migrateLegacyPlaintextSecrets(into = keychain)
    return keychain
}

/**
 * One-time migration for installs that already wrote secrets to the plaintext NSUserDefaults
 * suite. Copies any surviving sensitive keys into the Keychain and then removes them from the
 * plist, so an upgrade neither logs the user out nor leaves the old plaintext copy behind.
 *
 * Only copies keys the Keychain doesn't already have, so it is safe to run on every launch and
 * never clobbers a fresher Keychain value. Best-effort: a failure here must not stop the app
 * from starting — worst case the user re-enters their credentials.
 */
private fun migrateLegacyPlaintextSecrets(into: Settings) {
    runCatching {
        val legacyDefaults = NSUserDefaults(suiteName = LEGACY_PLAINTEXT_SUITE)
        val legacy = NSUserDefaultsSettings(legacyDefaults)

        for (key in PreferencesHelper.SENSITIVE_KEYS) {
            if (!legacy.hasKey(key)) continue
            // Every sensitive key holds a String today; guard anyway so a type change upstream
            // degrades to "drop the legacy value" rather than throwing on launch.
            val legacyValue = legacy.getStringOrNull(key)
            if (legacyValue != null && !into.hasKey(key)) {
                into.putString(key, legacyValue)
            }
            legacy.remove(key)
        }
        legacyDefaults.synchronize()
    }
}
