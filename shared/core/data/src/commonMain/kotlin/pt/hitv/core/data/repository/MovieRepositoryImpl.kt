package pt.hitv.core.data.repository

import app.cash.paging.Pager
import app.cash.paging.PagingConfig
import app.cash.paging.PagingData
import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingSourceLoadResultError
import app.cash.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.common.mapData
import pt.hitv.core.data.mapper.asExternalModel
import pt.hitv.core.data.mapper.toCategory
import pt.hitv.core.data.mapper.toMovie
import pt.hitv.core.data.paging.*
import pt.hitv.core.data.util.CachePolicy
import pt.hitv.core.data.util.FetchTimeTracker
import pt.hitv.core.data.util.SearchUtils
import pt.hitv.core.data.util.networkBoundResourceWithMapper
import pt.hitv.core.database.CategoryVodQueries
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.database.MovieInfoQueries
import pt.hitv.core.database.MovieQueries
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.*
import pt.hitv.core.model.cast.CastResponse
import pt.hitv.core.model.movieInfo.CachedMovieInfo
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import pt.hitv.core.model.movieInfo.MovieInfoResponse
import pt.hitv.core.network.datasource.MovieRemoteDataSource
import pt.hitv.core.network.model.movieInfo.NetworkMovieInfoResponse

