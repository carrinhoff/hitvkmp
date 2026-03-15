package pt.hitv.core.common

import com.russhwolf.settings.Settings

/**
 * Platform-agnostic create function for encrypted/secure settings.
 * - Android: Uses EncryptedSharedPreferences
 * - iOS: Uses NSUserDefaults backed by Keychain
 */
expect fun createEncryptedSettings(): Settings

/**
 * KMP PreferencesHelper using multiplatform-settings.
 * Sensitive keys (username, password, hostUrl, parental_control_pin) are stored
 * in encrypted/secure storage via [createEncryptedSettings].
 * Non-sensitive keys use the regular [Settings] instance.
 */
class PreferencesHelper(
    private val settings: Settings,
    private val secureSettings: Settings = createEncryptedSettings()
) {

    private fun isSensitiveKey(key: String): Boolean = key in SENSITIVE_KEYS

    private fun settingsFor(key: String): Settings =
        if (isSensitiveKey(key)) secureSettings else settings

    fun getStoredTag(key: String): String {
        return settingsFor(key).getStringOrNull(key) ?: ""
    }

    fun setStoredTag(key: String, value: String) {
        settingsFor(key).putString(key, value)
    }

    fun getStoredIntTag(key: String): Int {
        return settings.getInt(key, -1)
    }

    fun setStoredIntTag(key: String, value: Int) {
        settings.putInt(key, value)
    }

    fun getStoredLongTag(key: String): Long {
        return try {
            settings.getLong(key, -1L)
        } catch (e: Exception) {
            settings.remove(key)
            -1L
        }
    }

    fun setStoredLongTag(key: String, value: Long) {
        settings.putLong(key, value)
    }

    fun getUsername(): String {
        return getStoredTag("username")
    }

    fun getPassword(): String {
        return getStoredTag("password")
    }

    fun getHostUrl(): String {
        val rawUrl = getStoredTag("hostUrl")
        if (rawUrl.isBlank()) return rawUrl
        return if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"
    }

    fun getExpirationDate(): String {
        return getStoredTag("expirationDate")
    }

    fun getUserId(): Int {
        return getStoredIntTag("userId")
    }

    fun deleteStoredFile() {
        settings.clear()
        secureSettings.clear()
    }

    fun getStoredBoolean(key: String): Boolean {
        return settings.getBoolean(key, false)
    }

    fun getStoredBoolean(key: String, defaultValue: Boolean): Boolean {
        return settings.getBoolean(key, defaultValue)
    }

    fun setStoredBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    fun getChannelPreviewSoundEnabled(): Boolean {
        return getStoredBoolean("channel_preview_sound_enabled", true)
    }

    fun setChannelPreviewSoundEnabled(enabled: Boolean) {
        setStoredBoolean("channel_preview_sound_enabled", enabled)
    }

    fun hasSeenTrialOffer(): Boolean {
        return getStoredBoolean("has_seen_trial_offer", false)
    }

    fun getAppLanguage(): String {
        return getStoredTag("app_language").ifEmpty { "system" }
    }

    fun setAppLanguage(languageCode: String) {
        setStoredTag("app_language", languageCode)
    }

    fun setTrialOfferShown() {
        setStoredBoolean("has_seen_trial_offer", true)
    }

    fun hasDeclinedTrialOffer(): Boolean {
        return getStoredBoolean("has_declined_trial_offer", false)
    }

    fun setTrialOfferDeclined() {
        setStoredBoolean("has_declined_trial_offer", true)
    }

    fun getParentalControlPin(): String {
        return getStoredTag("parental_control_pin")
    }

    fun setParentalControlPin(pin: String) {
        setStoredTag("parental_control_pin", pin)
    }

    fun hasParentalControlPin(): Boolean {
        return getParentalControlPin().isNotEmpty()
    }

    fun clearParentalControlPin() {
        setStoredTag("parental_control_pin", "")
    }

    fun isParentalControlEnabled(): Boolean {
        return getStoredBoolean("parental_control_enabled", false)
    }

    fun setParentalControlEnabled(enabled: Boolean) {
        setStoredBoolean("parental_control_enabled", enabled)
    }

    companion object {
        private val SENSITIVE_KEYS = setOf("username", "password", "hostUrl", "parental_control_pin")
    }
}
