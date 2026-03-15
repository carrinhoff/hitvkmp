package pt.hitv.core.common.crashreporting

/**
 * Interface for crash reporting that abstracts away the specific implementation.
 * Feature modules can use this interface without depending on Firebase Crashlytics directly.
 *
 * Implemented by platform-specific modules (Firebase Crashlytics on Android, etc.).
 */
interface CrashReportingHelper {

    /**
     * Record a non-fatal exception for crash reporting.
     *
     * @param throwable The exception to record
     * @param customKeys Optional map of custom key-value pairs for context
     */
    fun recordException(throwable: Throwable, customKeys: Map<String, String> = emptyMap())

    /**
     * Set a custom key-value pair that will be included in crash reports.
     */
    fun setCustomKey(key: String, value: String)
    fun setCustomKey(key: String, value: Int)
    fun setCustomKey(key: String, value: Boolean)
    fun setCustomKey(key: String, value: Long)
    fun setCustomKey(key: String, value: Double)
    fun setCustomKey(key: String, value: Float)

    /**
     * Set the user identifier for crash reports.
     *
     * @param userId The user identifier (should not contain PII)
     */
    fun setUserId(userId: String?)

    /**
     * Log a message that will be included in the next crash report.
     *
     * @param message The log message
     */
    fun log(message: String)

    /**
     * Enable or disable crash collection.
     *
     * @param enabled Whether crash collection should be enabled
     */
    fun setCrashCollectionEnabled(enabled: Boolean)

    companion object {
        // Common custom key names
        const val KEY_SCREEN_NAME = "screen_name"
        const val KEY_CONTENT_TYPE = "content_type"
        const val KEY_CONTENT_ID = "content_id"
        const val KEY_ERROR_TYPE = "error_type"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_LOCATION = "location"
        const val KEY_USER_ACTION = "user_action"
        const val KEY_DATA_SOURCE = "data_source"
    }
}
