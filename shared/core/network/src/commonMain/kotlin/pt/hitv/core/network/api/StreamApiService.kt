package pt.hitv.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkLiveStream
import pt.hitv.core.network.model.NetworkLoginResponse
import pt.hitv.core.network.model.NetworkTvShow
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfoResponse

/**
 * Ktor-based API service for stream-related endpoints (Xtream Codes player_api.php).
 * Replaces the Retrofit StreamApiService interface.
 */
class StreamApiService(private val client: HttpClient) {

    suspend fun getStreamsWithM3u(
        baseUrl: String,
        username: String,
        password: String,
        type: String,
        output: String
    ): String {
        return client.get("${baseUrl}get.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("type", type)
            parameter("output", output)
        }.bodyAsText()
    }

    suspend fun signIn(
        baseUrl: String,
        username: String,
        password: String
    ): NetworkLoginResponse {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
        }.body()
    }

    suspend fun getLiveStreams(
        baseUrl: String,
        username: String,
        password: String
    ): List<NetworkLiveStream> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", "get_live_streams")
        }.body()
    }

    suspend fun getCategories(
        baseUrl: String,
        username: String,
        password: String,
        action: String
    ): List<NetworkCategory> {
        return client.get("${baseUrl}player_api.php") {
            parameter("username", username)
            parameter("password", password)
            parameter("action", action)
        }.body()
    }

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

    suspend fun fetchEPG(
        baseUrl: String,
        username: String,
        password: String
    ): String {
        return client.get("${baseUrl}xmltv.php") {
            parameter("username", username)
            parameter("password", password)
        }.bodyAsText()
    }
}
