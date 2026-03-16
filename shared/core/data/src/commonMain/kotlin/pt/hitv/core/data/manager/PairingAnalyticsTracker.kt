package pt.hitv.core.data.manager

/**
 * Interface for tracking pairing-related analytics events.
 * Implementations should be provided by the app module to decouple
 * the data layer from Firebase Analytics.
 */
interface PairingAnalyticsTracker {

    fun logCredentialsReceivedDetailed(
        sessionId: String,
        pairingType: String,
        url: String?,
        username: String?,
        password: String?,
        m3uUrl: String?
    )
}
