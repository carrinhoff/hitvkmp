package pt.hitv.core.model.seriesInfo

data class Season(
    val airDate: String? = null,
    val episodeCount: Int? = null,
    val id: String,
    val name: String? = null,
    val overview: String? = null,
    val seasonNumber: Int,
    val cover: String? = null,
    val coverBig: String? = null
)
