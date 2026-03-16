package pt.hitv.core.testing.repository

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import pt.hitv.core.common.Resources
import pt.hitv.core.database.entity.EntityCategoryVod
import pt.hitv.core.database.entity.EntityMovie
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.Category
import pt.hitv.core.model.CategoryWithMovie
import pt.hitv.core.model.Movie
import pt.hitv.core.model.cast.CastResponse
import pt.hitv.core.model.movieInfo.CachedMovieInfo
import pt.hitv.core.model.movieInfo.MovieInfoResponse

/**
 * Fake implementation of [MovieRepository] for testing.
 *
 * This implementation stores data in memory and provides control over
 * responses for testing different scenarios.
 *
 * Usage:
 * ```
 * val fakeRepo = FakeMovieRepository()
 * fakeRepo.setMovies(listOf(movie1, movie2))
 * fakeRepo.setCategories(listOf(category1, category2))
 *
 * // For error scenarios:
 * fakeRepo.setShouldReturnError(true)
 * ```
 */
class FakeMovieRepository : MovieRepository {

    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _favorites = MutableStateFlow<List<Movie>>(emptyList())
    private val _recentlyViewed = MutableStateFlow<List<Movie>>(emptyList())
    private val _playbackPositions = mutableMapOf<Int, Long>()

    private var shouldReturnError = false
    private var errorMessage = "Test error"

    // ==================== Test Control Methods ====================

    fun setMovies(movies: List<Movie>) {
        _movies.value = movies
    }

    fun setCategories(categories: List<Category>) {
        _categories.value = categories
    }

    fun setFavorites(favorites: List<Movie>) {
        _favorites.value = favorites
    }

    fun setRecentlyViewed(recentlyViewed: List<Movie>) {
        _recentlyViewed.value = recentlyViewed
    }

    fun setShouldReturnError(shouldError: Boolean, message: String = "Test error") {
        shouldReturnError = shouldError
        errorMessage = message
    }

    fun clear() {
        _movies.value = emptyList()
        _categories.value = emptyList()
        _favorites.value = emptyList()
        _recentlyViewed.value = emptyList()
        _playbackPositions.clear()
        shouldReturnError = false
    }

    // ==================== MovieRepository Implementation ====================

