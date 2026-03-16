package pt.hitv.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkMovie(
    @SerialName("movieId") val movieId: Long = 0L,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_id") val streamId: String? = null,
    @SerialName("stream_icon") val streamIcon: String? = "",
    @SerialName("rating") val rating: String? = "",
    @SerialName("added") val added: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("custom_sid") val customSid: String? = "",
    @SerialName("direct_source") val directSource: String? = "",
    @SerialName("num") val num: String? = null,
    @SerialName("stream_type") val streamType: String? = "",
    @SerialName("rating_5based") val rating5based: Double? = 0.0
)
