package pt.hitv.core.model.movieInfo

data class Info(
    val kinopoiskUrl: String? = "",
    val tmdbId: String? = "",
    val name: String? = "",
    val oName: String? = "",
    val coverBig: String? = "",
    val movieImage: String? = "",
    val releasedate: String? = "",
    val episodeRunTime: String? = "",
    val youtubeTrailer: String? = "",
    val director: String? = "",
    val actors: String? = "",
    val cast: String? = "",
    val description: String? = "",
    val plot: String? = "",
    val age: String? = "",
    val mpaaRating: String? = "",
    val ratingCountKinopoisk: String? = "",
    val country: String? = "",
    val genre: String? = "",
    val backdropPath: List<String>? = emptyList(),
    val durationSecs: String?,
    val duration: String? = "",
    val bitrate: String? = "",
    val rating: String? = ""
)
