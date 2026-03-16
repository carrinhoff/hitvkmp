package pt.hitv.core.network.model.movieInfo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkMovieInfoResponse(
    @SerialName("info") val info: NetworkInfo? = null,
    @SerialName("movie_data") val movieData: NetworkMovieData? = null
)
