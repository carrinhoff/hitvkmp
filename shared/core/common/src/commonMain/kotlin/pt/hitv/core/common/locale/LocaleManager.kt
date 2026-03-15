package pt.hitv.core.common.locale

/**
 * Supported language definition with native and English names.
 */
data class Language(
    val code: String,
    val nativeName: String,
    val englishName: String
)

/**
 * Enum of all supported languages in the app.
 */
enum class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String
) {
    SYSTEM("system", "System Default", "System Default"),
    ENGLISH("en", "English", "English"),
    ARABIC("ar", "العربية", "Arabic"),
    BENGALI("bn", "বাংলা", "Bengali"),
    CHINESE("zh", "中文", "Chinese"),
    GERMAN("de", "Deutsch", "German"),
    GREEK("el", "Ελληνικά", "Greek"),
    SPANISH("es", "Español", "Spanish"),
    FRENCH("fr", "Français", "French"),
    HINDI("hi", "हिन्दी", "Hindi"),
    INDONESIAN("in", "Bahasa Indonesia", "Indonesian"),
    ITALIAN("it", "Italiano", "Italian"),
    JAPANESE("ja", "日本語", "Japanese"),
    POLISH("pl", "Polski", "Polish"),
    PORTUGUESE("pt", "Português", "Portuguese"),
    PORTUGUESE_BRAZIL("pt-rBR", "Português (Brasil)", "Portuguese (Brazil)"),
    PORTUGUESE_PORTUGAL("pt-rPT", "Português (Portugal)", "Portuguese (Portugal)"),
    RUSSIAN("ru", "Русский", "Russian"),
    TURKISH("tr", "Türkçe", "Turkish");

    fun toLanguage(): Language = Language(code, nativeName, englishName)

    companion object {
        /**
         * All supported languages as [Language] data objects.
         */
        val allLanguages: List<Language> = entries.map { it.toLanguage() }

        /**
         * Find a supported language by its code.
         */
        fun fromCode(code: String): SupportedLanguage? =
            entries.find { it.code == code }
    }
}

/**
 * Platform-specific locale manager.
 * - Android: Applies locale via Context configuration and can restart the Activity.
 * - iOS: Uses NSBundle / NSLocale for locale management.
 */
expect class LocaleManager {

    /**
     * Get the current language's native display name.
     * @param languageCode The language code to look up
     * @return The native name of the language, or "System Default" if not found
     */
    fun getCurrentLanguageName(languageCode: String): String
}
