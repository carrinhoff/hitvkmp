package pt.hitv.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkLoginResponse(
    @SerialName("user_info") var userInfo: NetworkUserInfo? = null,
    @SerialName("server_info") var serverInfo: NetworkServerInfo? = null
)
