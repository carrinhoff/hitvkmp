package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkMovieData(
    @SerialName("stream_id") val streamId: Int = 0,
    @SerialName("name") val name: String? = null,
    @SerialName("added") val added: Double = 0.0,
    @SerialName("category_id") val categoryId: Int = 0,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("direct_source") val directSource: String? = null
)
