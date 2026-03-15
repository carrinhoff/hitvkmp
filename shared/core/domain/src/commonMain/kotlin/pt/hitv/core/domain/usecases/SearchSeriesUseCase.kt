package pt.hitv.core.domain.usecases

import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.TvShow

class SearchSeriesUseCase(
    private val tvShowRepository: TvShowRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 500): List<TvShow> {
        return tvShowRepository.searchTvShowsWithFallback(query, limit)
    }
}
