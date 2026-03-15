package pt.hitv.core.common.crashreporting

/**
 * No-op implementation of CrashReportingHelper for testing or when crash reporting is disabled.
 */
class NoOpCrashReportingHelper : CrashReportingHelper {
    override fun recordException(throwable: Throwable, customKeys: Map<String, String>) {}
    override fun setCustomKey(key: String, value: String) {}
    override fun setCustomKey(key: String, value: Int) {}
    override fun setCustomKey(key: String, value: Boolean) {}
    override fun setCustomKey(key: String, value: Long) {}
    override fun setCustomKey(key: String, value: Double) {}
    override fun setCustomKey(key: String, value: Float) {}
    override fun setUserId(userId: String?) {}
    override fun log(message: String) {}
    override fun setCrashCollectionEnabled(enabled: Boolean) {}
}
