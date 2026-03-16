package pt.hitv.android.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.crashlytics.crashlytics
import pt.hitv.core.common.crashreporting.CrashReportingHelper

/**
 * Firebase Crashlytics implementation of CrashReportingHelper
 * using GitLive Firebase KMP SDK.
 */
class FirebaseCrashReportingHelperImpl : CrashReportingHelper {

    private val crashlytics = Firebase.crashlytics

    override fun recordException(throwable: Throwable, customKeys: Map<String, String>) {
        customKeys.forEach { (key, value) ->
            crashlytics.setCustomKey(key, value)
        }
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    override fun setCustomKey(key: String, value: Int) {
        crashlytics.setCustomKey(key, value.toString())
    }

    override fun setCustomKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value.toString())
    }

    override fun setCustomKey(key: String, value: Long) {
        crashlytics.setCustomKey(key, value.toString())
    }

    override fun setCustomKey(key: String, value: Double) {
        crashlytics.setCustomKey(key, value.toString())
    }

    override fun setCustomKey(key: String, value: Float) {
        crashlytics.setCustomKey(key, value.toString())
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId ?: "")
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun setCrashCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }
}
