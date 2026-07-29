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
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.data.mapper.toChannel
import app.cash.sqldelight.db.SqlDriver
import pt.hitv.core.data.paging.PagedTables
import pt.hitv.core.data.paging.invalidateOnChangeTo
import pt.hitv.core.data.util.SearchUtils
import pt.hitv.core.database.CustomGroupQueries
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.model.Channel
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.model.CustomGroupWithChannels
import pt.hitv.core.domain.repositories.CustomGroupRepository

class CustomGroupRepositoryImpl(
    private val customGroupQueries: CustomGroupQueries,
    private val channelQueries: ChannelQueries,
    private val driver: SqlDriver
) : CustomGroupRepository {

    // ========== Custom Group Management ==========

    override suspend fun createCustomGroup(name: String, icon: String?): Long {
        return withContext(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            // count -> insert -> read-back-the-id is a read-modify-write; outside a transaction two
            // groups created in quick succession can take the same sortOrder, and the id read back
            // is not guaranteed to be the one just inserted.
            customGroupQueries.transactionWithResult {
                val groupCount = customGroupQueries.countGroups().executeAsOne()
                customGroupQueries.insertGroup(
                    groupName = name,
                    groupIcon = icon,
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = groupCount,
                    isPinned = 0L,
                    isHidden = 0L,
                    isDefault = 0L
                )
                customGroupQueries.lastInsertGroupId().executeAsOne().MAX ?: 0L
            }
        }
    }

    override suspend fun updateCustomGroup(group: CustomGroup) {
        withContext(Dispatchers.IO) {
            customGroupQueries.updateGroup(
                groupName = group.name,
                groupIcon = group.icon,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                sortOrder = group.sortOrder.toLong(),
                isPinned = if (group.isPinned) 1L else 0L,
                isHidden = if (group.isHidden) 1L else 0L,
                isDefault = if (group.isDefault) 1L else 0L,
                groupId = group.id
            )
        }
    }

    /**
     * Atomic, matching `deleteCustomGroupWithChannels` (`@Transaction`) in the original.
     * Unwrapped, a failure between the two statements left an empty group behind — or, in the
     * other order, membership rows pointing at a group that no longer exists.
     */
    override suspend fun deleteCustomGroup(groupId: Long) {
        withContext(Dispatchers.IO) {
            customGroupQueries.transaction {
                customGroupQueries.removeAllChannelsFromGroup(groupId)
                customGroupQueries.deleteGroup(groupId)
            }
        }
    }

    override fun getAllCustomGroups(): Flow<List<CustomGroup>> {
        // Reactive, and a single query. Previously this emitted once and issued one count query
        // per group, so creating or deleting a group — or adding a channel to one — left the list
        // showing the state from whenever the screen was first composed.
        return customGroupQueries.selectAllGroupsWithChannelCount()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map { row ->
                    CustomGroup(
                        id = row.groupId,
                        name = row.groupName,
                        icon = row.groupIcon,
                        channelCount = row.channelCount.toInt(),
                        createdAt = row.createdAt,
                        updatedAt = row.updatedAt,
                        sortOrder = row.sortOrder.toInt(),
                        isPinned = row.isPinned != 0L,
                        isHidden = row.isHidden != 0L,
                        isDefault = row.isDefault != 0L
                    )
                }
            }
    }

    override suspend fun getCustomGroupById(groupId: Long): CustomGroup? {
        return withContext(Dispatchers.IO) {
            try {
                val entity = customGroupQueries.selectGroupById(groupId).executeAsOneOrNull()
                entity?.let {
                    val channelCount = customGroupQueries.countChannelsInGroup(it.groupId)
                        .executeAsOne().toInt()
                    CustomGroup(
                        id = it.groupId,
                        name = it.groupName,
                        icon = it.groupIcon,
                        channelCount = channelCount,
                        createdAt = it.createdAt,
                        updatedAt = it.updatedAt,
                        sortOrder = it.sortOrder.toInt(),
                        isPinned = it.isPinned != 0L,
                        isHidden = it.isHidden != 0L,
                        isDefault = it.isDefault != 0L
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun getCustomGroupWithChannels(groupId: Long): CustomGroupWithChannels? {
        return withContext(Dispatchers.IO) {
            try {
                val group = getCustomGroupById(groupId) ?: return@withContext null
                val channels = getChannelsInGroup(groupId)
                CustomGroupWithChannels(group = group, channels = channels)
            } catch (e: Exception) {
                null
            }
        }
    }

    // ========== Channel Management in Groups ==========

    override suspend fun addChannelToGroup(groupId: Long, channelId: Long, channelUserId: Int, position: Int) {
        withContext(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            customGroupQueries.addChannelToGroup(
                groupId = groupId,
                channelId = channelId,
                channelUserId = channelUserId.toLong(),
                position = position.toLong(),
                addedAt = now
            )
        }
    }

    /**
     * One transaction, as in the original's `replaceChannelsInGroup`. Besides atomicity this is a
     * large win on the write path — adding a few hundred channels to a group was a few hundred
     * separate commits — and it collapses the change notifications into one, so the group list
     * refreshes once instead of once per channel.
     */
    override suspend fun addChannelsToGroup(groupId: Long, channels: List<Pair<Long, Int>>) {
        withContext(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
            customGroupQueries.transaction {
                channels.forEachIndexed { index, (channelId, userId) ->
                    customGroupQueries.addChannelToGroup(
                        groupId = groupId,
                        channelId = channelId,
                        channelUserId = userId.toLong(),
                        position = index.toLong(),
                        addedAt = now
                    )
                }
            }
        }
    }

    override suspend fun removeChannelFromGroup(groupId: Long, channelId: Long) {
        withContext(Dispatchers.IO) {
            customGroupQueries.removeChannelFromGroupById(groupId, channelId)
        }
    }

    override suspend fun removeAllChannelsFromGroup(groupId: Long) {
        withContext(Dispatchers.IO) {
            customGroupQueries.removeAllChannelsFromGroup(groupId)
        }
    }

    /**
     * Atomic, matching `reorderChannelsInGroup` (`@Transaction`) in the original. A partial reorder
     * is worse than none: positions collide and the list settles into an order the user did not
     * choose. The read of the current rows belongs inside the transaction too, otherwise it can be
     * invalidated by a concurrent write before the updates land.
     */
    override suspend fun reorderChannelsInGroup(groupId: Long, channelIds: List<Long>) {
        withContext(Dispatchers.IO) {
            customGroupQueries.transaction {
                val groupChannels = customGroupQueries.selectGroupChannels(groupId).executeAsList()
                channelIds.forEachIndexed { index, channelId ->
                    val existing = groupChannels.find { it.channelId == channelId }
                    if (existing != null) {
                        customGroupQueries.updateChannelPosition(index.toLong(), existing.id)
                    }
                }
            }
        }
    }

    override suspend fun isChannelInGroup(groupId: Long, channelId: Long): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                (customGroupQueries.isChannelInGroup(groupId, channelId).executeAsOne() as Long) > 0L
            } catch (e: Exception) {
                false
            }
        }
    }

    // ========== Channel Queries ==========

    override suspend fun getChannelsInGroup(groupId: Long): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                customGroupQueries.selectChannelsInGroup(groupId)
                    .executeAsList()
                    .map { it.toChannel() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun getChannelsInGroupPaged(groupId: Long): Flow<PagingData<Channel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 60,
                prefetchDistance = 6,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                CustomGroupChannelPagingSource(customGroupQueries, groupId)
                    .also { it.invalidateOnChangeTo(driver, PagedTables.CUSTOM_GROUP_CHANNEL, PagedTables.CHANNEL) }
            }
        ).flow
    }

    override suspend fun getChannelCountInGroup(groupId: Long): Int {
        return withContext(Dispatchers.IO) {
            try {
                customGroupQueries.countChannelsInGroup(groupId).executeAsOne().toInt()
            } catch (e: Exception) {
                0
            }
        }
    }

    // ========== Search Channels ==========

    override fun searchAllChannels(query: String): Flow<PagingData<Channel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 60,
                prefetchDistance = 6,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                AllChannelsSearchPagingSource(channelQueries, query)
                    .also { it.invalidateOnChangeTo(driver, PagedTables.CHANNEL) }
            }
        ).flow
    }

    override fun getAllChannels(): Flow<PagingData<Channel>> {
        return Pager(
            config = PagingConfig(
                pageSize = 30,
                initialLoadSize = 60,
                prefetchDistance = 6,
                enablePlaceholders = true
            ),
            pagingSourceFactory = {
                AllChannelsPagingSource(customGroupQueries)
                    .also { it.invalidateOnChangeTo(driver, PagedTables.CHANNEL) }
            }
        ).flow
    }

    // ========== List-based Channel Queries ==========

    override suspend fun getAllChannelsList(): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                customGroupQueries.selectAllChannelsPaged(Long.MAX_VALUE, 0L)
                    .executeAsList()
                    .map { it.toChannel() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override suspend fun searchAllChannelsList(query: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                // Across every account and word-order independent, as in the original. The old
                // call passed userId = 0 to the single-user query, which matches no row (userId
                // is AUTOINCREMENT, so it starts at 1), leaving this search permanently empty.
                val slots = SearchUtils.flexibleLikeSlots(query)
                val overflow = SearchUtils.overflowSearchWords(query)
                channelQueries.searchAllByNameFlexible(
                    w1 = slots[0], w2 = slots[1], w3 = slots[2],
                    w4 = slots[3], w5 = slots[4], w6 = slots[5],
                    limit = Long.MAX_VALUE,
                    offset = 0L,
                )
                    .executeAsList()
                    .filter { SearchUtils.matchesOverflowWords(it.name, overflow) }
                    .map { it.toChannel() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ========== Maintenance ==========

    override suspend fun cleanupOrphanedChannels(): Int {
        return withContext(Dispatchers.IO) {
            try {
                customGroupQueries.cleanupOrphanedChannels()
                // SQLDelight doesn't return affected rows directly; estimate from count
                0
            } catch (e: Exception) {
                0
            }
        }
    }

    // ========== Paging Sources ==========

    private class CustomGroupChannelPagingSource(
        private val queries: CustomGroupQueries,
        private val groupId: Long
    ) : PagingSource<Int, Channel>() {
        override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Channel> {
            return try {
                val page = params.key ?: 0
                val pageSize = params.loadSize
                val offset = page * pageSize
                val entities = queries.selectChannelsInGroupPaged(groupId, pageSize.toLong(), offset.toLong())
                    .executeAsList()
                val channels = entities.map { it.toChannel() }
                PagingSourceLoadResultPage(
                    data = channels,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (channels.isEmpty()) null else page + 1
                )
            } catch (e: Exception) {
                PagingSourceLoadResultError(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Channel>): Int? {
            return state.anchorPosition?.let { pos ->
                state.closestPageToPosition(pos)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
            }
        }
    }

    private class AllChannelsSearchPagingSource(
        private val channelQueries: ChannelQueries,
        private val query: String
    ) : PagingSource<Int, Channel>() {
        override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Channel> {
            return try {
                val page = params.key ?: 0
                val pageSize = params.loadSize
                val offset = page * pageSize
                // Search across all users' channels, word-order independent -- see
                // searchAllChannelsList above for why the previous userId = 0 call never matched.
                val slots = SearchUtils.flexibleLikeSlots(query)
                val overflow = SearchUtils.overflowSearchWords(query)
                val entities = channelQueries.searchAllByNameFlexible(
                    w1 = slots[0], w2 = slots[1], w3 = slots[2],
                    w4 = slots[3], w5 = slots[4], w6 = slots[5],
                    limit = pageSize.toLong(),
                    offset = offset.toLong(),
                )
                    .executeAsList()
                    .filter { SearchUtils.matchesOverflowWords(it.name, overflow) }
                val channels = entities.map { it.toChannel() }
                PagingSourceLoadResultPage(
                    data = channels,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (channels.isEmpty()) null else page + 1
                )
            } catch (e: Exception) {
                PagingSourceLoadResultError(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Channel>): Int? {
            return state.anchorPosition?.let { pos ->
                state.closestPageToPosition(pos)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
            }
        }
    }

    private class AllChannelsPagingSource(
        private val queries: CustomGroupQueries
    ) : PagingSource<Int, Channel>() {
        override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, Channel> {
            return try {
                val page = params.key ?: 0
                val pageSize = params.loadSize
                val offset = page * pageSize
                val entities = queries.selectAllChannelsPaged(pageSize.toLong(), offset.toLong())
                    .executeAsList()
                val channels = entities.map { it.toChannel() }
                PagingSourceLoadResultPage(
                    data = channels,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (channels.isEmpty()) null else page + 1
                )
            } catch (e: Exception) {
                PagingSourceLoadResultError(e)
            }
        }

        override fun getRefreshKey(state: PagingState<Int, Channel>): Int? {
            return state.anchorPosition?.let { pos ->
                state.closestPageToPosition(pos)?.prevKey?.plus(1)
                    ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
            }
        }
    }
}
