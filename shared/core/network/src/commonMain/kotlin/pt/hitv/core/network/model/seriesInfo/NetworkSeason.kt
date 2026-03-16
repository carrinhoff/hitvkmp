package pt.hitv.core.network.model.seriesInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSeason(
    @SerialName("air_date") var airDate: String? = null,
    @SerialName("episode_count") var episodeCount: Int? = null,
    @SerialName("id") var id: String? = null,
    @SerialName("name") var name: String? = null,
    @SerialName("overview") var overview: String? = null,
    @SerialName("season_number") var seasonNumber: Int = 0,
    @SerialName("cover") var cover: String? = null,
    @SerialName("cover_big") var coverBig: String? = null
)
