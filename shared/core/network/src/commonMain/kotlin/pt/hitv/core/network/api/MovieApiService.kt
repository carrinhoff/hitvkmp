package pt.hitv.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkMovie
import pt.hitv.core.network.model.cast.NetworkCastResponse
import pt.hitv.core.network.model.movieInfo.NetworkMovieInfoResponse

/**
 * Ktor-based API service for movie/VOD endpoints (Xtream Codes player_api.php).
 * Replaces the Retrofit MovieApiService interface.
 */
class MovieApiService(private val client: HttpClient) {

    suspend fun getVod(
        baseUrl: String,
        username: String,
        password: String
    ): List<NetworkMovie> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", "get_vod_streams")
        }.body()
    }

    suspend fun getVodInfo(
        baseUrl: String,
        username: String,
        password: String,
        vodId: String
    ): NetworkMovieInfoResponse {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("vod_id", vodId)
            parameter("action", "get_vod_info")
        }.body()
    }

    suspend fun getVodCategories(
        baseUrl: String,
        username: String,
        password: String
    ): List<NetworkCategory> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", "get_vod_categories")
        }.body()
    }

    /**
     * Fetches cast information from TMDB API.
     * Uses an absolute URL (bypasses host selection).
     */
    suspend fun getCast(url: String): NetworkCastResponse {
        return client.get(url).body()
    }
}
