package pt.hitv.core.domain.usecases

import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.Movie

class SearchMoviesUseCase(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 500): List<Movie> {
        return movieRepository.searchMoviesWithFallback(query, limit)
    }
}
