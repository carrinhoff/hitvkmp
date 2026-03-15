package pt.hitv.core.model

data class PairingSession(
    val sessionId: String = "",
    val createdAt: Long = 0,
    val expiresAt: Long = 0,
    val status: String = "pending", // pending, completed, expired, login_success, login_failed, processing
    val credentials: PairingCredentials? = null,
    val completedAt: Long = 0,
    val updatedAt: Long = 0,
    val errorMessage: String? = null
)

data class PairingCredentials(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val playlistName: String = "",
    val type: String = "" // "xtream" or "m3u"
)
