package pt.hitv.core.network.model.seriesInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkSeriesInfoResponse(
    @SerialName("seasons") var seasons: List<NetworkSeason>? = null,
    @SerialName("info") var info: NetworkSeriesInfo? = null,
    @SerialName("episodes") var episodes: Map<String, List<NetworkEpisode>>? = null
)
