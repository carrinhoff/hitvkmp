package pt.hitv.core.model.seriesInfo

data class SeriesInfoResponse(
    val seasons: List<Season>? = emptyList(),
    val info: SeriesInfo?,
    val episodes: Map<String, List<Episode>>? = emptyMap()
)
