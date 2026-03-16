package pt.hitv.feature.auth.qr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.model.PairingCredentials
import pt.hitv.core.data.manager.PairingManager
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.feature.auth.analytics.AuthAnalyticsTracker

/**
 * ViewModel for QR pairing flow.
 * Ported from Hilt to plain class for Koin injection.
 * Uses kotlinx.datetime.Clock instead of System.currentTimeMillis().
 * QR code generation uses expect/actual QRCodeGenerator.
 */
class QRPairingViewModel(
    private val pairingManager: PairingManager,
    private val preferencesHelper: PreferencesHelper,
    private val analyticsTracker: AuthAnalyticsTracker
) : ViewModel() {

    private val _sessionId = MutableStateFlow("")
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    /** QR code data as a byte array (PNG). Platform UI converts to image. */
    private val _qrCodeData = MutableStateFlow<ByteArray?>(null)
    val qrCodeData: StateFlow<ByteArray?> = _qrCodeData.asStateFlow()

    private val _pairingUrl = MutableStateFlow("")
    val pairingUrl: StateFlow<String> = _pairingUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _timeRemaining = MutableStateFlow(600) // 10 minutes in seconds
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _receivedCredentials = MutableStateFlow<PairingCredentials?>(null)
    val receivedCredentials: StateFlow<PairingCredentials?> = _receivedCredentials.asStateFlow()

    private val _pairingSuccessful = MutableSharedFlow<Boolean>()
    val pairingSuccessful: SharedFlow<Boolean> = _pairingSuccessful.asSharedFlow()

    private var timerJob: Job? = null
    private var pairingStartTime: Long = 0
    private var retryCount: Int = 0
    private var screenViewTime: Long = 0

    companion object {
        private const val WEB_APP_BASE_URL = "https://iptv-hitv.web.app/pair"
        private var lastPairingInitTime: Long = 0
        private const val MIN_PAIRING_INTERVAL_MS = 30000L
    }

    private fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun initializePairing() {
        val now = currentTimeMillis()
        if (now - lastPairingInitTime < MIN_PAIRING_INTERVAL_MS) {
            _errorMessage.value =
                "Please wait ${MIN_PAIRING_INTERVAL_MS / 1000} seconds before generating another QR code."
            return
        }
        lastPairingInitTime = now
        pairingStartTime = now
        screenViewTime = now
        retryCount = 0
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val newSessionId = pairingManager.createPairingSession()
                _sessionId.value = newSessionId

                analyticsTracker.logQRPairingInitiated(newSessionId, "tv")
                analyticsTracker.logQRScreenViewed(newSessionId)

                val pairingUrlStr = "$WEB_APP_BASE_URL?session=$newSessionId"
                _pairingUrl.value = pairingUrlStr

                // QR code generation is handled by the platform UI layer
                // The pairingUrl is exposed for the UI to generate the QR code

                analyticsTracker.logQRCodeGenerated(newSessionId)
                analyticsTracker.logQRWaitingForScan(newSessionId)

                pairingManager.listenForCredentials(
                    sessionId = newSessionId,
                    onCredentialsReceived = { credentials ->
                        handleCredentialsReceived(credentials)
                    },
                    onError = { error ->
                        _errorMessage.value = error
                        _isLoading.value = false
                    },
                    onExpired = {
                        analyticsTracker.logQRPairingTimeout(_sessionId.value)
                        _errorMessage.value = "Session expired. Please try again."
                        _isLoading.value = false
                    }
                )

                startTimer()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to initialize pairing: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _timeRemaining.value = 600

        timerJob = viewModelScope.launch {
            while (_timeRemaining.value > 0) {
                delay(1000)
                _timeRemaining.value = _timeRemaining.value - 1
            }
            analyticsTracker.logQRPairingTimeout(_sessionId.value)
            _errorMessage.value = "Time expired. Please generate a new code."
            pairingManager.stopListening()
            _sessionId.value.let { sessionId ->
                if (sessionId.isNotEmpty()) {
                    pairingManager.deletePairingSession(sessionId)
                }
            }
        }
    }

    private fun handleCredentialsReceived(credentials: PairingCredentials) {
        viewModelScope.launch {
            try {
                analyticsTracker.logCredentialsReceived(_sessionId.value, credentials.type)
                _receivedCredentials.value = credentials
                _pairingSuccessful.emit(true)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to process credentials: ${e.message}"
            }
        }
    }

    fun cleanup() {
        timerJob?.cancel()
        pairingManager.stopListening()
        _sessionId.value.let { sessionId ->
            if (sessionId.isNotEmpty()) {
                analyticsTracker.logQRPairingCancelled(sessionId)
                analyticsTracker.logQRCancelButtonClicked(
                    sessionId,
                    currentTimeMillis() - screenViewTime
                )
                pairingManager.deletePairingSession(sessionId)
            }
        }
    }

    fun updateSessionStatus(status: String, errorMessage: String? = null) {
        _sessionId.value.let { sessionId ->
            if (sessionId.isNotEmpty()) {
                pairingManager.updateSessionStatus(sessionId, status, errorMessage)
            }
        }
    }

    fun getCurrentSessionId(): String = _sessionId.value

    fun onPairingStatusChanged(success: Boolean, errorMessage: String? = null) {
        val sessionId = _sessionId.value
        if (sessionId.isNotEmpty()) {
            val credentials = _receivedCredentials.value
            val pairingType = credentials?.type ?: "unknown"
            val duration = currentTimeMillis() - pairingStartTime

            if (success) {
                updateSessionStatus("login_success")
                analyticsTracker.logQRPairingSuccess(sessionId, pairingType, duration)
            } else {
                updateSessionStatus("login_failed", errorMessage ?: "Login failed")
                retryCount++
                analyticsTracker.logQRPairingFailed(
                    sessionId = sessionId,
                    pairingType = pairingType,
                    errorMessage = errorMessage ?: "Login failed",
                    retryCount = retryCount
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanup()
    }
}
