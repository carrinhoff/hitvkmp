package pt.hitv.core.domain.repositories

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.database.entity.*
import pt.hitv.core.model.*
import pt.hitv.core.model.seriesInfo.*
import pt.hitv.core.common.Resources

/**
 * Interface defining operations for TV Show data.
 */
interface TvShowRepository {

    /**
     * Fetches TV show categories from the remote source.
     * (Credentials might be handled internally by the implementation)
     */
    suspend fun getSeriesCategories(username: String, password: String): Resources<List<Category>>

    /**
     * Fetches TV shows from the remote source.
     * (Credentials might be handled internally by the implementation)
     */
    suspend fun getSeries(username: String, password: String): Resources<List<TvShow>>

    /**
     * Fetches TV show categories with their associated TV shows from the local database.
     * Filters by the current user ID internally.
     */
    suspend fun getCategoriesWithTvShows(): List<CategoryWithTvShow> // Returns processed data directly from DB

    /**
     * Fetches all TV show data (categories and shows) from the remote source,
     * processes it, and saves it to the local database.
     * Returns the list of fetched TvShow models on success, or an error resource.
     */
    suspend fun fetchTvShowsData(): Resources<List<TvShow>>

    /**
     * Fetches detailed information for a specific series from the remote source,
     * processes it, and saves it to the local database.
     * Returns the full SeriesInfoResponse on success, or an error resource.
     * The returned data can be null even on success if the server response is empty.
     */
    suspend fun getSeriesInfo(seriesId: String): Resources<SeriesInfoResponse?>

    /**
     * Fetches processed seasons and their episodes for a specific series from the local database.
     * Returns a Flow emitting a map where keys are Seasons and values are lists of Episodes.
     */
    suspend fun fetchSeasonsWithEpisodes(seriesId: String): Flow<LinkedHashMap<Season, List<Episode>>>

    /**
     * Fetches basic series information for a specific series from the local database.
     * Returns a Flow emitting the SeriesInfo object.
     */
    fun fetchSeriesInfo(seriesId: String): Flow<SeriesInfo?>

    /**
     * Updates the favorite status of a TV show in the local database.
     * The implementation should handle updating all relevant entries for the show.
     */
    suspend fun saveFavoriteTvShow(tvShow: TvShow) // No change needed here

    /**
     * Gets a Flow of favorite TV shows for the current user from the local database.
     */
    suspend fun getFavoritesTvShow(): Flow<List<TvShow>> // Returns Flow<Domain Model>

    /**
     * Updates the last viewed timestamp for a TV show in the local database.
     */
    suspend fun saveRecentlyViewedTvShow(tvShow: TvShow)

    /**
     * Gets a Flow of recently viewed TV shows for the current user from the local database.
     * Returns Flow<Domain Model>.
     */
    suspend fun getRecentlyViewedTvShowsFlow(): Flow<List<TvShow>> // Renamed to clarify it's a flow

    /**
     * Updates the playback position for a specific episode in the local database.
     */
    suspend fun updatePlaybackPosition(id: String, position: Long)

    /**
     * Updates the duration for a specific episode in the local database.
     */
    suspend fun updateEpisodeDuration(id: String, duration: Double)

    fun getTvShowsPager(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String = "added",
        isAscending: Boolean = false
    ): Flow<PagingData<TvShow>>

    fun getAllTvShowCategories(userId: Int): Flow<List<Category>>

    /**
     * Gets series by category for horizontal scrolling sections.
     * Supports "All", "Favorites", "Recently Viewed", or specific category IDs.
     */
    suspend fun getSeriesByCategory(categoryId: String, limit: Int = 20): List<TvShow>

    // --- Default Category Methods ---
    /**
     * Get the default series category ID for the current user.
     * Returns the category ID as a string, or null if no default is set.
     */
    suspend fun getDefaultSeriesCategoryId(): String?

    /**
     * Search TV shows with FTS fallback to LIKE search.
     * @param query Search query
     * @param limit Maximum number of results
     * @return List of matching TV shows
     */
    suspend fun searchTvShowsWithFallback(query: String, limit: Int = 500): List<TvShow>

    /**
     * Get total count of TV shows for the current user.
     */
    suspend fun getTotalTvShowCount(): Int

    /**
     * Get count of TV shows in a specific category.
     * @param categoryId The category ID
     */
    suspend fun getCategoryTvShowCount(categoryId: String): Int

    /**
     * Get the most recently added TV shows.
     * @param limit Maximum number of shows to return
     * @return List of last added TV shows
     */
    suspend fun getLastAddedTvShows(limit: Int = 20): List<TvShow>

    /**
     * Get TV shows that are in progress (have watched episodes).
     * @param limit Maximum number of shows to return
     * @return List of continue watching TV shows
     */
    suspend fun getContinueWatchingSeries(limit: Int = 20): List<TvShow>
}
