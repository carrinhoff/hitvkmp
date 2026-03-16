package pt.hitv.core.network.model.seriesInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEpisode(
    @SerialName("id") val id: String? = null,
    @SerialName("episode_num") val episodeNum: Int? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("container_extension") val containerExtension: String? = null,
    @SerialName("info") val info: NetworkEpisodeInfo? = null,
    @SerialName("custom_sid") val customSid: String? = null,
    @SerialName("added") val added: String? = null,
    @SerialName("season") val season: Int? = null,
    @SerialName("direct_source") val directSource: String? = null
)
