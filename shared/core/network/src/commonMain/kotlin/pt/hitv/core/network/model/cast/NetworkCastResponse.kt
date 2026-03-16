package pt.hitv.core.network.model.cast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkCastResponse(
    @SerialName("id") val id: Int = 0,
    @SerialName("cast") val cast: List<NetworkCast>? = null,
    @SerialName("crew") val crew: List<NetworkCrew>? = null
)
