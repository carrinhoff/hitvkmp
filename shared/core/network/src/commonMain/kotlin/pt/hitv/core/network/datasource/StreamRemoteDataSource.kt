package pt.hitv.core.network.datasource

import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.network.api.StreamApiService
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkLiveStream
import pt.hitv.core.network.model.NetworkLoginResponse
import pt.hitv.core.network.model.NetworkTvShow
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfoResponse

/**
 * Remote data source for stream-related operations.
 * Replaces the Hilt-injected StreamRemoteDataSource.
 */
class StreamRemoteDataSource(
    private val streamApiService: StreamApiService,
    private val preferencesHelper: PreferencesHelper
) : BaseRemoteDataSource() {

    private val baseUrl get() = preferencesHelper.getHostUrl()
    private val username get() = preferencesHelper.getUsername()
    private val password get() = preferencesHelper.getPassword()

    suspend fun getStreamsWithM3u(
        username: String,
        password: String,
        type: String,
        output: String
    ): Resources<String> {
        return getResult(endpoint = "get_streams_m3u") {
            streamApiService.getStreamsWithM3u(baseUrl, username, password, type, output)
        }
    }

    suspend fun getLiveStreams(): Resources<List<NetworkLiveStream>> {
        return getResult(endpoint = "get_live_streams") {
            streamApiService.getLiveStreams(baseUrl, username, password)
        }
    }

    suspend fun getCategories(): Resources<List<NetworkCategory>> {
        return getResult(endpoint = "get_live_categories") {
            streamApiService.getCategories(baseUrl, username, password, "get_live_categories")
        }
    }

    suspend fun getSeriesCategories(): Resources<List<NetworkCategory>> {
        return getResult(endpoint = "get_series_categories") {
            streamApiService.getSeriesCategories(baseUrl, username, password)
        }
    }

    suspend fun getTvShows(): Resources<List<NetworkTvShow>> {
        return getResult(endpoint = "get_series") {
            streamApiService.getSeries(baseUrl, username, password, "get_series")
        }
    }

    suspend fun signIn(username: String, password: String): Resources<NetworkLoginResponse> {
        return getResult(endpoint = "sign_in") {
            streamApiService.signIn(baseUrl, username, password)
        }
    }

    suspend fun signInWithFallback(
        username: String,
        password: String
    ): Resources<NetworkLoginResponse> {
        // Try original URL first
        val originalResult = signIn(username, password)

        return when (originalResult) {
            is Resources.Success -> originalResult
            is Resources.Error -> {
                val shouldTryFallback =
                    originalResult.message.contains("Failed to connect") ||
                        originalResult.message.contains("Connection refused") ||
                        originalResult.message.contains("SSL handshake failed") ||
                        originalResult.message.contains("Connection timed out")

                if (shouldTryFallback) {
                    tryLoginProtocolFallback(username, password, originalResult.message)
                } else {
                    originalResult
                }
            }
            is Resources.Loading -> originalResult
        }
    }

    private suspend fun tryLoginProtocolFallback(
        username: String,
        password: String,
        originalError: String
    ): Resources<NetworkLoginResponse> {
        val currentHostUrl = preferencesHelper.getHostUrl()
        val fallbackUrl = when {
            currentHostUrl.startsWith("https://") ->
                currentHostUrl.replace("https://", "http://")

            currentHostUrl.startsWith("http://") ->
                currentHostUrl.replace("http://", "https://")

            else -> return Resources.Error("Login failed: $originalError")
        }

        // Temporarily switch to fallback URL
        val originalUrl = preferencesHelper.getHostUrl()
        preferencesHelper.setStoredTag("hostUrl", fallbackUrl)

        return try {
            val fallbackResult = signIn(username, password)

            when (fallbackResult) {
                is Resources.Success -> {
                    // Keep the working URL for future calls
                    fallbackResult
                }
                is Resources.Error -> {
                    // Restore original URL
                    preferencesHelper.setStoredTag("hostUrl", originalUrl)
                    Resources.Error(
                        "Login failed with both protocols. Original: $originalError. Fallback: ${fallbackResult.message}"
                    )
                }
                is Resources.Loading -> fallbackResult
            }
        } catch (e: Exception) {
            // Restore original URL
            preferencesHelper.setStoredTag("hostUrl", originalUrl)
            Resources.Error("Login failed: $originalError")
        }
    }

    suspend fun fetchEPG(): Resources<String> {
        return getResult(endpoint = "fetch_epg") {
            streamApiService.fetchEPG(baseUrl, username, password)
        }
    }

    suspend fun getSeriesInfo(seriesId: String): Resources<NetworkSeriesInfoResponse> {
        return getResult(endpoint = "get_series_info") {
            streamApiService.getSeriesInfo(baseUrl, username, password, "get_series_info", seriesId)
        }
    }

    fun getMainUrl(): String {
        return "${preferencesHelper.getHostUrl()}live/${preferencesHelper.getUsername()}/${preferencesHelper.getPassword()}/"
    }
}
