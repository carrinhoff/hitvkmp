package pt.hitv.core.network.datasource

import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.network.api.TvShowApiService
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkTvShow
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfoResponse

/**
 * Remote data source for TV show / series operations.
 * Replaces the Hilt-injected TvShowRemoteDataSource.
 */
class TvShowRemoteDataSource(
    private val tvShowApiService: TvShowApiService,
    private val preferencesHelper: PreferencesHelper
) : BaseRemoteDataSource() {

    private val baseUrl get() = preferencesHelper.getHostUrl()
    private val username get() = preferencesHelper.getUsername()
    private val password get() = preferencesHelper.getPassword()

    suspend fun getSeriesCategories(): Resources<List<NetworkCategory>> {
        return getResult(endpoint = "get_series_categories") {
            tvShowApiService.getSeriesCategories(baseUrl, username, password)
        }
    }

    suspend fun getTvShows(): Resources<List<NetworkTvShow>> {
        return getResult(endpoint = "get_series") {
            tvShowApiService.getSeries(baseUrl, username, password, "get_series")
        }
    }

    suspend fun getSeriesInfo(seriesId: String): Resources<NetworkSeriesInfoResponse> {
        return getResult(endpoint = "get_series_info") {
            tvShowApiService.getSeriesInfo(
                baseUrl, username, password, "get_series_info", seriesId
            )
        }
    }
}
