package pt.hitv.core.domain.repositories

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.*
import pt.hitv.core.common.Resources
import pt.hitv.epg.EpgDomainData

interface StreamRepository {

    // --- Existing Methods (Keep relevant ones) ---
    suspend fun getStreamsWithM3u(username: String, password: String, type: String, output: String): Resources<String>
    suspend fun getCategories(): Resources<List<Category>> // Needed for menu
    suspend fun getLiveStreams(username: String, password: String): Resources<List<LiveStream>> // Needed for initial fetch
    suspend fun signIn(username: String, password: String): Resources<LoginResponse>
    // suspend fun insertChannels(entityChannels: ArrayList<EntityChannel>): List<Long> // Might be internal now
    suspend fun fetchChannelsFromDB(): Flow<List<Channel>> // Replaced by Pager
    // suspend fun insertCategories(entitiesCategories: ArrayList<EntityCategory>) : List<Long> // Might be internal
    // suspend fun fetchCategoriesFromDB(): Flow<List<Category>> // Keep if needed elsewhere
    suspend fun getCategoriesWithChannels(): List<CategoryWithChannel> // Needed for menu
    // suspend fun getCategoriesWithChannelsMenu(): List<CategoryWithChannel> // Likely same as above
    suspend fun fetchEPG(
        epgUrlOverride: String? = null,
        onChannelProgress: suspend (channelsProcessed: Int, totalChannels: Int) -> Unit,
        onProgrammeProgress: suspend (programmesProcessed: Int, totalProgrammes: Int) -> Unit // Add this new callback
    ): Resources<EpgDomainData>

    suspend fun insertEpgDB(
        epgList: EpgDomainData?,
        onChannelProgress: suspend (channelsProcessed: Int, totalChannels: Int) -> Unit,
        onProgrammeProgress: suspend (programmesProcessed: Int, totalProgrammes: Int) -> Unit // Add this new callback
    )
    suspend fun fetchCurrentEpg(channel: Channel, currentTimeInMillis: Long): ChannelEpgInfo?

    /**
     * Past programmes (already ended) for a channel, newest first.
     * Drives the catch-up archive sheet. `epgChannelId` is normalized to
     * lowercase+trimmed inside the repository.
     */
    suspend fun getPastProgramsForChannel(epgChannelId: String, limit: Int = Int.MAX_VALUE): List<ChannelEpgInfo>

    /**
     * Fetches the Xtream server timezone and caches it in UserCredentials.
     * Returns the cached value if already present. Used by the catch-up URL
     * builder to format start-times for the XC timeshift endpoint in the
     * server's local time rather than UTC.
     */
    suspend fun fetchAndCacheServerTimezone(): String?
    suspend fun fetchChannelsData(): Resources<List<LiveStream>> // For initial data fetch/refresh
    suspend fun saveFavoriteChannel(channel: Channel) // Keep signature
    suspend fun getFavoritesChannel(): Flow<List<Channel>> // Returns Domain Model (consistent with MovieRepository/TvShowRepository)
    suspend fun getAllChannelsEpg(): List<ChannelEpgInfo>

    // New efficient EPG methods
    suspend fun getCategoriesWithEpgData(): List<Category>
    suspend fun getChannelCountWithEpgForCategory(categoryId: String): Int
    suspend fun getCategoriesWithCounts(): List<Pair<Category, Int>>
    suspend fun getProgrammesForCategory(
        categoryId: String,
        startTime: Long,
        endTime: Long
    ): List<ChannelEpgInfo>

    suspend fun getChannel(name: String): Channel? // Keep if needed
    suspend fun getChannelByEpgId(epgChannelId: String): Channel? // Get channel by EPG ID
    suspend fun saveRecentlyViewedChannel(channel: Channel) // Keep signature
    suspend fun getRecentlyViewedChannels(): Flow<List<Channel>> // Returns Domain Model
    suspend fun fetchAndParseM3uUrl(userId: Int, playlistName: String, m3uUrl: String): Resources<Boolean>
    suspend fun saveM3uData(userId: Int, playlistName: String, channels: List<Channel>)
    // --- New Paging Method ---
    fun getChannelsPager(categoryId: String?, searchQuery: String?): Flow<PagingData<Channel>>

    // --- New Count Methods ---
    suspend fun getTotalChannelCount(): Int
    suspend fun getCategoryChannelCount(categoryId: String): Int
    suspend fun getCatchUpChannelCount(): Int
    // New function to get all channel categories from local database
    fun getAllChannelCategories(userId: Int): Flow<List<Category>>

    // --- Default Category Methods ---
    /**
     * Get the default channel category ID for the current user.
     * Returns the category ID as a string, or null if no default is set.
     */
    suspend fun getDefaultChannelCategoryId(): String?

    /**
     * Get the default custom group ID for the current user.
     * Returns the group ID formatted as "custom_group_{id}", or null if no default is set.
     */
    suspend fun getDefaultCustomGroupId(): String?

    /**
     * Get all channels as a Flow for the current user.
     * This is an alias for fetchChannelsFromDB() for clarity.
     */
    fun getAllChannelsFlow(): Flow<List<Channel>>

    /**
     * Get all channel categories (including hidden) for parental control.
     */
    fun getAllChannelCategoriesForParentalControl(userId: Int): Flow<List<Category>>
}
