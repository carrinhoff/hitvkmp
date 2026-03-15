package pt.hitv.core.model

data class UserInfo(
    val username: String,
    val password: String,
    val message: String,
    val auth: Int,
    val status: String,
    val expDate: String?,
    val isTrial: Int,
    val activeCons: Int,
    val createdAt: Int,
    val maxConnections: Int,
    val allowedOutputFormats: List<String>
)
