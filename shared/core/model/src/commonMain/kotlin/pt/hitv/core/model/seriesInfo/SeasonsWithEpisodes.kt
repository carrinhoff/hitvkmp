package pt.hitv.core.model.seriesInfo

data class SeasonsWithEpisodes(
    val seasonId: String,
    val airDate: String?,
    val episodeCount: String?,
    val name: String?,
    val overview: String?,
    val cover: String?,
    val coverBig: String?,
    val seriesId: String?,
    val episodeId: String,
    val episodeNum: String?,
    val containerExtension: String,
    val added: String?,
    val season: String?,
    val tmdbId: String?,
    val releaseDate: String?,
    val plot: String?,
    val durationSecs: Double?,
    val duration: String?,
    val bitrate: Double?,
    val rating: Double?,
    val title: String?,
    val movieImage: String?,
    val seasonNumber: Int,
    val playbackPosition: Long
)
