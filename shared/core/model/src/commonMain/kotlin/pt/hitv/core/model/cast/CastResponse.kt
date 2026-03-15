package pt.hitv.core.model.cast

data class CastResponse(
    val id: Int,
    val cast: List<Cast>,
    val crew: List<Crew>
)
