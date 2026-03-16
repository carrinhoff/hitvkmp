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
import pt.hitv.core.data.mapper.toSeriesInfo
import pt.hitv.core.data.mapper.toTvShow
import pt.hitv.core.data.paging.*
import pt.hitv.core.data.util.SearchUtils
import pt.hitv.core.database.CategoryTvShowQueries
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.database.SeriesInfoQueries
import pt.hitv.core.database.TvShowQueries
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.*
import pt.hitv.core.model.seriesInfo.*
import pt.hitv.core.network.datasource.TvShowRemoteDataSource

class TvShowRepositoryImpl(
    private val tvShowRemoteDataSource: TvShowRemoteDataSource,
    private val tvShowQueries: TvShowQueries,
    private val categoryTvShowQueries: CategoryTvShowQueries,
    private val seriesInfoQueries: SeriesInfoQueries,
    private val database: HitvDatabase,
    private val preferencesHelper: PreferencesHelper
) : TvShowRepository {

    private val userId: Int get() = preferencesHelper.getUserId()

    override suspend fun fetchTvShowsData(): Resources<List<TvShow>> {
        val categoriesResponse = tvShowRemoteDataSource.getSeriesCategories()

        if (categoriesResponse is Resources.Error) {
            return Resources.Error("Failed to fetch TV show categories: ${categoriesResponse.message}")
        }

        val seriesResponse = tvShowRemoteDataSource.getTvShows()

        if (seriesResponse is Resources.Success) {
            val networkTvShows = seriesResponse.data ?: emptyList()
            val networkCategories = (categoriesResponse as? Resources.Success)?.data ?: emptyList()

            val categories = networkCategories.map { it.asExternalModel() }
            val tvShows = networkTvShows.map { it.asExternalModel() }

            try {
                database.transaction {
                    categories.forEach { category ->
                        categoryTvShowQueries.insertOrReplace(
                            categoryId = category.categoryId,
                            categoryName = category.categoryName,
                            userId = userId,
                            isPinned = false,
                            isHidden = false,
                            isDefault = false
                        )
                    }

                    val now = Clock.System.now().toEpochMilliseconds()
                    tvShows.forEach { tvShow ->
                        tvShowQueries.insertOrReplace(
                            num = tvShow.num,
                            name = tvShow.name,
                            series_id = tvShow.seriesId ?: 0,
                            cover = tvShow.cover,
                            plot = tvShow.plot,
                            cast_ = tvShow.cast,
                            director = tvShow.director,
                            genre = tvShow.genre,
                            releaseDate = tvShow.releaseDate,
                            last_modified = tvShow.lastModified,
                            rating = tvShow.rating,
                            rating_5based = tvShow.rating5based,
                            backdrop_path = tvShow.backdropPath,
                            youtube_trailer = tvShow.youtubeTrailer,
                            episode_run_time = tvShow.episodeRunTime,
                            category_id = tvShow.categoryId ?: "-1",
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
                return Resources.Success(tvShows)
            } catch (e: Exception) {
                return Resources.Error("Database error while saving TV shows: ${e.message}", tvShows)
            }
        } else if (seriesResponse is Resources.Error) {
            return Resources.Error("Failed to fetch TV shows: ${seriesResponse.message}")
        }

        return Resources.Error("Unknown error during TV show data fetch.", null)
    }

    override suspend fun getSeriesInfo(seriesId: String): Resources<SeriesInfoResponse?> {
        val networkResponse = tvShowRemoteDataSource.getSeriesInfo(seriesId)

        if (networkResponse is Resources.Success) {
            networkResponse.data?.let { networkSeriesInfoResponse ->
                val seriesInfoResponse = networkSeriesInfoResponse.asExternalModel()

                seriesInfoResponse.info?.let { seriesInfo ->
                    try {
                        database.transaction {
                            seriesInfoQueries.insertSeriesInfo(
                                seriesId = seriesId,
                                name = seriesInfo.name,
                                cover = seriesInfo.cover,
                                plot = seriesInfo.plot,
                                cast_ = seriesInfo.cast,
                                director = seriesInfo.director,
                                genre = seriesInfo.genre,
                                releaseDate = seriesInfo.releaseDate,
                                last_modified = seriesInfo.lastModified,
                                rating = seriesInfo.rating,
                                rating_5based = seriesInfo.rating5based,
                                backdrop_path = seriesInfo.backdropPath.joinToString(","),
                                youtube_trailer = seriesInfo.youtubeTrailer,
                                episode_run_time = seriesInfo.episodeRunTime,
                                category_id = seriesInfo.categoryId,
                                userId = userId
                            )

                            seriesInfoResponse.episodes?.forEach { (seasonNumberString, episodeList) ->
                                try {
                                    val seasonNumber = seasonNumberString.toInt()
                                    val seasonId = "${seriesId}_S${seasonNumber}"
                                    val apiSeason = Season(
                                        id = seasonId,
                                        seasonNumber = seasonNumber,
                                        name = "Season $seasonNumber"
                                    )

                                    seriesInfoQueries.insertSeason(
                                        season_id = apiSeason.id ?: seasonId,
                                        air_date = apiSeason.airDate,
                                        episode_count = apiSeason.episodeCount,
                                        name = apiSeason.name,
                                        overview = apiSeason.overview,
                                        season_number = apiSeason.seasonNumber ?: seasonNumber,
                                        cover = apiSeason.cover,
                                        cover_big = apiSeason.coverBig,
                                        series_id = seriesId,
                                        userId = userId
                                    )

                                    episodeList.forEach { episode ->
                                        if (episode.id != null) {
                                            seriesInfoQueries.insertEpisode(
                                                episode_id = episode.id!!,
                                                episode_num = episode.episodeNum,
                                                title = episode.title,
                                                container_extension = episode.containerExtension,
                                                custom_sid = episode.customSid,
                                                added = episode.added,
                                                season = episode.season,
                                                direct_source = episode.directSource,
                                                seasonCreatorId = seasonId,
                                                userId = userId
                                            )

                                            episode.info?.let { episodeInfo ->
                                                seriesInfoQueries.insertEpisodeInfo(
                                                    episodeCreatorId = episode.id!!,
                                                    tmdb_id = episodeInfo.tmdbId,
                                                    release_date = episodeInfo.releasedate,
                                                    plot = episodeInfo.plot,
                                                    duration_secs = episodeInfo.durationSecs,
                                                    duration = episodeInfo.duration,
                                                    movie_image = episodeInfo.movieImage,
                                                    bitrate = episodeInfo.bitrate,
                                                    rating = episodeInfo.rating,
                                                    season = episodeInfo.season,
                                                    userId = userId,
                                                    playbackPosition = 0
                                                )
                                            }
                                        }
                                    }
                                } catch (_: NumberFormatException) {
                                }
                            }
                        }
                        return Resources.Success(seriesInfoResponse)
                    } catch (e: Exception) {
                        return Resources.Error("Database error saving series info: ${e.message}", seriesInfoResponse)
                    }
                } ?: return Resources.Success(seriesInfoResponse)
            } ?: return Resources.Success(null)
        } else if (networkResponse is Resources.Error) {
            return Resources.Error(networkResponse.message)
        }

        return Resources.Error("Unknown state fetching series info.", null)
    }

    override suspend fun fetchSeasonsWithEpisodes(seriesId: String): Flow<LinkedHashMap<Season, List<Episode>>> {
        return flow {
            val dataList = seriesInfoQueries.selectSeasonsWithEpisodes(seriesId, userId)
                .executeAsList()

            val seasonsAndEpisodes: LinkedHashMap<Season, List<Episode>> = linkedMapOf()
            dataList.forEach { data ->
                val season = Season(
                    airDate = data.airDate,
                    episodeCount = data.episodeCount?.toString()?.toIntOrNull(),
                    id = data.seasonId,
                    name = data.name,
                    overview = data.overview,
                    seasonNumber = data.seasonNumber,
                    cover = data.cover,
                    coverBig = data.coverBig
                )
                val episodeInfo = EpisodeInfo(
                    tmdbId = data.tmdbId,
                    releasedate = data.releaseDate,
                    plot = data.plot,
                    durationSecs = data.durationSecs,
                    duration = data.duration,
                    movieImage = data.movieImage,
                    bitrate = data.bitrate,
                    rating = data.rating,
                    season = data.season?.toString(),
                    playbackPosition = data.playbackPosition
                )
                val episode = Episode(
                    id = data.episodeId,
                    episodeNum = data.episodeNum,
                    title = data.title,
                    containerExtension = data.containerExtension,
                    info = episodeInfo,
                    customSid = "",
                    added = data.added,
                    season = data.season,
                    directSource = ""
                )
                val episodeList = seasonsAndEpisodes.getOrPut(season) { mutableListOf() }
                (episodeList as MutableList).add(episode)
            }
            val sortedMap = LinkedHashMap<Season, List<Episode>>()
            seasonsAndEpisodes.entries.sortedBy { it.key.seasonNumber }.forEach { sortedMap[it.key] = it.value }
            emit(sortedMap)
        }.flowOn(Dispatchers.IO)
    }

    override fun fetchSeriesInfo(seriesId: String): Flow<SeriesInfo?> {
        return flow {
            val entity = seriesInfoQueries.selectSeriesInfo(seriesId, userId)
                .executeAsOneOrNull()
            emit(entity?.toSeriesInfo())
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveFavoriteTvShow(tvShow: TvShow) {
        val currentStatus = tvShowQueries.selectFavoriteStatus(tvShow.seriesId ?: 0, userId.toLong())
            .executeAsOneOrNull()?.isFavorite ?: false
        tvShowQueries.updateFavoriteStatus(!currentStatus, tvShow.seriesId ?: 0, userId.toLong())
    }

    override suspend fun getFavoritesTvShow(): Flow<List<TvShow>> {
        return flow {
            val tvShows = tvShowQueries.selectFavorites(userId.toLong())
                .executeAsList()
                .map { it.toTvShow() }
            emit(tvShows)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveRecentlyViewedTvShow(tvShow: TvShow) {
        tvShowQueries.updateLastViewedTimestamp(
            tvShow.lastViewedTimestamp,
            tvShow.seriesId ?: 0,
            userId.toLong()
        )
    }

    override suspend fun getRecentlyViewedTvShowsFlow(): Flow<List<TvShow>> {
        return flow {
            val tvShows = tvShowQueries.selectRecentlyViewed(userId.toLong())
                .executeAsList()
                .map { it.toTvShow() }
            emit(tvShows)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updatePlaybackPosition(id: String, position: Long) {
        seriesInfoQueries.updateEpisodePlaybackPosition(position, id, userId)
    }

    override suspend fun updateEpisodeDuration(id: String, duration: Double) {
        seriesInfoQueries.updateEpisodeDuration(duration, id, userId)
    }

    override suspend fun getSeries(username: String, password: String): Resources<List<TvShow>> {
        return tvShowRemoteDataSource.getTvShows().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun getSeriesCategories(username: String, password: String): Resources<List<Category>> {
        return tvShowRemoteDataSource.getSeriesCategories().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun getCategoriesWithTvShows(): List<CategoryWithTvShow> {
        return withContext(Dispatchers.IO) {
            val categories = categoryTvShowQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            categories.mapNotNull { category ->
                val tvShows = tvShowQueries.selectByCategoryLimited(userId.toLong(), category.categoryId.toString(), 100)
                    .executeAsList()
                    .map { it.toTvShow() }
                if (tvShows.isNotEmpty()) {
                    CategoryWithTvShow.from(category, tvShows)
                } else null
            }
        }
    }

    override fun getTvShowsPager(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String,
        isAscending: Boolean
    ): Flow<PagingData<TvShow>> {
        return Pager(
            config = PagingConfig(
                pageSize = 40,
                prefetchDistance = 30,
                enablePlaceholders = true,
                initialLoadSize = 40,
                maxSize = 200
            ),
            pagingSourceFactory = {
                TvShowPagingSource(
                    tvShowQueries = tvShowQueries,
                    userId = userId,
                    categoryId = categoryId,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    isAscending = isAscending
                )
            }
        ).flow
    }

    override fun getAllTvShowCategories(userId: Int): Flow<List<Category>> {
        return flow {
            val categories = categoryTvShowQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            emit(categories)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getSeriesByCategory(categoryId: String, limit: Int): List<TvShow> {
        return withContext(Dispatchers.IO) {
            try {
                val entities = when (categoryId) {
                    MOVIE_FILTER_ALL -> tvShowQueries.selectAllLimited(userId.toLong(), limit.toLong()).executeAsList()
                    MOVIE_FILTER_FAVORITES -> tvShowQueries.selectFavorites(userId.toLong()).executeAsList().take(limit)
                    MOVIE_FILTER_RECENTLY_VIEWED -> tvShowQueries.selectRecentlyViewed(userId.toLong()).executeAsList().take(limit)
                    else -> tvShowQueries.selectByCategoryLimited(userId.toLong(), categoryId, limit.toLong()).executeAsList()
                }
                entities.map { it.toTvShow() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getDefaultSeriesCategoryId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                categoryTvShowQueries.selectDefaultCategory(userId.toLong())
                    .executeAsOneOrNull()?.categoryId?.toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun searchTvShowsWithFallback(query: String, limit: Int): List<TvShow> {
        return withContext(Dispatchers.IO) {
            try {
                val ftsQuery = SearchUtils.createFtsQuery(query)
                var results = tvShowQueries.searchFts(ftsQuery, userId.toLong(), limit.toLong(), 0)
                    .executeAsList()
                if (results.isEmpty()) {
                    results = tvShowQueries.searchByName(userId.toLong(), "%$query%", limit.toLong())
                        .executeAsList()
                }
                results.map { it.toTvShow() }
            } catch (e: Exception) {
                try {
                    tvShowQueries.searchByName(userId.toLong(), "%$query%", limit.toLong())
                        .executeAsList()
                        .map { it.toTvShow() }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
    }

    override suspend fun getTotalTvShowCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                tvShowQueries.countByUserId(userId.toLong()).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getCategoryTvShowCount(categoryId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                tvShowQueries.countByCategoryAndUserId(userId.toLong(), categoryId).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getLastAddedTvShows(limit: Int): List<TvShow> {
        return withContext(Dispatchers.IO) {
            try {
                tvShowQueries.selectLastAdded(userId.toLong(), limit.toLong())
                    .executeAsList()
                    .map { it.toTvShow() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getContinueWatchingSeries(limit: Int): List<TvShow> {
        return withContext(Dispatchers.IO) {
            try {
                tvShowQueries.selectContinueWatching(userId.toLong(), limit.toLong())
                    .executeAsList()
                    .map { it.toTvShow() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

/**
 * PagingSource implementation for TV shows using SQLDelight.
 */
private class TvShowPagingSource(
    private val tvShowQueries: TvShowQueries,
    private val userId: Int,
    private val categoryId: String?,
    private val searchQuery: String?,
    private val sortOrder: String,
    private val isAscending: Boolean
) : PagingSource<Int, TvShow>() {

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, TvShow> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val dbTvShows = when {
                !searchQuery.isNullOrBlank() -> {
                    val ftsQuery = SearchUtils.createFtsQuery(searchQuery)
                    tvShowQueries.searchFts(ftsQuery, userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_FAVORITES -> {
                    tvShowQueries.selectFavoritesPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_RECENTLY_VIEWED -> {
                    tvShowQueries.selectRecentlyViewedPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId == MOVIE_FILTER_LAST_ADDED -> {
                    tvShowQueries.selectLastAddedPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                }

                categoryId != null && categoryId != MOVIE_FILTER_ALL -> {
                    val isAscLong = if (isAscending) 1L else 0L
                    tvShowQueries.selectByCategorySorted(
                        userId.toLong(), categoryId, categoryId,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        pageSize.toLong(), offset.toLong()
                    ).executeAsList()
                }

                else -> {
                    val isAscLong = if (isAscending) 1L else 0L
                    tvShowQueries.selectByCategorySorted(
                        userId.toLong(), "", "",
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        sortOrder, isAscLong, sortOrder, isAscLong,
                        pageSize.toLong(), offset.toLong()
                    ).executeAsList()
                }
            }

            val tvShows = dbTvShows.map { it.toTvShow() }
            PagingSourceLoadResultPage(
                data = tvShows,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (tvShows.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            PagingSourceLoadResultError(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, TvShow>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
