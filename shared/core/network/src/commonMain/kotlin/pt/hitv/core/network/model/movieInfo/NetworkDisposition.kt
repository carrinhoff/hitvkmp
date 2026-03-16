package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkDisposition(
    @SerialName("default") val default_: Int = 0,
    @SerialName("dub") val dub: Int = 0,
    @SerialName("original") val original: Int = 0,
    @SerialName("comment") val comment: Int = 0,
    @SerialName("lyrics") val lyrics: Int = 0,
    @SerialName("karaoke") val karaoke: Int = 0,
    @SerialName("forced") val forced: Int = 0,
    @SerialName("hearing_impaired") val hearingImpaired: Int = 0,
    @SerialName("visual_impaired") val visualImpaired: Int = 0,
    @SerialName("clean_effects") val cleanEffects: Int = 0,
    @SerialName("attached_pic") val attachedPic: Int = 0,
    @SerialName("timed_thumbnails") val timedThumbnails: Int = 0
)
