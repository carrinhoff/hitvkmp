package pt.hitv.core.network.datasource

import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.network.api.MovieApiService
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkMovie
import pt.hitv.core.network.model.cast.NetworkCastResponse
import pt.hitv.core.network.model.movieInfo.NetworkMovieInfoResponse

/**
 * Remote data source for movie/VOD operations.
 * Replaces the Hilt-injected MovieRemoteDataSource.
 */
class MovieRemoteDataSource(
    private val movieApiService: MovieApiService,
    private val preferencesHelper: PreferencesHelper,
    private val tmdbApiKey: String
) : BaseRemoteDataSource() {

    private val baseUrl get() = preferencesHelper.getHostUrl()
    private val username get() = preferencesHelper.getUsername()
    private val password get() = preferencesHelper.getPassword()

    suspend fun getVodStreams(): Resources<List<NetworkMovie>> {
        return getResult(endpoint = "get_vod_streams") {
            movieApiService.getVod(baseUrl, username, password)
        }
    }

    suspend fun getVodCategories(): Resources<List<NetworkCategory>> {
        return getResult(endpoint = "get_vod_categories") {
            movieApiService.getVodCategories(baseUrl, username, password)
        }
    }

    suspend fun getVodInfo(
        username: String,
        password: String,
        vodId: String
    ): Resources<NetworkMovieInfoResponse> {
        return getResult(endpoint = "get_vod_info/$vodId") {
            movieApiService.getVodInfo(baseUrl, username, password, vodId)
        }
    }

    suspend fun getCast(tmdbId: String): Resources<NetworkCastResponse> {
        val url = "https://api.themoviedb.org/3/movie/$tmdbId/credits?api_key=$tmdbApiKey"
        return getResult(endpoint = "tmdb/movie/$tmdbId/credits") {
            movieApiService.getCast(url)
        }
    }
}
