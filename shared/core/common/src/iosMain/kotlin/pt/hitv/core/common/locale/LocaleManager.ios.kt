package pt.hitv.core.common.locale

/**
 * iOS implementation of LocaleManager.
 * Uses NSBundle / NSLocale for locale management.
 */
actual class LocaleManager {

    actual fun getCurrentLanguageName(languageCode: String): String {
        return SupportedLanguage.fromCode(languageCode)?.nativeName ?: "System Default"
    }
}
