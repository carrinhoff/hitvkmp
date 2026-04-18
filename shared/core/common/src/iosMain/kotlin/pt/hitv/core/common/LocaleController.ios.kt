package pt.hitv.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of [LocaleController].
 *
 * Writes the chosen tag into the `AppleLanguages` key of `NSUserDefaults.standard`
 * (UIKit's documented mechanism for forcing app language). This only takes effect
 * on the NEXT launch, so [pendingRestart] is set to `true` after every apply —
 * the UI should display a "please reopen the app" banner.
 */
actual class LocaleController {

    private val defaults = NSUserDefaults.standardUserDefaults

    private val _currentLocale = MutableStateFlow(readCurrentTag())
    actual val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    private val _pendingRestart = MutableStateFlow(false)
    actual val pendingRestart: StateFlow<Boolean> = _pendingRestart.asStateFlow()

    actual fun applyLocale(tag: String) {
        val sanitized = tag.trim()
        if (sanitized.isBlank()) {
            defaults.removeObjectForKey(KEY_APPLE_LANGUAGES)
        } else {
            defaults.setObject(listOf(sanitized), forKey = KEY_APPLE_LANGUAGES)
        }
        defaults.synchronize()
        _currentLocale.value = sanitized
        _pendingRestart.value = true
    }

    @Suppress("UNCHECKED_CAST")
    private fun readCurrentTag(): String {
        val raw = defaults.arrayForKey(KEY_APPLE_LANGUAGES) as? List<Any?>
        return (raw?.firstOrNull() as? String) ?: ""
    }

    private companion object {
        const val KEY_APPLE_LANGUAGES = "AppleLanguages"
    }
}