class MovieRepositoryImpl(
    private val movieRemoteDataSource: MovieRemoteDataSource,
    private val movieQueries: MovieQueries,
    private val categoryVodQueries: CategoryVodQueries,
    private val movieInfoQueries: MovieInfoQueries,
    private val database: HitvDatabase,
    private val preferencesHelper: PreferencesHelper
) : MovieRepository {

    private val userId: Int get() = preferencesHelper.getUserId()

    override suspend fun fetchMoviesData(): Resources<List<Movie>> {
        val categoriesResponse = movieRemoteDataSource.getVodCategories()

        if (categoriesResponse is Resources.Error) {
            return Resources.Error("Failed to fetch movie categories: ${categoriesResponse.message}")
        }
        if (categoriesResponse is Resources.Success && categoriesResponse.data.isNullOrEmpty()) {
            return Resources.Error("No movie categories found", null)
        }

        if (categoriesResponse is Resources.Success && !categoriesResponse.data.isNullOrEmpty()) {
            val moviesResponse = movieRemoteDataSource.getVodStreams()

            if (moviesResponse is Resources.Success) {
                if (!moviesResponse.data.isNullOrEmpty()) {
                    val categories = categoriesResponse.data.map { it.asExternalModel() }
                    val movies = moviesResponse.data.map { it.asExternalModel() }

                    try {
                        database.transaction {
                            categories.forEach { category ->
                                categoryVodQueries.insertOrReplace(
                                    categoryId = category.categoryId,
                                    categoryName = category.categoryName,
                                    userId = userId,
                                    isPinned = false,
                                    isHidden = false,
                                    isDefault = false
                                )
                            }

                            val now = Clock.System.now().toEpochMilliseconds()
                            movies.forEach { movie ->
                                movieQueries.insertOrReplace(
                                    name = movie.name,
                                    streamId = movie.streamId,
                                    streamIcon = movie.streamIcon ?: "",
                                    rating = movie.rating ?: "",
                                    added = movie.added,
                                    categoryCreatorId = movie.categoryId ?: "",
                                    containerExtension = movie.containerExtension,
                                    isFavorite = false,
                                    userId = userId,
                                    lastViewedTimestamp = 0,
                                    lastUpdated = now,
                                    lastSeen = now,
                                    contentHash = null,
                                    syncVersion = 1
                                )
                            }
                        }
                        return Resources.Success(movies)
                    } catch (e: Exception) {
                        return Resources.Error("Database error while saving movies: ${e.message}", movies)
                    }
                } else {
                    return Resources.Success(emptyList())
                }
            } else if (moviesResponse is Resources.Error) {
                return Resources.Error("Failed to fetch movies: ${moviesResponse.message}")
            }
        }

        return Resources.Error("Could not proceed fetching movies due to category fetch issue.", null)
    }

    override suspend fun saveFavoriteMovie(movie: Movie) {
        val currentStatus = movieQueries.selectFavoriteStatus(movie.streamId, userId.toLong())
            .executeAsOneOrNull()?.isFavorite ?: false
        movieQueries.updateFavorite(!currentStatus, movie.streamId, userId.toLong())
    }

    override suspend fun getFavoritesMovie(): Flow<List<Movie>> {
        return flow {
            val movies = movieQueries.selectFavorites(userId.toLong())
                .executeAsList()
                .map { it.toMovie() }
            emit(movies)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveRecentlyViewedMovie(movie: Movie) {
        movieQueries.updateLastViewedTimestamp(movie.lastViewedTimestamp, movie.streamId, userId.toLong())
    }

    override suspend fun getRecentlyViewedMovies(): Flow<List<Movie>> {
        return flow {
            val movies = movieQueries.selectRecentlyViewed(userId.toLong())
                .executeAsList()
                .map { it.toMovie() }
            emit(movies)
        }.flowOn(Dispatchers.IO)
    }

    override fun getMoviesPager(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String,
        isAscending: Boolean
    ): Flow<PagingData<Movie>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 30,
                enablePlaceholders = true,
                initialLoadSize = 40,
                maxSize = 200
            ),
            pagingSourceFactory = {
                MoviePagingSource(
                    movieQueries = movieQueries,
                    movieInfoQueries = movieInfoQueries,
                    userId = userId,
                    categoryId = categoryId,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    isAscending = isAscending
                )
            }
        ).flow
    }

    override suspend fun getTotalMovieCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                movieQueries.countByUserId(userId.toLong()).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getCategoryMovieCount(categoryId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                movieQueries.countByCategoryAndUserId(userId.toLong(), categoryId).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getMoviesCategories(username: String, password: String): Resources<List<Category>> {
        return movieRemoteDataSource.getVodCategories().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun getCategoriesWithMovies(): List<CategoryWithMovie> {
        return withContext(Dispatchers.IO) {
            val categories = categoryVodQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            categories.mapNotNull { category ->
                val movies = movieQueries.selectByCategoryLimited(userId.toLong(), category.categoryId.toString(), 100)
                    .executeAsList()
                    .map { it.toMovie() }
                if (movies.isNotEmpty()) {
                    CategoryWithMovie.from(category, movies)
                } else null
            }
        }
    }

    override suspend fun getMovies(username: String, password: String): Resources<List<Movie>> {
        return movieRemoteDataSource.getVodStreams().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun getMovieInfo(username: String, password: String, vodId: String): Resources<MovieInfoResponse> {
        return movieRemoteDataSource.getVodInfo(username, password, vodId).mapData { it.asExternalModel() }
    }

    override suspend fun insertCategoriesMovies(categories: List<Any>) {
        // Categories are inserted via fetchMoviesData transaction
    }

    override suspend fun insertMovies(movies: List<Any>) {
        // Movies are inserted via fetchMoviesData transaction
    }

    override suspend fun insertMovieInfo(movieInfo: MovieInfoResponse) {
        try {
            movieInfoQueries.insertMovieData(
                streamId = movieInfo.movieData.streamId,
                name = movieInfo.movieData.name,
                added = movieInfo.movieData.added,
                category_id = movieInfo.movieData.categoryId,
                container_extension = movieInfo.movieData.containerExtension,
                custom_sid = movieInfo.movieData.customSid,
                direct_source = movieInfo.movieData.directSource,
                userId = userId
            )
            movieInfoQueries.insertMovieInfo(
                streamIdCreator = movieInfo.movieData.streamId,
                kinopoisk_url = movieInfo.info.kinopoiskUrl,
                tmdb_id = movieInfo.info.tmdbId,
                name = movieInfo.info.name,
                o_name = movieInfo.info.oName,
                cover_big = movieInfo.info.coverBig,
                movie_image = movieInfo.info.movieImage,
                releasedate = movieInfo.info.releasedate,
                episode_run_time = movieInfo.info.episodeRunTime,
                youtube_trailer = movieInfo.info.youtubeTrailer,
                director = movieInfo.info.director,
                actors = movieInfo.info.actors,
                cast_ = movieInfo.info.cast,
                description = movieInfo.info.description,
                plot = movieInfo.info.plot,
                age = movieInfo.info.age,
                mpaa_rating = movieInfo.info.mpaaRating,
                rating_count_kinopoisk = movieInfo.info.ratingCountKinopoisk,
                country = movieInfo.info.country,
                genre = movieInfo.info.genre,
                backdrop_path = movieInfo.info.backdropPath?.joinToString(","),
                duration_secs = movieInfo.info.durationSecs,
                duration = movieInfo.info.duration,
                bitrate = movieInfo.info.bitrate,
                rating = movieInfo.info.rating,
                userId = userId,
                playbackPosition = 0
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getMovieInfoCached(streamId: String): Resources<CachedMovieInfo?> {
        return try {
            val result = movieInfoQueries.selectMovieDataWithInfo(streamId.toInt(), userId)
                .executeAsOneOrNull()
            if (result != null) {
                val movieData = MovieData(
                    streamId = result.streamId,
                    name = result.name,
                    added = result.added,
                    categoryId = result.category_id,
                    containerExtension = result.container_extension,
                    customSid = result.custom_sid,
                    directSource = result.direct_source
                )
                val info = Info(
                    kinopoiskUrl = result.kinopoisk_url,
                    tmdbId = result.tmdb_id,
                    name = result.name_,
                    oName = result.o_name,
                    coverBig = result.cover_big,
                    movieImage = result.movie_image,
                    releasedate = result.releasedate,
                    episodeRunTime = result.episode_run_time,
                    youtubeTrailer = result.youtube_trailer,
                    director = result.director,
                    actors = result.actors,
                    cast = result.cast_,
                    description = result.description,
                    plot = result.plot,
                    age = result.age,
                    mpaaRating = result.mpaa_rating,
                    ratingCountKinopoisk = result.rating_count_kinopoisk,
                    country = result.country,
                    genre = result.genre,
                    backdropPath = result.backdrop_path?.split(","),
                    durationSecs = result.duration_secs,
                    duration = result.duration,
                    bitrate = result.bitrate,
                    rating = result.rating_
                )
                val cached = CachedMovieInfo(
                    movieData = movieData,
                    info = info,
                    playbackPosition = result.playbackPosition
                )
                Resources.Success(cached)
            } else {
                Resources.Success(null)
            }
        } catch (e: Exception) {
            Resources.Error("Error accessing cache for movie info: ${e.message}", null)
        }
    }

    override suspend fun getCast(tmdbId: String): Resources<CastResponse> {
        return movieRemoteDataSource.getCast(tmdbId).mapData { it.asExternalModel() }
    }

    override fun getAllMovieCategories(): Flow<List<Category>> {
        return flow {
            val categories = categoryVodQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            emit(categories)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getMoviesByCategory(categoryId: String, limit: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = when (categoryId) {
                    MOVIE_FILTER_ALL -> movieQueries.selectAllLimited(userId.toLong(), limit.toLong()).executeAsList()
                    MOVIE_FILTER_FAVORITES -> movieQueries.selectFavorites(userId.toLong()).executeAsList().take(limit)
                    MOVIE_FILTER_RECENTLY_VIEWED -> movieQueries.selectRecentlyViewed(userId.toLong()).executeAsList().take(limit)
                    else -> movieQueries.selectByCategoryLimited(userId.toLong(), categoryId, limit.toLong()).executeAsList()
                }
                entities.map { it.toMovie() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun updateMoviePlaybackPosition(streamId: Int, position: Long) {
        withContext(Dispatchers.IO) {
            try {
                movieInfoQueries.updatePlaybackPosition(position, streamId, userId)
            } catch (_: Exception) {
            }
        }
    }

    override suspend fun getMoviePlaybackPosition(streamId: Int): Long? {
        return withContext(Dispatchers.IO) {
            try {
                movieInfoQueries.selectPlaybackPosition(streamId, userId)
                    .executeAsOneOrNull()?.playbackPosition
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun getMovieInfoOfflineFirst(
        vodId: String,
        forceRefresh: Boolean
    ): Flow<Resources<CachedMovieInfo?>> {
        val cacheKey = FetchTimeTracker.Keys.movieInfo(vodId)
        val policy = CachePolicy.DEFAULT_CONTENT_DETAIL

        return networkBoundResourceWithMapper<Any?, NetworkMovieInfoResponse>(
            query = {
                flow {
                    val cached = movieInfoQueries.selectMovieDataWithInfo(vodId.toIntOrNull() ?: 0, userId)
                        .executeAsOneOrNull()
                    emit(cached)
                }
            },
            fetch = {
                val username = preferencesHelper.getUsername()
                val password = preferencesHelper.getPassword()
                movieRemoteDataSource.getVodInfo(username, password, vodId)
            },
            saveFetchResult = { networkResponse ->
                try {
                    val response = networkResponse.asExternalModel()
                    insertMovieInfo(response)
                    FetchTimeTracker.recordFetch(cacheKey)
                } catch (_: Exception) {
                }
            },
            shouldFetch = { cachedData ->
                when {
                    forceRefresh -> true
                    cachedData == null -> true
                    else -> FetchTimeTracker.shouldFetch(cacheKey, policy)
                }
            },
            onFetchFailed = { _ -> }
        ).map { resource ->
            // Map the raw query result to CachedMovieInfo
            when (resource) {
                is Resources.Success -> getMovieInfoCached(vodId)
                is Resources.Error -> {
                    val cached = try { getMovieInfoCached(vodId) } catch (_: Exception) { Resources.Success(null) }
                    Resources.Error(resource.message, (cached as? Resources.Success)?.data)
                }
                is Resources.Loading -> {
                    val cached = try { getMovieInfoCached(vodId) } catch (_: Exception) { Resources.Success(null) }
                    Resources.Loading((cached as? Resources.Success)?.data)
                }
            }
        }
    }

    override suspend fun getDefaultMovieCategoryId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                categoryVodQueries.selectDefaultCategory(userId.toLong())
                    .executeAsOneOrNull()?.categoryId?.toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun searchMoviesWithFallback(query: String, limit: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                val ftsQuery = SearchUtils.createFtsQuery(query)
                var results = movieQueries.searchFts(ftsQuery, userId.toLong(), limit.toLong(), 0)
                    .executeAsList()

                if (results.isEmpty()) {
                    results = movieQueries.searchByName(userId.toLong(), "%$query%", limit.toLong())
                        .executeAsList()
                }

                results.map { it.toMovie() }
            } catch (e: Exception) {
                try {
                    movieQueries.searchByName(userId.toLong(), "%$query%", limit.toLong())
                        .executeAsList()
                        .map { it.toMovie() }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun getLastAddedMovies(limit: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                movieQueries.selectLastAdded(userId.toLong(), limit.toLong())
                    .executeAsList()
                    .map { it.toMovie() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getContinueWatchingMovies(limit: Int): List<Movie> {
        return withContext(Dispatchers.IO) {
            try {
                movieQueries.selectContinueWatching(userId.toLong(), limit.toLong())
                    .executeAsList()
                    .map { it.toMovie() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

/**
 * PagingSource implementation for movies using SQLDelight.
 */
private class MoviePagingSource(
    private val movieQueries: MovieQueries,
    private val movieInfoQueries: MovieInfoQueries,
    private val userId: Int,
    private val categoryId: String?,
    private val searchQuery: String?,
    private val sortOrder: String,
    private val isAscending: Boolean
) : PagingSource<Int, Movie>() {

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Movie> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val dbMovies = when {
                !searchQuery.isNullOrBlank() -> {
                    val ftsQuery = SearchUtils.createFtsQuery(searchQuery)
                    movieQueries.searchFts(ftsQuery, userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_FAVORITES -> {
                    movieQueries.selectFavoritesPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_RECENTLY_VIEWED -> {
                    movieQueries.selectRecentlyViewedPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_LAST_ADDED -> {
                    movieQueries.selectLastAddedPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_CONTINUE_WATCHING -> {
                    movieQueries.selectContinueWatching(userId.toLong(), pageSize.toLong())
                        .executeAsList()
                }

                categoryId != null && categoryId != MOVIE_FILTER_ALL -> {
                    val isAscLong = if (isAscending) 1L else 0L
                    movieQueries.selectByCategorySorted(
                        userId.toLong(), categoryId, categoryId,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        pageSize.toLong(), offset.toLong()
                    ).executeAsList()
                }

                else -> {
                    val isAscLong = if (isAscending) 1L else 0L
                    movieQueries.selectByCategorySorted(
                        userId.toLong(), "", "",
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        pageSize.toLong(), offset.toLong()
                    ).executeAsList()
                }
            }

            val movies = dbMovies.map { it.toMovie() }
            PagingSourceLoadResultPage(
                data = movies,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (movies.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            PagingSourceLoadResultError(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Movie>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
