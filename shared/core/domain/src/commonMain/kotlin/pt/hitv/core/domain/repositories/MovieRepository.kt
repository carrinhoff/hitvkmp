package pt.hitv.core.domain.repositories

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.database.entity.*
import pt.hitv.core.model.*
import pt.hitv.core.model.cast.CastResponse
import pt.hitv.core.model.movieInfo.CachedMovieInfo
import pt.hitv.core.model.movieInfo.MovieInfoResponse
import pt.hitv.core.common.Resources

interface MovieRepository {

    // --- Keep Existing Methods ---
    suspend fun getMoviesCategories(username: String, password: String): Resources<List<Category>>
    suspend fun getCategoriesWithMovies(): List<CategoryWithMovie>
    suspend fun getMovies(username: String, password: String): Resources<List<Movie>>
    suspend fun getMovieInfo(username: String, password: String, vodId: String): Resources<MovieInfoResponse>
    suspend fun insertCategoriesMovies(entitiesCategoryVod: ArrayList<EntityCategoryVod>)
    suspend fun insertMovies(entitiesMovies: ArrayList<EntityMovie>)
    suspend fun insertMovieInfo(movieInfo: MovieInfoResponse)
    suspend fun getMovieInfoCached(streamId: String): Resources<CachedMovieInfo?>
    suspend fun fetchMoviesData(): Resources<List<Movie>>
    suspend fun saveFavoriteMovie(movie: Movie) // Signature remains the same
    suspend fun getFavoritesMovie(): Flow<List<Movie>> // Returns Domain Model
    suspend fun getCast(tmdbId: String): Resources<CastResponse>
    suspend fun saveRecentlyViewedMovie(movie: Movie) // Signature remains the same
    suspend fun getRecentlyViewedMovies(): Flow<List<Movie>> // Returns Domain Model

    // Updated to support sorting and direction
    fun getMoviesPager(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String = "added",
        isAscending: Boolean = false
    ): Flow<PagingData<Movie>>

    // Count Methods ---
    suspend fun getTotalMovieCount(): Int
    suspend fun getCategoryMovieCount(categoryId: String): Int
    fun getAllMovieCategories(): Flow<List<Category>>

    // --- Category Movies Method ---
    suspend fun getMoviesByCategory(categoryId: String, limit: Int = 20): List<Movie>

    // --- Movie Playback Position Methods ---
    suspend fun updateMoviePlaybackPosition(streamId: Int, position: Long)
    suspend fun getMoviePlaybackPosition(streamId: Int): Long?

    // --- Offline-First Methods ---
    /**
     * Get movie info using offline-first pattern.
     *
     * Flow emissions:
     * 1. Loading with cached data (if available)
     * 2. Success with fresh data OR Error with cached fallback
     *
     * @param vodId The movie stream ID
     * @param forceRefresh If true, always fetch from network even if cache is fresh
     * @return Flow of Resources containing movie info
     */
    fun getMovieInfoOfflineFirst(
        vodId: String,
        forceRefresh: Boolean = false
    ): Flow<Resources<CachedMovieInfo?>>

    // --- Default Category Methods ---
    /**
     * Get the default movie category ID for the current user.
     * Returns the category ID as a string, or null if no default is set.
     */
    suspend fun getDefaultMovieCategoryId(): String?

    /**
     * Search movies with FTS fallback to LIKE search.
     * @param query Search query
     * @param limit Maximum number of results
     * @return List of matching movies
     */
    suspend fun searchMoviesWithFallback(query: String, limit: Int = 500): List<Movie>

    /**
     * Get the most recently added movies.
     * @param limit Maximum number of movies to return
     * @return List of last added movies
     */
    suspend fun getLastAddedMovies(limit: Int = 20): List<Movie>

    /**
     * Get movies that are in progress (have playback position).
     * @param limit Maximum number of movies to return
     * @return List of continue watching movies
     */
    suspend fun getContinueWatchingMovies(limit: Int = 20): List<Movie>
}
