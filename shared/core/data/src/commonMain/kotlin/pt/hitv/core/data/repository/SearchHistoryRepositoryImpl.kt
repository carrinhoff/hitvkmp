package pt.hitv.core.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.domain.repositories.SearchHistoryRepository
import pt.hitv.core.model.SearchHistoryItem

/**
 * Snapshot-based implementation: the queries run once on subscription and again
 * whenever [mutations] emits. Mutations trigger a fresh snapshot — this avoids
 * pulling in `sqldelight-coroutines-extensions`, which isn't declared in this module.
 */
class SearchHistoryRepositoryImpl(
    private val database: HitvDatabase
) : SearchHistoryRepository {

    private val queries get() = database.searchHistoryQueries
    private val mutations = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    override fun observe(userId: Int, kind: String, limit: Int): Flow<List<SearchHistoryItem>> =
        flow {
            emit(load(userId, kind, limit))
            mutations.collect { emit(load(userId, kind, limit)) }
        }

    private suspend fun load(userId: Int, kind: String, limit: Int): List<SearchHistoryItem> =
        withContext(Dispatchers.Default) {
            queries.selectRecentByUserAndKind(userId.toLong(), kind, limit.toLong())
                .executeAsList()
                .map {
                    SearchHistoryItem(
                        id = it.id,
                        userId = it.userId.toInt(),
                        query = it.query,
                        kind = it.kind,
                        timestamp = it.timestamp
                    )
                }
        }

    override suspend fun add(userId: Int, kind: String, query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        withContext(Dispatchers.Default) {
            queries.insertOrReplace(
                userId = userId.toLong(),
                kind = kind,
                query = trimmed,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            queries.trimKeepingNewest(
                userId = userId.toLong(),
                kind = kind,
                userId_ = userId.toLong(),
                kind_ = kind,
                value = 10L
            )
        }
        mutations.tryEmit(Unit)
    }

    override suspend fun deleteById(id: Long) {
        withContext(Dispatchers.Default) { queries.deleteById(id) }
        mutations.tryEmit(Unit)
    }

    override suspend fun clear(userId: Int, kind: String) {
        withContext(Dispatchers.Default) { queries.deleteAllByUserAndKind(userId.toLong(), kind) }
        mutations.tryEmit(Unit)
    }
}
