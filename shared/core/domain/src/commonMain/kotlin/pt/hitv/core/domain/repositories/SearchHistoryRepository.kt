package pt.hitv.core.domain.repositories

import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.SearchHistoryItem

interface SearchHistoryRepository {
    fun observe(userId: Int, kind: String, limit: Int = 10): Flow<List<SearchHistoryItem>>
    suspend fun add(userId: Int, kind: String, query: String)
    suspend fun deleteById(id: Long)
    suspend fun clear(userId: Int, kind: String)

    companion object {
        const val KIND_CHANNEL = "channel"
        const val KIND_MOVIE = "movie"
        const val KIND_SERIES = "series"
    }
}
