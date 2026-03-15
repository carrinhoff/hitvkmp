package pt.hitv.core.model.seriesInfo

data class SeriesInfo(
    val name: String? = null,
    val cover: String? = null,
    val plot: String? = null,
    val cast: String? = null,
    val director: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val lastModified: String? = null,
    val rating: String? = null,
    val rating5based: Double? = null,
    val backdropPath: List<String> = emptyList(),
    val youtubeTrailer: String? = null,
    val episodeRunTime: String? = null,
    val categoryId: String? = null
)
