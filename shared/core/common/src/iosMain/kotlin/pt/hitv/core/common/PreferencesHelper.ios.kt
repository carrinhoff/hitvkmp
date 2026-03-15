package pt.hitv.core.common

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

/**
 * iOS actual implementation using NSUserDefaults for secure storage.
 * Note: For production, sensitive data should be stored in the iOS Keychain.
 * NSUserDefaults is used here as a baseline; Keychain integration can be
 * added via a KeychainSettings wrapper in the future.
 */
actual fun createEncryptedSettings(): Settings {
    return NSUserDefaultsSettings(NSUserDefaults(suiteName = "pt.hitv.secure"))
}
