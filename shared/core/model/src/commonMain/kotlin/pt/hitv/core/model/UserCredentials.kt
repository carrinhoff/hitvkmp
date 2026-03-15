package pt.hitv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserCredentials(
    val userId: Int,
    val username: String,
    val password: String, // is encrypted
    val hostname: String,
    val expirationDate: String? = null,
    val epgUrl: String? = null,
    val allowedOutputFormats: List<String>? = null,
    val channelPreviewEnabled: Boolean = true // Default to true for existing users
)
