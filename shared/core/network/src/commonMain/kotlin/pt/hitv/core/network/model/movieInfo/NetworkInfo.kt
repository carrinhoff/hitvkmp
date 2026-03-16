package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkInfo(
    @SerialName("kinopoisk_url") val kinopoiskUrl: String? = "",
    @SerialName("tmdb_id") val tmdbId: String? = "",
    @SerialName("name") val name: String? = "",
    @SerialName("o_name") val oName: String? = "",
    @SerialName("cover_big") val coverBig: String? = "",
    @SerialName("movie_image") val movieImage: String? = "",
    @SerialName("releasedate") val releasedate: String? = "",
    @SerialName("episode_run_time") val episodeRunTime: String? = "",
    @SerialName("youtube_trailer") val youtubeTrailer: String? = "",
    @SerialName("director") val director: String? = "",
    @SerialName("actors") val actors: String? = "",
    @SerialName("cast") val cast: String? = "",
    @SerialName("description") val description: String? = "",
    @SerialName("plot") val plot: String? = "",
    @SerialName("age") val age: String? = "",
    @SerialName("mpaa_rating") val mpaaRating: String? = "",
    @SerialName("rating_count_kinopoisk") val ratingCountKinopoisk: String? = "",
    @SerialName("country") val country: String? = "",
    @SerialName("genre") val genre: String? = "",
    @SerialName("backdrop_path")
    @Serializable(with = BackdropPathSerializer::class)
    val backdropPath: List<String> = emptyList(),
    @SerialName("duration_secs") val durationSecs: String? = null,
    @SerialName("duration") val duration: String? = "",
    @SerialName("bitrate") val bitrate: String? = "",
    @SerialName("rating") val rating: String? = ""
)
