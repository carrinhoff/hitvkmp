package pt.hitv.feature.auth.analytics

/**
 * Interface for tracking auth-related analytics events.
 * Implementations should be provided by the app module to decouple
 * the feature module from Firebase Analytics.
 */
interface AuthAnalyticsTracker {

    // QR Pairing analytics
    fun logQRPairingInitiated(sessionId: String, source: String)
    fun logQRScreenViewed(sessionId: String)
    fun logQRCodeGenerated(sessionId: String)
    fun logQRWaitingForScan(sessionId: String)
    fun logQRPairingTimeout(sessionId: String)
    fun logQRPairingCancelled(sessionId: String)
    fun logQRCancelButtonClicked(sessionId: String, timeOnScreen: Long)
    fun logCredentialsReceived(sessionId: String, pairingType: String)
    fun logQRPairingSuccess(sessionId: String, pairingType: String, duration: Long)
    fun logQRPairingFailed(
        sessionId: String,
        pairingType: String,
        errorMessage: String,
        retryCount: Int
    )

    // Login analytics
    fun logScreenView(screenName: String, screenClass: String)
    fun logLogin(method: String)

    // Switch account analytics
    fun logSwitchAccount(userId: String, hostname: String)
    fun logAddAccountClicked()
    fun logDeleteAccountIntent(userId: String, hostname: String)
    fun logDeleteAccountConfirmed(userId: String)

    // User identification
    fun setUserId(userId: String?)
}
