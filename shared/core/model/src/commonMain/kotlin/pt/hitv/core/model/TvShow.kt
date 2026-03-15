package pt.hitv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class TvShow(
    val num: Int?,
    val name: String?,
    val seriesId: Int,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5based: Double?,
    val backdropPath: List<String>?,
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String?,
    val isFavorite: Boolean = false,
    val lastViewedTimestamp: Long = 0L
)
