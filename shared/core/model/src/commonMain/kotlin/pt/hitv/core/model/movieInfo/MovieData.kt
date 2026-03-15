package pt.hitv.core.model.movieInfo

data class MovieData(
    val streamId: Int,
    val name: String,
    val added: Double,
    val categoryId: Int,
    val containerExtension: String,
    val customSid: String?,
    val directSource: String?
)
