package pt.hitv.core.model.seriesInfo

data class Episode(
    val id: String,
    val episodeNum: Int? = null,
    val title: String? = null,
    val containerExtension: String? = null,
    val info: EpisodeInfo,
    val customSid: String? = null,
    val added: String? = null,
    val season: Int? = null,
    val directSource: String? = null
)
