package pt.hitv.core.model

data class LoginResponse(
    val userInfo: UserInfo? = null,
    val serverInfo: ServerInfo
)
