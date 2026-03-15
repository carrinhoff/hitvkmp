package pt.hitv.core.domain.usecases

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.TvShow

class GetSeriesPagerUseCase(
    private val tvShowRepository: TvShowRepository
) {
    operator fun invoke(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String = "added",
        isAscending: Boolean = false
    ): Flow<PagingData<TvShow>> {
        return tvShowRepository.getTvShowsPager(categoryId, searchQuery, sortOrder, isAscending)
    }
}
