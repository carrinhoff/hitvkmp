package pt.hitv.core.model.seriesInfo

data class EpisodeInfo(
    val tmdbId: Double?,
    val releasedate: String? = null,
    val plot: String? = null,
    val durationSecs: Double? = null,
    val duration: String? = null,
    val movieImage: String? = null,
    val bitrate: Double? = null,
    val rating: Double? = null,
    val season: String? = null,
    val playbackPosition: Long = 0L
)
