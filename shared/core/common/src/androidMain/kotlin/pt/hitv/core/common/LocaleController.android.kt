package pt.hitv.core.common

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android implementation of [LocaleController].
 *
 * Uses `AppCompatDelegate.setApplicationLocales` for hot-swap locale changes
 * (works on API 33+ natively and is backported via AppCompat below). [pendingRestart]
 * is always `false` because AppCompat handles activity re-creation internally.
 */
actual class LocaleController {

    private val _currentLocale = MutableStateFlow(readCurrentTag())
    actual val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    private val _pendingRestart = MutableStateFlow(false)
    actual val pendingRestart: StateFlow<Boolean> = _pendingRestart.asStateFlow()

    actual fun applyLocale(tag: String) {
        val sanitized = tag.trim()
        val localeList = if (sanitized.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(sanitized)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
        _currentLocale.value = readCurrentTag()
        _pendingRestart.value = false
    }

    private fun readCurrentTag(): String {
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return tags ?: ""
    }
}
