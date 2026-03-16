package pt.hitv.core.network.model.cast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkCast(
    @SerialName("adult") val adult: Boolean = false,
    @SerialName("gender") val gender: Int = 0,
    @SerialName("id") val id: Int = 0,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    @SerialName("popularity") val popularity: Double = 0.0,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("cast_id") val castId: Int = 0,
    @SerialName("character") val character: String? = null,
    @SerialName("credit_id") val creditId: String? = null,
    @SerialName("order") val order: Int = 0
)
