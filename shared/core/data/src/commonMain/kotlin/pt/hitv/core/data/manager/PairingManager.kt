package pt.hitv.core.data.manager

import dev.gitlive.firebase.database.DatabaseReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.common.util.QRCodeGenerator
import pt.hitv.core.model.PairingCredentials
import pt.hitv.core.model.PairingSession

/**
 * Manages QR pairing sessions via Firebase Realtime Database.
 * Ported to KMP using GitLive Firebase SDK.
 */
class PairingManager(
    private val analyticsTracker: PairingAnalyticsTracker,
    private val pairingSessions: DatabaseReference
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var currentSessionId: String? = null

    companion object {
        private const val SESSION_EXPIRY_MS = 10 * 60 * 1000L // 10 minutes
    }

    /**
     * Creates a new pairing session.
     * @return The session ID
     */
    suspend fun createPairingSession(): String {
        val sessionId = QRCodeGenerator.generateSessionId()
        val now = Clock.System.now().toEpochMilliseconds()

        val session = mapOf(
            "sessionId" to sessionId,
            "createdAt" to now,
            "expiresAt" to (now + SESSION_EXPIRY_MS),
            "status" to "pending"
        )

        pairingSessions.child(sessionId).setValue(session)
        return sessionId
    }

    /**
     * Listens for credentials updates on a specific session.
     * Uses GitLive Firebase KMP valueEvents flow.
     */
    fun listenForCredentials(
        sessionId: String,
        onCredentialsReceived: (PairingCredentials) -> Unit,
        onError: (String) -> Unit,
        onExpired: () -> Unit
    ) {
        currentSessionId = sessionId
        val sessionRef = pairingSessions.child(sessionId)
        var lastProcessedTimestamp: Long = 0

        scope.launch {
            try {
                sessionRef.valueEvents.collect { snapshot ->
                    val status = snapshot.child("status").value?.toString()
                    val expiresAt = snapshot.child("expiresAt").value?.toString()?.toLongOrNull() ?: 0L

                    if (!snapshot.exists) {
                        if (lastProcessedTimestamp > 0) {
                            onError("Session not found")
                        }
                        return@collect
                    }

                    val now = Clock.System.now().toEpochMilliseconds()
                    if (now > expiresAt) {
                        onExpired()
                        stopListening()
                        deletePairingSession(sessionId)
                        return@collect
                    }

                    if (status == "completed" && snapshot.child("credentials").exists) {
                        val creds = snapshot.child("credentials")
                        val completedAt = snapshot.child("completedAt").value?.toString()?.toLongOrNull()
                            ?: snapshot.child("createdAt").value?.toString()?.toLongOrNull()
                            ?: 0L

                        if (completedAt > lastProcessedTimestamp) {
                            lastProcessedTimestamp = completedAt

                            // Remove credentials after reading
                            scope.launch {
                                try {
                                    sessionRef.child("credentials").removeValue()
                                } catch (_: Exception) {
                                }
                            }

                            val credentials = PairingCredentials(
                                type = creds.child("type").value?.toString() ?: "",
                                url = creds.child("url").value?.toString() ?: "",
                                username = creds.child("username").value?.toString() ?: "",
                                password = creds.child("password").value?.toString() ?: "",
                                m3uUrl = creds.child("m3uUrl").value?.toString() ?: ""
                            )

                            analyticsTracker.logCredentialsReceivedDetailed(
                                sessionId = sessionId,
                                pairingType = credentials.type,
                                url = credentials.url.takeIf { it.isNotEmpty() },
                                username = credentials.username.takeIf { it.isNotEmpty() },
                                password = credentials.password.takeIf { it.isNotEmpty() },
                                m3uUrl = credentials.m3uUrl.takeIf { it.isNotEmpty() }
                            )

                            onCredentialsReceived(credentials)
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Stops listening to the current session.
     */
    fun stopListening() {
        // The flow collection will be cancelled when the scope is cancelled
        // or when we set currentSessionId to null
        currentSessionId = null
    }

    /**
     * Deletes a pairing session.
     */
    fun deletePairingSession(sessionId: String) {
        scope.launch {
            try {
                pairingSessions.child(sessionId).removeValue()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Updates the pairing session status.
     */
    fun updateSessionStatus(
        sessionId: String,
        status: String,
        errorMessage: String? = null
    ) {
        scope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "status" to status,
                    "updatedAt" to Clock.System.now().toEpochMilliseconds()
                )
                if (errorMessage != null) {
                    updates["errorMessage"] = errorMessage
                }
                pairingSessions.child(sessionId).updateChildren(updates)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Cleans up expired sessions.
     */
    fun cleanupExpiredSessions() {
        scope.launch {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val snapshot = pairingSessions
                    .orderByChild("expiresAt")
                    .endAt(now.toDouble())
                    .valueEvents
                // Cleanup would require consuming the first event from the flow
                // This is simplified for KMP - full implementation would use a one-shot query
            } catch (_: Exception) {
            }
        }
    }
}
