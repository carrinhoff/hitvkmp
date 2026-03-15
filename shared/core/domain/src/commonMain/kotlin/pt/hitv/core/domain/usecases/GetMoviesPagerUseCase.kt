package pt.hitv.core.domain.usecases

import app.cash.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.Movie

class GetMoviesPagerUseCase(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(
        categoryId: String?,
        searchQuery: String?,
        sortOrder: String = "added",
        isAscending: Boolean = false
    ): Flow<PagingData<Movie>> {
        return movieRepository.getMoviesPager(categoryId, searchQuery, sortOrder, isAscending)
    }
}
