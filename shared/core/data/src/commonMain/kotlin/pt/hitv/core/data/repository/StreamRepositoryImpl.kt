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
import pt.hitv.core.data.mapper.toChannel
import pt.hitv.core.data.paging.*
import pt.hitv.core.data.parser.M3uParser
import pt.hitv.core.data.util.SearchUtils
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.database.CategoryQueries
import pt.hitv.core.database.CustomGroupQueries
import pt.hitv.core.database.EpgChannelQueries
import pt.hitv.core.database.ProgrammeQueries
import pt.hitv.core.database.UserCredentialsQueries
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.domain.manager.ParentalControlManager
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.*
import pt.hitv.core.network.datasource.M3uRemoteDataSource
import pt.hitv.core.network.datasource.StreamRemoteDataSource
import pt.hitv.epg.EpgDomainData
import pt.hitv.epg.EpgParser

class StreamRepositoryImpl(
    private val streamRemoteDataSource: StreamRemoteDataSource,
    private val m3uRemoteDataSource: M3uRemoteDataSource,
    private val channelQueries: ChannelQueries,
    private val categoryQueries: CategoryQueries,
    private val customGroupQueries: CustomGroupQueries,
    private val epgChannelQueries: EpgChannelQueries,
    private val programmeQueries: ProgrammeQueries,
    private val userCredentialsQueries: UserCredentialsQueries,
    private val database: HitvDatabase,
    private val preferencesHelper: PreferencesHelper,
    private val parentalControlManager: ParentalControlManager,
    private val m3uParser: M3uParser
) : StreamRepository {

    private val userId: Int get() = preferencesHelper.getUserId()

    override suspend fun fetchChannelsData(): Resources<List<LiveStream>> {
        val categoriesResponse = streamRemoteDataSource.getCategories()
        val mainUrl = streamRemoteDataSource.getMainUrl()

        if (categoriesResponse is Resources.Error) {
            return Resources.Error("Failed to fetch channel categories: ${categoriesResponse.message}")
        }

        val channelsResponse = streamRemoteDataSource.getLiveStreams()

        if (channelsResponse is Resources.Success) {
            val networkLiveStreams = channelsResponse.data ?: emptyList()
            val networkCategories = (categoriesResponse as? Resources.Success)?.data ?: emptyList()

            val categories = networkCategories.map { it.asExternalModel() }
            val liveStreams = networkLiveStreams.map { it.asExternalModel() }

            try {
                database.transaction {
                    categories.forEach { category ->
                        categoryQueries.insertOrReplace(
                            categoryId = category.categoryId.toLong(),
                            categoryName = category.categoryName,
                            userId = userId.toLong(),
                            isPinned = 0L,
                            isHidden = 0L,
                            isDefault = 0L
                        )
                    }

                    liveStreams.forEach { liveStream ->
                        val now = Clock.System.now().toEpochMilliseconds()
                        channelQueries.insertOrReplace(
                            name = liveStream.name,
                            streamUrl = mainUrl + liveStream.streamId,
                            streamIcon = liveStream.streamIcon,
                            epgChannelId = liveStream.epgChannelId,
                            categoryCreatorId = liveStream.categoryId.toString(),
                            isFavorite = 0L,
                            licenseKey = null,
                            userId = userId.toLong(),
                            lastViewedTimestamp = 0L,
                            lastUpdated = now,
                            lastSeen = now,
                            contentHash = null,
                            syncVersion = 1L
                        )
                    }
                }
                return Resources.Success(liveStreams)
            } catch (e: Exception) {
                return Resources.Error("Database error while saving channels: ${e.message}", liveStreams)
            }
        } else if (channelsResponse is Resources.Error) {
            return Resources.Error("Failed to fetch live streams: ${channelsResponse.message}")
        }

        return Resources.Error("Unknown error during channel data fetch.", null)
    }

    override suspend fun saveFavoriteChannel(channel: Channel) {
        val categoryId = channel.categoryId?.takeIf { it.isNotBlank() } ?: return
        val currentStatus = channelQueries.selectFavoriteStatus(channel.name ?: "", categoryId, userId.toLong())
            .executeAsOneOrNull() ?: 0L
        channelQueries.updateFavorite(if (currentStatus != 0L) 0L else 1L, channel.name ?: "", userId.toLong(), categoryId)
    }

    override suspend fun getFavoritesChannel(): Flow<List<Channel>> {
        return flow {
            val channels = channelQueries.selectFavoritesPaged(userId.toLong(), Long.MAX_VALUE, 0L)
                .executeAsList()
                .map { it.toChannel() }
            emit(channels)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getAllChannelsEpg(): List<ChannelEpgInfo> {
        // TODO: Implement EPG query with SQLDelight joins
        return emptyList()
    }

    override suspend fun getCategoriesWithEpgData(): List<Category> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val endTime = now + 24 * 60 * 60 * 1000L
                programmeQueries.selectCategoriesWithEpgCounts(userId.toLong(), now, endTime)
                    .executeAsList()
                    .map { Category(categoryId = it.categoryId.toInt(), categoryName = it.categoryName) }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getChannelCountWithEpgForCategory(categoryId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val endTime = now + 24 * 60 * 60 * 1000L
                programmeQueries.selectCategoriesWithEpgCounts(userId.toLong(), now, endTime)
                    .executeAsList()
                    .find { it.categoryId.toString() == categoryId }
                    ?.channelCount?.toInt() ?: 0
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getCategoriesWithCounts(): List<Pair<Category, Int>> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val endTime = now + 24 * 60 * 60 * 1000L
                programmeQueries.selectCategoriesWithEpgCounts(userId.toLong(), now, endTime)
                    .executeAsList()
                    .map {
                        Category(categoryId = it.categoryId.toInt(), categoryName = it.categoryName) to it.channelCount.toInt()
                    }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun getProgrammesForCategory(
        categoryId: String,
        startTime: Long,
        endTime: Long
    ): List<ChannelEpgInfo> {
        // TODO: Map from SQLDelight selectProgrammesForCategory result to ChannelEpgInfo
        return emptyList()
    }

    override suspend fun getChannel(name: String): Channel? {
        return channelQueries.selectByName(name, userId.toLong()).executeAsOneOrNull()?.toChannel()
    }

    override suspend fun getChannelByEpgId(epgChannelId: String): Channel? {
        return channelQueries.selectByEpgId(epgChannelId, userId.toLong()).executeAsOneOrNull()?.toChannel()
    }

    override suspend fun saveRecentlyViewedChannel(channel: Channel) {
        val categoryId = channel.categoryId?.takeIf { it.isNotBlank() } ?: return
        channelQueries.updateLastViewedTimestamp(
            channel.lastViewedTimestamp,
            channel.name ?: "",
            userId.toLong(),
            categoryId
        )
    }

    override suspend fun getRecentlyViewedChannels(): Flow<List<Channel>> {
        return flow {
            val channels = channelQueries.selectRecentlyViewed(userId.toLong())
                .executeAsList()
                .map { it.toChannel() }
            emit(channels)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun saveM3uData(userId: Int, playlistName: String, channels: List<Channel>) {
        database.transaction {
            val channelsByCategory = channels.groupBy { it.categoryId ?: "Uncategorized" }

            channelsByCategory.keys.forEachIndexed { index, categoryName ->
                val tempCategoryId = (index + 1).toLong()
                categoryQueries.insertOrReplace(
                    categoryId = tempCategoryId,
                    categoryName = categoryName,
                    userId = userId.toLong(),
                    isPinned = 0L,
                    isHidden = 0L,
                    isDefault = 0L
                )
            }

            val categoryNameToIdMap = channelsByCategory.keys.mapIndexed { index, name -> name to (index + 1) }.toMap()

            channelsByCategory.forEach { (categoryName, channelList) ->
                val catId = categoryNameToIdMap[categoryName]
                    ?: throw IllegalStateException("Category $categoryName not found in mapping for userId: $userId")

                channelList.forEach { channel ->
                    val now = Clock.System.now().toEpochMilliseconds()
                    channelQueries.insertOrReplace(
                        name = channel.name ?: "",
                        streamUrl = channel.streamUrl ?: "",
                        streamIcon = channel.streamIcon ?: "",
                        epgChannelId = channel.epgChannelId,
                        categoryCreatorId = catId.toString(),
                        isFavorite = 0L,
                        licenseKey = null,
                        userId = userId.toLong(),
                        lastViewedTimestamp = 0L,
                        lastUpdated = now,
                        lastSeen = now,
                        contentHash = null,
                        syncVersion = 1L
                    )
                }
            }
        }
    }

    override suspend fun fetchAndParseM3uUrl(userId: Int, playlistName: String, m3uUrl: String): Resources<Boolean> {
        return try {
            when (val contentResult = m3uRemoteDataSource.fetchM3uContent(m3uUrl)) {
                is Resources.Success -> {
                    val parseResult = m3uParser.parse(contentResult.data)
                    val parsedEpgUrl = parseResult.epgUrls.firstOrNull() ?: ""
                    if (parseResult.channels.isEmpty()) {
                        Resources.Error("M3U file is empty or contains no valid channels.")
                    } else {
                        if (parsedEpgUrl.isNotBlank()) {
                            val existing = userCredentialsQueries.selectByUserId(userId.toLong())
                                .executeAsOneOrNull()
                            if (existing?.epgUrl.isNullOrBlank()) {
                                userCredentialsQueries.updateEpgUrl(parsedEpgUrl, userId.toLong())
                            }
                        }
                        saveM3uData(userId, playlistName, parseResult.channels)
                        Resources.Success(true)
                    }
                }
                is Resources.Error -> {
                    Resources.Error("Download failed: ${contentResult.message}")
                }
                is Resources.Loading -> {
                    Resources.Loading()
                }
            }
        } catch (e: Exception) {
            Resources.Error("Processing failed: ${e.message}")
        }
    }

    override fun getChannelsPager(categoryId: String?, searchQuery: String?): Flow<PagingData<Channel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 60,
                prefetchDistance = 6,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                ChannelPagingSource(
                    channelQueries = channelQueries,
                    customGroupQueries = customGroupQueries,
                    userId = userId,
                    categoryId = categoryId,
                    searchQuery = searchQuery,
                    parentalControlManager = parentalControlManager
                )
            }
        ).flow
    }

    override suspend fun getTotalChannelCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                channelQueries.countByUserId(userId.toLong()).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getCategoryChannelCount(categoryId: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                channelQueries.countByCategoryAndUserId(userId.toLong(), categoryId).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    override suspend fun getStreamsWithM3u(username: String, password: String, type: String, output: String): Resources<String> {
        return streamRemoteDataSource.getStreamsWithM3u(username, password, type, output)
    }

    override suspend fun getCategories(): Resources<List<Category>> {
        return streamRemoteDataSource.getCategories().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun getLiveStreams(username: String, password: String): Resources<List<LiveStream>> {
        return streamRemoteDataSource.getLiveStreams().mapData { list -> list.map { it.asExternalModel() } }
    }

    override suspend fun signIn(username: String, password: String): Resources<LoginResponse> {
        return streamRemoteDataSource.signInWithFallback(username, password).mapData { it.asExternalModel() }
    }

    override suspend fun fetchChannelsFromDB(): Flow<List<Channel>> {
        return flow {
            val channels = channelQueries.selectAllByUserId(userId.toLong())
                .executeAsList()
                .map { it.toChannel() }
            emit(channels)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getCategoriesWithChannels(): List<CategoryWithChannel> {
        return withContext(Dispatchers.IO) {
            val categories = categoryQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            categories.mapNotNull { category ->
                val channels = channelQueries.selectByCategoryLimited(userId.toLong(), category.categoryId.toString(), 100L)
                    .executeAsList()
                    .map { it.toChannel() }
                if (channels.isNotEmpty()) {
                    CategoryWithChannel.from(category, channels)
                } else null
            }
        }
    }

    override suspend fun fetchEPG(
        epgUrlOverride: String?,
        onChannelProgress: suspend (channelsProcessed: Int, totalChannels: Int) -> Unit,
        onProgrammeProgress: suspend (programmesProcessed: Int, totalProgrammes: Int) -> Unit
    ): Resources<EpgDomainData> {
        if (!epgUrlOverride.isNullOrBlank()) {
            return when (val contentResource = m3uRemoteDataSource.fetchEpgFromUrl(epgUrlOverride)) {
                is Resources.Success -> {
                    val xmlContent = contentResource.data
                    if (xmlContent.isBlank() || !xmlContent.trim().startsWith("<")) {
                        return Resources.Error("Received invalid or non-XML content from EPG source.")
                    }
                    val epgData = EpgParser.parse(xmlContent)
                    insertEpgDB(epgData, onChannelProgress, onProgrammeProgress)
                    Resources.Success(epgData)
                }
                is Resources.Error -> Resources.Error(contentResource.message)
                is Resources.Loading -> Resources.Loading()
            }
        } else {
            return when (val result = streamRemoteDataSource.fetchEPG()) {
                is Resources.Success -> {
                    val xmlContent = result.data
                    if (xmlContent.isBlank()) {
                        return Resources.Error("Empty EPG response from server.")
                    }
                    val epgData = EpgParser.parse(xmlContent)
                    insertEpgDB(epgData, onChannelProgress, onProgrammeProgress)
                    Resources.Success(epgData)
                }
                is Resources.Error -> Resources.Error(result.message)
                is Resources.Loading -> Resources.Loading()
            }
        }
    }

    override suspend fun insertEpgDB(
        epgList: EpgDomainData?,
        onChannelProgress: suspend (channelsProcessed: Int, totalChannels: Int) -> Unit,
        onProgrammeProgress: suspend (programmesProcessed: Int, totalProgrammes: Int) -> Unit
    ) {
        val epgChannels = epgList?.channels ?: emptyList()
        val programmesMap = epgList?.programmes ?: emptyMap()

        val totalChannels = epgChannels.size
        val allProgrammes = programmesMap.values.flatten()
        val totalProgrammes = allProgrammes.size

        val uniqueCleanedChannels = epgChannels
            .filter { it.channelID.isNotEmpty() }
            .distinctBy { it.channelID.trim().lowercase() }

        val cleanedChannelIdsSet = uniqueCleanedChannels.map { it.channelID.trim().lowercase() }.toSet()

        if (uniqueCleanedChannels.isNotEmpty()) {
            database.transaction {
                uniqueCleanedChannels.forEach { epgChannel ->
                    epgChannelQueries.insertOrReplace(
                        channel_id = epgChannel.channelID.trim().lowercase(),
                        display_name = epgChannel.name.ifBlank { null },
                        logo = epgChannel.imageURL.ifBlank { null },
                        userId = userId.toLong()
                    )
                }
            }
        }
        onChannelProgress(totalChannels, totalChannels)

        var programmesProcessed = 0
        val batchSize = 500

        // Flatten programmes map into a list of (channelId, event) pairs
        val programmeEntries = programmesMap.flatMap { (channelId, events) ->
            events.map { channelId.trim().lowercase() to it }
        }
        val programmeBatches = programmeEntries.chunked(batchSize)

        for (programmeBatch in programmeBatches) {
            database.transaction {
                programmeBatch.forEach { (channelId, event) ->
                    if (channelId.isNotEmpty() && cleanedChannelIdsSet.contains(channelId) &&
                        event.start > 0 && event.end > 0
                    ) {
                        try {
                            programmeQueries.insertProgramme(
                                channel_name = channelId,
                                start_time = event.start,
                                end_time = event.end,
                                userId = userId.toLong(),
                                imageUrl = event.imageURL.ifBlank { null }
                            )
                            val programmeId = programmeQueries.lastInsertProgrammeId().executeAsOne().MAX

                            if (event.title.isNotBlank()) {
                                programmeQueries.insertTitle(
                                    title = event.title,
                                    programme_id = programmeId,
                                    userId = userId.toLong()
                                )
                            }

                            if (event.description.isNotBlank()) {
                                programmeQueries.insertDescription(
                                    desc = event.description,
                                    programme_id = programmeId,
                                    userId = userId.toLong()
                                )
                            }
                        } catch (_: Exception) {
                            // Skip problematic programmes
                        }
                    }
                }
            }

            programmesProcessed += programmeBatch.size
            onProgrammeProgress(programmesProcessed, totalProgrammes)
        }
    }

    override suspend fun fetchCurrentEpg(channel: Channel, currentTimeInMillis: Long): ChannelEpgInfo? {
        // TODO: Implement EPG current programme query
        return null
    }

    override fun getAllChannelCategories(userId: Int): Flow<List<Category>> {
        return flow {
            val categories = categoryQueries.selectVisibleSorted(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            emit(categories)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getDefaultChannelCategoryId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                categoryQueries.selectDefaultCategory(userId.toLong())
                    .executeAsOneOrNull()?.categoryId?.toString()
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getDefaultCustomGroupId(): String? {
        return withContext(Dispatchers.IO) {
            try {
                customGroupQueries.selectDefaultGroup().executeAsOneOrNull()
                    ?.let { "custom_group_${it.groupId}" }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun getAllChannelsFlow(): Flow<List<Channel>> {
        return flow {
            val channels = channelQueries.selectAllByUserId(userId.toLong())
                .executeAsList()
                .map { it.toChannel() }
            emit(channels)
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllChannelCategoriesForParentalControl(userId: Int): Flow<List<Category>> {
        return flow {
            val categories = categoryQueries.selectAllByUserId(userId.toLong())
                .executeAsList()
                .map { it.toCategory() }
            emit(categories)
        }.flowOn(Dispatchers.IO)
    }
}

/**
 * PagingSource implementation for channels using SQLDelight.
 */
private class ChannelPagingSource(
    private val channelQueries: ChannelQueries,
    private val customGroupQueries: CustomGroupQueries,
    private val userId: Int,
    private val categoryId: String?,
    private val searchQuery: String?,
    private val parentalControlManager: ParentalControlManager
) : PagingSource<Int, Channel>() {

    override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Channel> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            val channels: List<Channel> = when {
                categoryId?.startsWith(CHANNEL_FILTER_CUSTOM_GROUP_PREFIX) == true -> {
                    val groupId = categoryId.removePrefix(CHANNEL_FILTER_CUSTOM_GROUP_PREFIX).toLongOrNull()
                    if (groupId != null) {
                        customGroupQueries.selectChannelsInGroupPaged(groupId, pageSize.toLong(), offset.toLong())
                            .executeAsList()
                            .map { it.toChannel() }
                    } else emptyList()
                }

                !searchQuery.isNullOrBlank() -> {
                    val words = SearchUtils.normalizeSearchWords(searchQuery)
                    val likePattern = "%${words.joinToString("%")}%"
                    channelQueries.searchByName(userId.toLong(), likePattern, pageSize.toLong(), offset.toLong())
                        .executeAsList()
                        .map { it.toChannel() }
                }

                categoryId == CHANNEL_FILTER_FAVORITES -> {
                    channelQueries.selectFavoritesPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                        .map { it.toChannel() }
                }

                categoryId == CHANNEL_FILTER_RECENTLY_VIEWED -> {
                    channelQueries.selectRecentlyViewedPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                        .map { it.toChannel() }
                }

                categoryId != null && categoryId != CHANNEL_FILTER_ALL -> {
                    channelQueries.selectByCategoryPaged(userId.toLong(), categoryId, pageSize.toLong(), offset.toLong())
                        .executeAsList()
                        .map { it.toChannel() }
                }

                else -> {
                    channelQueries.selectAllPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
                        .executeAsList()
                        .map { it.toChannel() }
                }
            }

            // Apply parental control filtering for "all" view
            val filteredChannels = if (categoryId == null || categoryId == CHANNEL_FILTER_ALL) {
                val protectedCategoryIds = parentalControlManager.getProtectedCategoryIds(userId)
                if (protectedCategoryIds.isNotEmpty()) {
                    channels.filter { channel -> channel.categoryId !in protectedCategoryIds }
                } else {
                    channels
                }
            } else {
                channels
            }

            PagingSourceLoadResultPage(
                data = filteredChannels,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (filteredChannels.size < pageSize) null else page + 1
            )
        } catch (e: Exception) {
            PagingSourceLoadResultError(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Channel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}
