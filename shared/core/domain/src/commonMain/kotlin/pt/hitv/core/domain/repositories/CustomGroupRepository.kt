package pt.hitv.core.domain.repositories

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.Channel
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.model.CustomGroupWithChannels

interface CustomGroupRepository {

    // Custom Group Management
    suspend fun createCustomGroup(name: String, icon: String? = null): Long
    suspend fun updateCustomGroup(group: CustomGroup)
    suspend fun deleteCustomGroup(groupId: Long)
    fun getAllCustomGroups(): Flow<List<CustomGroup>>
    suspend fun getCustomGroupById(groupId: Long): CustomGroup?
    suspend fun getCustomGroupWithChannels(groupId: Long): CustomGroupWithChannels?

    // Channel Management in Groups
    suspend fun addChannelToGroup(groupId: Long, channelId: Long, channelUserId: Int, position: Int)
    suspend fun addChannelsToGroup(groupId: Long, channels: List<Pair<Long, Int>>) // Pair<channelId, userId>
    suspend fun removeChannelFromGroup(groupId: Long, channelId: Long)
    suspend fun removeAllChannelsFromGroup(groupId: Long)
    suspend fun reorderChannelsInGroup(groupId: Long, channelIds: List<Long>)
    suspend fun isChannelInGroup(groupId: Long, channelId: Long): Boolean

    // Channel Queries (Cross-userId)
    suspend fun getChannelsInGroup(groupId: Long): List<Channel>
    fun getChannelsInGroupPaged(groupId: Long): Flow<PagingData<Channel>>
    suspend fun getChannelCountInGroup(groupId: Long): Int

    // Search Channels (All playlists)
    fun searchAllChannels(query: String): Flow<PagingData<Channel>>
    fun getAllChannels(): Flow<PagingData<Channel>>

    // List-based channel queries (non-paged, for KMP compatibility)
    /**
     * Reads the **entire** Channel table into memory. No current caller, and new ones should use
     * [getAllChannels] instead.
     *
     * The Add Channels screen used this and held the result in a StateFlow: on a 50k-channel
     * account that is 50k domain objects resident for as long as the screen is open — the memory
     * profile iOS terminates apps over. Kept only because a bounded, deliberate use is conceivable;
     * it is not a general-purpose accessor.
     */
    suspend fun getAllChannelsList(): List<Channel>

    /** Unbounded, like [getAllChannelsList]. Prefer [searchAllChannels], which is paged. */
    suspend fun searchAllChannelsList(query: String): List<Channel>

    // Maintenance
    suspend fun cleanupOrphanedChannels(): Int
}
