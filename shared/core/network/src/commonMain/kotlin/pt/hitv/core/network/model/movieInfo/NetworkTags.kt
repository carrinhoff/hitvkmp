package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkTags(
    @SerialName("creation_time") val creationTime: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("handler_name") val handlerName: String? = null
)
