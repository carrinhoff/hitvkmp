package pt.hitv.android.analytics

import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.data.manager.PairingAnalyticsTracker

/**
 * Firebase-backed implementation of PairingAnalyticsTracker.
 * Bridges the core data module's pairing analytics to the shared AnalyticsHelper.
 */
class FirebasePairingAnalyticsTrackerImpl(
    private val analyticsHelper: AnalyticsHelper
) : PairingAnalyticsTracker {

    override fun logCredentialsReceivedDetailed(
        sessionId: String,
        pairingType: String,
        url: String?,
        username: String?,
        password: String?,
        m3uUrl: String?
    ) {
        analyticsHelper.logCustomEvent(
            eventName = "credentials_received_detailed",
            params = buildMap {
                put("session_id", sessionId)
                put("pairing_type", pairingType)
                url?.let { put("server_url", it) }
                username?.let { put("has_username", (it.isNotEmpty()).toString()) }
                password?.let { put("has_password", (it.isNotEmpty()).toString()) }
                m3uUrl?.let { put("has_m3u_url", (it.isNotEmpty()).toString()) }
            }
        )
    }
}
