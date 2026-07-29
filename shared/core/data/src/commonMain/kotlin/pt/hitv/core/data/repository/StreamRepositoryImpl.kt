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
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
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
import app.cash.sqldelight.db.SqlDriver
import pt.hitv.core.data.sync.DifferentialChannelSync
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

/**
 * How far either side of "now" EPG programmes are retained, matching the original project's
 * EPG sync window. Anything outside it is dropped at parse time rather than being inserted and
 * later cleaned up.
 */
private const val EPG_WINDOW_DAYS = 7L
private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

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
    private val m3uParser: M3uParser,
    private val driver: SqlDriver
) : StreamRepository {

    private val userId: Int get() = preferencesHelper.getUserId()

    /** See [DifferentialChannelSync] for why channel sync is no longer INSERT OR REPLACE. */
    private val differentialChannelSync = DifferentialChannelSync(channelQueries)

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
                    // Categories are still written with INSERT OR REPLACE, which flattens the
                    // user's pin/hide/default flags — hence the snapshot/restore around them.
                    // See Category.selectPreferencesForSync for why this is not an UPSERT.
                    //
                    // Channels no longer need it: performDifferentialChannelSync carries
                    // isFavorite and lastViewedTimestamp across on the UPDATE path and never
                    // rewrites an unchanged row. The channel snapshot/restore is kept anyway as
                    // defence in depth — it is a narrow query (only favourited or watched rows)
                    // and, after a correct differential sync, a no-op. Given this path cannot be
                    // exercised on a real device from here, a redundant guard on the user's own
                    // data is the right trade.
                    val channelState = snapshotChannelUserState()
                    val categoryPrefs = snapshotCategoryPreferences()

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

                    differentialChannelSync.sync(liveStreams, userId, mainUrl)

                    restoreChannelUserState(channelState)
                    restoreCategoryPreferences(categoryPrefs)
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
        // Reactive: a Flow return type promises updates, and this used to emit exactly once.
        // Wrapped in flow{} so `userId` is still resolved at collection time, as before.
        return flow {
            emitAll(
                channelQueries.selectFavoritesPaged(userId.toLong(), Long.MAX_VALUE, 0L)
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { rows -> rows.map { it.toChannel() } }
            )
        }
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

    /**
     * Every programme overlapping [startTime]..[endTime] for the channels in [categoryId].
     *
     * Backs the EPG grid. This was a `return emptyList()` stub, which is why the grid had no data
     * source even once the query existed. `hasCatchUp` comes from the Channel row's `tvArchive`
     * flag — the grid uses it to mark past programmes as replayable, matching the original's
     * `EPGChannel.hasCatchUp`.
     */
    override suspend fun getProgrammesForCategory(
        categoryId: String,
        startTime: Long,
        endTime: Long
    ): List<ChannelEpgInfo> = withContext(Dispatchers.IO) {
        try {
            val rows = programmeQueries.selectProgrammesForCategory(
                categoryCreatorId = categoryId,
                userId = userId.toLong(),
                end_time = startTime,
                start_time = endTime,
            ).executeAsList()

            // One lookup per distinct channel rather than per programme row.
            val catchUpByEpgId = rows.mapNotNull { it.channel_id }
                .distinct()
                .associateWith { epgId ->
                    runCatching {
                        channelQueries.selectByEpgId(epgId, userId.toLong())
                            .executeAsOneOrNull()
                            ?.let { (it.tvArchive ?: 0L) > 0L }
                    }.getOrNull() ?: false
                }

            rows.map { row ->
                ChannelEpgInfo(
                    channelId = row.channel_id,
                    channelName = row.display_name ?: row.channel_name,
                    programmeTitle = row.title,
                    programmeDescription = row.description,
                    startTime = row.start_time,
                    endTime = row.end_time,
                    logo = row.logo,
                    hasCatchUp = catchUpByEpgId[row.channel_id] ?: false,
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
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
        // Reactive: a Flow return type promises updates, and this used to emit exactly once.
        // Wrapped in flow{} so `userId` is still resolved at collection time, as before.
        return flow {
            emitAll(
                channelQueries.selectRecentlyViewed(userId.toLong())
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { rows -> rows.map { it.toChannel() } }
            )
        }
    }

    override suspend fun saveM3uData(userId: Int, playlistName: String, channels: List<Channel>) {
        database.transaction {
            // Same INSERT OR REPLACE data loss as syncChannels — see snapshotChannelUserState.
            val channelState = snapshotChannelUserState(userId)
            val categoryPrefs = snapshotCategoryPreferences(userId)

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
                        epgChannelId = channel.epgChannelId?.trim()?.lowercase(),
                        categoryCreatorId = catId.toString(),
                        isFavorite = 0L,
                        licenseKey = null,
                        userId = userId.toLong(),
                        lastViewedTimestamp = 0L,
                        lastUpdated = now,
                        lastSeen = now,
                        contentHash = null,
                        syncVersion = 1L,
                        tvArchive = channel.tvArchive.toLong(),
                        tvArchiveDuration = channel.tvArchiveDuration.toLong(),
                        catchupType = channel.catchupType,
                        catchupSource = channel.catchupSource,
                    )
                }
            }

            restoreChannelUserState(channelState, userId)
            restoreCategoryPreferences(categoryPrefs, userId)
        }
    }

    // ===== Sync data-preservation helpers =====
    //
    // `syncChannels` and `saveM3uData` both write content with INSERT OR REPLACE, which resets
    // every column — including the ones the user owns (favourites, recently-viewed timestamps,
    // pinned/hidden/default categories). Before this, every content re-sync silently wiped all
    // of them, and on iOS that fires from the background BGTask with no user action at all.
    //
    // SQLite 3.19 (minSdk 26) has no UPSERT, so the fix is: snapshot the user-owned columns,
    // let the content write happen, then re-apply. Both snapshots are deliberately restricted
    // to rows the user has actually touched, so they stay small even on a 50k-channel account.
    // Called inside the caller's existing transaction, so it is all-or-nothing.

    private data class ChannelUserState(
        val name: String,
        val categoryCreatorId: String,
        val isFavorite: Long,
        val lastViewedTimestamp: Long,
    )

    private data class CategoryPreference(
        val categoryId: Long,
        val isPinned: Long,
        val isHidden: Long,
        val isDefault: Long,
    )

    private fun snapshotChannelUserState(forUserId: Int = userId): List<ChannelUserState> =
        channelQueries.selectUserStateForSync(forUserId.toLong())
            .executeAsList()
            .map {
                ChannelUserState(
                    name = it.name,
                    categoryCreatorId = it.categoryCreatorId,
                    isFavorite = it.isFavorite,
                    lastViewedTimestamp = it.lastViewedTimestamp,
                )
            }

    private fun restoreChannelUserState(
        snapshot: List<ChannelUserState>,
        forUserId: Int = userId,
    ) {
        snapshot.forEach {
            channelQueries.restoreUserStateForSync(
                isFavorite = it.isFavorite,
                lastViewedTimestamp = it.lastViewedTimestamp,
                name = it.name,
                userId = forUserId.toLong(),
                categoryCreatorId = it.categoryCreatorId,
            )
        }
    }

    private fun snapshotCategoryPreferences(forUserId: Int = userId): List<CategoryPreference> =
        categoryQueries.selectPreferencesForSync(forUserId.toLong())
            .executeAsList()
            .map {
                CategoryPreference(
                    categoryId = it.categoryId,
                    isPinned = it.isPinned,
                    isHidden = it.isHidden,
                    isDefault = it.isDefault,
                )
            }

    private fun restoreCategoryPreferences(
        snapshot: List<CategoryPreference>,
        forUserId: Int = userId,
    ) {
        snapshot.forEach {
            categoryQueries.restorePreferencesForSync(
                isPinned = it.isPinned,
                isHidden = it.isHidden,
                isDefault = it.isDefault,
                categoryId = it.categoryId,
                userId = forUserId.toLong(),
            )
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
                ).also {
                    // Custom-group and parental-control tables matter too: this source reads
                    // group membership for the custom-group filter, and filters out protected
                    // categories, so a change to either alters what the list should show.
                    it.invalidateOnChangeTo(
                        driver,
                        PagedTables.CHANNEL,
                        PagedTables.CUSTOM_GROUP_CHANNEL,
                        PagedTables.PARENTAL_CONTROL,
                    )
                }
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

    override suspend fun getCatchUpChannelCount(): Int {
        return withContext(Dispatchers.IO) {
            try {
                channelQueries.countCatchUpByUserId(userId.toLong()).executeAsOne().toInt()
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
        // Reactive: a Flow return type promises updates, and this used to emit exactly once.
        // Wrapped in flow{} so `userId` is still resolved at collection time, as before.
        return flow {
            emitAll(
                channelQueries.selectAllByUserId(userId.toLong())
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { rows -> rows.map { it.toChannel() } }
            )
        }
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

    /**
     * The EPG channel ids this user's channel list actually references, used as the XMLTV parse
     * allowlist. Mirrors the original's `channelFilter` (`XmltvParser.kt:64`): public feeds
     * routinely carry thousands of channels a given subscription doesn't include, and parsing
     * them all costs memory (fatally so on iOS, where the whole feed is in a String) and
     * pointless DB writes.
     *
     * Returns null — meaning "no filtering" — when the channel table is empty, which happens on
     * the very first sync where EPG can run before channels land. Filtering to an empty set
     * there would silently discard the entire feed.
     */
    private fun subscribedEpgChannelIds(): Set<String>? {
        val ids = runCatching {
            // The query aliases a LOWER(TRIM(...)) expression, so SQLDelight generates a
            // single-property wrapper row type rather than returning a bare String.
            channelQueries.selectEpgChannelIdsForUser(userId.toLong())
                .executeAsList()
                .mapNotNull { it.epgId }
                .toSet()
        }.getOrNull()
        return ids?.takeIf { it.isNotEmpty() }
    }

    /** The EPG guide URL persisted on the current account, if any (set for M3U logins). */
    private fun storedEpgUrl(): String? = runCatching {
        userCredentialsQueries.selectByUserId(userId.toLong())
            .executeAsOneOrNull()
            ?.epgUrl
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    /** Start of the retained EPG window: 7 days back, matching the original's sync window. */
    private fun epgWindowStartMs(): Long =
        Clock.System.now().toEpochMilliseconds() - EPG_WINDOW_DAYS * MILLIS_PER_DAY

    /** End of the retained EPG window: 7 days forward. */
    private fun epgWindowEndMs(): Long =
        Clock.System.now().toEpochMilliseconds() + EPG_WINDOW_DAYS * MILLIS_PER_DAY

    override suspend fun fetchEPG(
        epgUrlOverride: String?,
        onChannelProgress: suspend (channelsProcessed: Int, totalChannels: Int) -> Unit,
        onProgrammeProgress: suspend (programmesProcessed: Int, totalProgrammes: Int) -> Unit
    ): Resources<EpgDomainData> {
        // Fall back to the EPG URL stored on the account when no override is given. M3U/playlist
        // accounts have no Xtream xmltv.php endpoint — their guide URL comes from the playlist
        // header (`url-tvg` / `x-tvg-url`, extracted by M3uParser) and is persisted on
        // UserCredentials.epgUrl. The only caller (SyncManagerImpl.syncEpg) passes null, so
        // without this fallback that stored URL was never used and M3U accounts got no EPG at all.
        val effectiveEpgUrl = epgUrlOverride?.takeIf { it.isNotBlank() } ?: storedEpgUrl()
        if (!effectiveEpgUrl.isNullOrBlank()) {
            return when (val contentResource = m3uRemoteDataSource.fetchEpgFromUrl(effectiveEpgUrl)) {
                is Resources.Success -> {
                    val xmlContent = contentResource.data
                    if (xmlContent.isBlank() || !xmlContent.trim().startsWith("<")) {
                        return Resources.Error("Received invalid or non-XML content from EPG source.")
                    }
                    val epgData = EpgParser.parse(
                        xmlContent = xmlContent,
                        channelFilter = subscribedEpgChannelIds(),
                        minEndTimeMs = epgWindowStartMs(),
                        maxStartTimeMs = epgWindowEndMs(),
                    )
                    insertEpgDB(epgData, onChannelProgress, onProgrammeProgress)
                    Resources.Success(epgData)
                }
                is Resources.Error -> Resources.Error(contentResource.message)
                is Resources.Loading -> Resources.Loading()
            }
        } else {
            // Xtream EPG endpoint: use the platform-native streaming loader —
            // Android goes through HttpURLConnection + XmlPullParser so the
            // ~80 MB XMLTV feed never lives fully in memory. Ktor's HttpSend
            // save() previously OOMed here.
            return try {
                val baseUrl = preferencesHelper.getHostUrl()
                    .trimEnd('/') + "/"
                val username = preferencesHelper.getUsername()
                val password = preferencesHelper.getPassword()
                val epgData = pt.hitv.epg.EpgStreamingLoader.fetchAndParse(
                    baseUrl = baseUrl,
                    username = username,
                    password = password,
                    onProgress = { _, _ -> },
                    channelFilter = subscribedEpgChannelIds(),
                    minEndTimeMs = epgWindowStartMs(),
                    maxStartTimeMs = epgWindowEndMs(),
                )
                insertEpgDB(epgData, onChannelProgress, onProgrammeProgress)
                Resources.Success(epgData)
            } catch (e: Exception) {
                Resources.Error("EPG fetch failed: ${e.message}")
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

        // Clear this user's existing programme data before inserting the fresh feed.
        //
        // Without this the method only ever APPENDED: `insertProgramme` uses an AUTOINCREMENT id,
        // so every EPG sync created a brand-new row for every programme, plus its title and
        // description. At a 6-12 hour sync cadence the Programme / Title / Description tables grew
        // without bound — real disk pressure on a phone, and progressively slower EPG queries.
        // Build 22's "EPG query deduplicates overlapping programmes" was a workaround for the
        // symptom; this removes the cause.
        //
        // Only runs when there is a feed to replace it with, so a failed fetch cannot wipe the
        // guide. Clear-and-reinsert rather than a surgical diff because `Programme.last_updated`
        // (the column the original uses for non-destructive sync) has no SQLDelight equivalent —
        // see §5. The trade-off is a brief window mid-sync with partial EPG, which the next
        // scheduled sync corrects.
        if (programmesMap.isNotEmpty()) {
            database.transaction {
                programmeQueries.deleteTitlesByUserId(userId.toLong())
                programmeQueries.deleteDescriptionsByUserId(userId.toLong())
                programmeQueries.deleteProgrammesByUserId(userId.toLong())
            }
        }

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
        // Match the normalization used at EPG insert time (EpgStreamingLoader lowercases/trims
        // EpgChannel.channel_id and Programme.channel_name). Without this, feeds whose
        // channel IDs contain uppercase or whitespace never join and the row shows "No EPG".
        val epgId = channel.epgChannelId?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val row = programmeQueries.selectChannelWithProgrammeDetails(
                    epgId,
                    userId.toLong(),
                    currentTimeInMillis
                ).executeAsOneOrNull() ?: return@withContext null
                val epgChannel = epgChannelQueries.selectByChannelIdAndUserId(epgId, userId.toLong())
                    .executeAsOneOrNull()
                ChannelEpgInfo(
                    channelId = epgId,
                    channelName = epgChannel?.display_name ?: channel.name,
                    programmeTitle = row.title,
                    programmeDescription = row.desc,
                    startTime = row.start_time,
                    endTime = row.end_time,
                    logo = epgChannel?.logo
                )
            } catch (_: Throwable) {
                null
            }
        }
    }

    override suspend fun getPastProgramsForChannel(epgChannelId: String, limit: Int): List<ChannelEpgInfo> {
        val epgId = epgChannelId.trim().lowercase().takeIf { it.isNotBlank() } ?: return emptyList()
        return withContext(Dispatchers.IO) {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val rows = programmeQueries.selectPastProgrammes(
                    epgId,
                    userId.toLong(),
                    now,
                    limit.toLong()
                ).executeAsList()
                val epgChannel = epgChannelQueries.selectByChannelIdAndUserId(epgId, userId.toLong())
                    .executeAsOneOrNull()
                rows.map { row ->
                    ChannelEpgInfo(
                        channelId = row.channel_id,
                        channelName = row.display_name ?: row.channel_name,
                        programmeTitle = row.title,
                        programmeDescription = row.description,
                        startTime = row.start_time,
                        endTime = row.end_time,
                        logo = epgChannel?.logo ?: row.logo,
                    )
                }
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    override suspend fun fetchAndCacheServerTimezone(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uid = userId.toLong()
                val cached = userCredentialsQueries.selectServerTimezone(uid).executeAsOneOrNull()
                if (!cached?.serverTimezone.isNullOrEmpty()) {
                    return@withContext cached?.serverTimezone
                }
                val username = preferencesHelper.getUsername()
                val password = preferencesHelper.getPassword()
                if (username.isEmpty() || password.isEmpty()) return@withContext null
                val result = streamRemoteDataSource.signInWithFallback(username, password)
                if (result is Resources.Success) {
                    val tz = result.data?.asExternalModel()?.serverInfo?.timezone
                    if (!tz.isNullOrEmpty()) {
                        userCredentialsQueries.updateServerTimezone(tz, uid)
                        return@withContext tz
                    }
                }
                null
            } catch (_: Throwable) {
                null
            }
        }
    }

    override fun getAllChannelCategories(userId: Int): Flow<List<Category>> {
        return categoryQueries.selectVisibleSorted(userId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toCategory() } }
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
        // Reactive: a Flow return type promises updates, and this used to emit exactly once.
        // Wrapped in flow{} so `userId` is still resolved at collection time, as before.
        return flow {
            emitAll(
                channelQueries.selectAllByUserId(userId.toLong())
                    .asFlow()
                    .mapToList(Dispatchers.IO)
                    .map { rows -> rows.map { it.toChannel() } }
            )
        }
    }

    override fun getAllChannelCategoriesForParentalControl(userId: Int): Flow<List<Category>> {
        return categoryQueries.selectAllByUserId(userId.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toCategory() } }
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
                    // Word-order independent, matching the original's searchChannelsFlexible:
                    // every word must appear somewhere in the name, in any order. The previous
                    // single `%a%b%` pattern demanded the typed order and so missed, for example,
                    // "HD Sports" when the user typed "sports hd".
                    val slots = SearchUtils.flexibleLikeSlots(searchQuery)
                    val overflow = SearchUtils.overflowSearchWords(searchQuery)
                    channelQueries.searchByNameFlexible(
                        userId = userId.toLong(),
                        w1 = slots[0], w2 = slots[1], w3 = slots[2],
                        w4 = slots[3], w5 = slots[4], w6 = slots[5],
                        limit = pageSize.toLong(),
                        offset = offset.toLong(),
                    )
                        .executeAsList()
                        .filter { SearchUtils.matchesOverflowWords(it.name, overflow) }
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

                categoryId == CHANNEL_FILTER_CATCH_UP -> {
                    channelQueries.selectCatchUpPaged(userId.toLong(), pageSize.toLong(), offset.toLong())
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
                // Exhaustion is decided by what the QUERY returned, not by what survived parental
                // filtering. Using `filteredChannels.size` meant a single protected channel in a
                // page made the page look short, so `nextKey` went null and paging stopped dead —
                // silently truncating the channel list at the first protected entry.
                //
                // This was unreachable while PremiumStatusProvider forced parental controls off
                // (getProtectedCategoryIds returned empty, so nothing was ever filtered). Enabling
                // them in this pass made it live.
                nextKey = if (channels.size < pageSize) null else page + 1
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
