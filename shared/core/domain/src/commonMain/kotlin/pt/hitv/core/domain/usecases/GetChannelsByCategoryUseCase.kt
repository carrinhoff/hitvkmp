package pt.hitv.core.domain.usecases

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.Channel

class GetChannelsByCategoryUseCase(
    private val streamRepository: StreamRepository
) {
    operator fun invoke(
        categoryId: String?,
        searchQuery: String?
    ): Flow<PagingData<Channel>> {
        return streamRepository.getChannelsPager(categoryId, searchQuery)
    }
}
