package pt.hitv.core.model.movieInfo

data class Disposition(
    val default: Int,
    val dub: Int,
    val original: Int,
    val comment: Int,
    val lyrics: Int,
    val karaoke: Int,
    val forced: Int,
    val hearingImpaired: Int,
    val visualImpaired: Int,
    val cleanEffects: Int,
    val attachedPic: Int,
    val timedThumbnails: Int
)
