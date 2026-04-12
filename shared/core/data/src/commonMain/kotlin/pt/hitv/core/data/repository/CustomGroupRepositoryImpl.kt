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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.data.mapper.toChannel
import pt.hitv.core.data.util.SearchUtils
import pt.hitv.core.database.CustomGroupQueries
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.model.Channel
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.model.CustomGroupWithChannels
import pt.hitv.core.domain.repositories.CustomGroupRepository

class CustomGroupRepositoryImpl(
    private val customGroupQueries: CustomGroupQueries,
    private val channelQueries: ChannelQueries
) : CustomGroupRepository {

    // ========== Custom Group Management ==========

    override suspend fun createCustomGroup(name: String, icon: String?): Long {
        return withContext(Dispatchers.IO) {
            val groupCount = customGroupQueries.countGroups().executeAsOne()
            val now = Clock.System.now().toEpochMilliseconds()
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

    override suspend fun deleteCustomGroup(groupId: Long) {
        withContext(Dispatchers.IO) {
            customGroupQueries.removeAllChannelsFromGroup(groupId)
            customGroupQueries.deleteGroup(groupId)
        }
    }

    override fun getAllCustomGroups(): Flow<List<CustomGroup>> {
        return flow {
            val entities = customGroupQueries.selectAllGroups().executeAsList()
            val groups = entities.map { entity ->
                val channelCount = customGroupQueries.countChannelsInGroup(entity.groupId)
                    .executeAsOne().toInt()
                CustomGroup(
                    id = entity.groupId,
                    name = entity.groupName,
                    icon = entity.groupIcon,
                    channelCount = channelCount,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    sortOrder = entity.sortOrder.toInt(),
                    isPinned = entity.isPinned != 0L,
                    isHidden = entity.isHidden != 0L,
                    isDefault = entity.isDefault != 0L
                )
            }
            emit(groups)
        }.flowOn(Dispatchers.IO)
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

    override suspend fun addChannelsToGroup(groupId: Long, channels: List<Pair<Long, Int>>) {
        withContext(Dispatchers.IO) {
            val now = Clock.System.now().toEpochMilliseconds()
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

    override suspend fun reorderChannelsInGroup(groupId: Long, channelIds: List<Long>) {
        withContext(Dispatchers.IO) {
            val groupChannels = customGroupQueries.selectGroupChannels(groupId).executeAsList()
            channelIds.forEachIndexed { index, channelId ->
                val existing = groupChannels.find { it.channelId == channelId }
                if (existing != null) {
                    customGroupQueries.updateChannelPosition(index.toLong(), existing.id)
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
                val words = SearchUtils.normalizeSearchWords(query)
                val likePattern = "%${words.joinToString("%")}%"
                channelQueries.searchByName(0L, likePattern, Long.MAX_VALUE, 0L)
                    .executeAsList()
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
                val words = SearchUtils.normalizeSearchWords(query)
                val likePattern = "%${words.joinToString("%")}%"
                // Search across all users' channels
                val entities = channelQueries.searchByName(0L, likePattern, pageSize.toLong(), offset.toLong())
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