    override suspend fun getMoviesCategories(
        username: String,
        password: String
    ): Resources<List<Category>> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Success(_categories.value)
        }
    }

    override suspend fun getCategoriesWithMovies(): List<CategoryWithMovie> {
        return emptyList() // Simplified for testing
    }

    override suspend fun getMovies(
        username: String,
        password: String
    ): Resources<List<Movie>> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Success(_movies.value)
        }
    }

    override suspend fun getMovieInfo(
        username: String,
        password: String,
        vodId: String
    ): Resources<MovieInfoResponse> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Error("Not implemented in fake")
        }
    }

    override suspend fun insertCategoriesMovies(entitiesCategoryVod: ArrayList<EntityCategoryVod>) {
        // No-op for fake
    }

    override suspend fun insertMovies(entitiesMovies: ArrayList<EntityMovie>) {
        // No-op for fake
    }

    override suspend fun insertMovieInfo(movieInfo: MovieInfoResponse) {
        // No-op for fake
    }

    override suspend fun getMovieInfoCached(streamId: String): Resources<CachedMovieInfo?> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Success(null)
        }
    }

    override suspend fun fetchMoviesData(): Resources<List<Movie>> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Success(_movies.value)
        }
    }

    override suspend fun saveFavoriteMovie(movie: Movie) {
        val currentFavorites = _favorites.value.toMutableList()
        val existingIndex = currentFavorites.indexOfFirst { it.movieId == movie.movieId }
        if (existingIndex >= 0) {
            if (movie.isFavorite) {
                currentFavorites[existingIndex] = movie
            } else {
                currentFavorites.removeAt(existingIndex)
            }
        } else if (movie.isFavorite) {
            currentFavorites.add(movie)
        }
        _favorites.value = currentFavorites
    }

    override suspend fun getFavoritesMovie(): Flow<List<Movie>> {
        return _favorites
    }

    override suspend fun getCast(tmdbId: String): Resources<CastResponse> {
        return if (shouldReturnError) {
            Resources.Error(errorMessage)
        } else {
            Resources.Error("Not implemented in fake")
        }
    }

    override suspend fun saveRecentlyViewedMovie(movie: Movie) {
        val current = _recentlyViewed.value.toMutableList()
        current.removeAll { it.movieId == movie.movieId }
        current.add(0, movie)
        _recentlyViewed.value = current.take(20)
    }

    override suspend fun getRecentlyViewedMovies(): Flow<List<Movie>> {
        return _recentlyViewed
    }

    override fun getMoviesPager(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String,
        isAscending: Boolean
    ): Flow<PagingData<Movie>> {
        var filtered = _movies.value

        if (categoryId != null && categoryId != "all") {
            filtered = filtered.filter { it.categoryId == categoryId }
        }

        if (!searchQuery.isNullOrBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        }

        filtered = when (sortOrder) {
            "name" -> if (isAscending) filtered.sortedBy { it.name }
            else filtered.sortedByDescending { it.name }
            "rating" -> if (isAscending) filtered.sortedBy { it.rating5based }
            else filtered.sortedByDescending { it.rating5based }
            else -> if (isAscending) filtered.sortedBy { it.added }
            else filtered.sortedByDescending { it.added }
        }

        return flowOf(PagingData.from(filtered))
    }

    override suspend fun getTotalMovieCount(): Int = _movies.value.size

    override suspend fun getCategoryMovieCount(categoryId: String): Int {
        return _movies.value.count { it.categoryId == categoryId }
    }

    override fun getAllMovieCategories(): Flow<List<Category>> = _categories

    override suspend fun getMoviesByCategory(categoryId: String, limit: Int): List<Movie> {
        return _movies.value
            .filter { it.categoryId == categoryId }
            .take(limit)
    }

    // ==================== Default Category Methods ====================

    private var _defaultCategoryId: String? = null

    fun setDefaultCategoryId(categoryId: String?) {
        _defaultCategoryId = categoryId
    }

    override suspend fun getDefaultMovieCategoryId(): String? = _defaultCategoryId

    override suspend fun searchMoviesWithFallback(query: String, limit: Int): List<Movie> {
        return _movies.value
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(limit)
    }

    override suspend fun getLastAddedMovies(limit: Int): List<Movie> {
        return _movies.value
            .sortedByDescending { it.added }
            .take(limit)
    }

    override suspend fun getContinueWatchingMovies(limit: Int): List<Movie> {
        return _movies.value
            .filter { movie -> _playbackPositions[movie.streamId.toIntOrNull() ?: 0] != null }
            .take(limit)
    }

    override suspend fun updateMoviePlaybackPosition(streamId: Int, position: Long) {
        _playbackPositions[streamId] = position
    }

    override suspend fun getMoviePlaybackPosition(streamId: Int): Long? {
        return _playbackPositions[streamId]
    }

    // ==================== Offline-First Methods ====================

    private val _movieInfoCache = mutableMapOf<String, CachedMovieInfo>()

    fun setMovieInfo(vodId: String, movieInfo: CachedMovieInfo) {
        _movieInfoCache[vodId] = movieInfo
    }

    override fun getMovieInfoOfflineFirst(
        vodId: String,
        forceRefresh: Boolean
    ): Flow<Resources<CachedMovieInfo?>> {
        return flow {
            val cached = _movieInfoCache[vodId]
            emit(Resources.Loading(cached))

            if (shouldReturnError) {
                emit(Resources.Error(errorMessage, cached))
            } else {
                emit(Resources.Success(cached))
            }
        }
    }
}
