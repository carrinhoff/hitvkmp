package pt.hitv.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkTvShow
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfoResponse

/**
 * Ktor-based API service for TV show / series endpoints (Xtream Codes player_api.php).
 * Replaces the Retrofit TvShowApiService interface.
 */
class TvShowApiService(private val client: HttpClient) {

    suspend fun getSeriesCategories(
        baseUrl: String,
        username: String,
        password: String
    ): List<NetworkCategory> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", "get_series_categories")
        }.body()
    }

    suspend fun getSeries(
        baseUrl: String,
        username: String,
        password: String,
        action: String
    ): List<NetworkTvShow> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", action)
        }.body()
    }

    suspend fun getSeriesInfo(
        baseUrl: String,
        username: String,
        password: String,
        action: String,
        seriesId: String
    ): NetworkSeriesInfoResponse {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", action)
            parameter("series_id", seriesId)
        }.body()
    }
}
