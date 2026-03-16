package pt.hitv.core.network.model.seriesInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pt.hitv.core.network.model.movieInfo.BackdropPathSerializer

@Serializable
data class NetworkSeriesInfo(
    @SerialName("name") var name: String? = null,
    @SerialName("cover") var cover: String? = null,
    @SerialName("plot") var plot: String? = null,
    @SerialName("cast") var cast: String? = null,
    @SerialName("director") var director: String? = null,
    @SerialName("genre") var genre: String? = null,
    @SerialName("releaseDate") var releaseDate: String? = null,
    @SerialName("last_modified") var lastModified: String? = null,
    @SerialName("rating") var rating: String? = null,
    @SerialName("rating_5based") var rating5based: Double? = null,
    @SerialName("backdrop_path")
    @Serializable(with = BackdropPathSerializer::class)
    var backdropPath: List<String> = emptyList(),
    @SerialName("youtube_trailer") var youtubeTrailer: String? = null,
    @SerialName("episode_run_time") var episodeRunTime: String? = null,
    @SerialName("category_id") var categoryId: String? = null
)
