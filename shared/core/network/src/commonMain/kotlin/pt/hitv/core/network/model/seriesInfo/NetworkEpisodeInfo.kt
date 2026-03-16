package pt.hitv.core.network.model.seriesInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkEpisodeInfo(
    @SerialName("tmdb_id") val tmdbId: Double? = null,
    @SerialName("releasedate") val releasedate: String? = null,
    @SerialName("plot") val plot: String? = null,
    @SerialName("duration_secs") val durationSecs: Double? = null,
    @SerialName("duration") val duration: String? = null,
    @SerialName("movie_image") val movieImage: String? = null,
    @SerialName("bitrate") val bitrate: Double? = null,
    @SerialName("rating") val rating: Double? = null,
    @SerialName("season") val season: String? = null
)
