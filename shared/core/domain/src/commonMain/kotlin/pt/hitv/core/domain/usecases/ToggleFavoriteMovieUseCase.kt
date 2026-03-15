package pt.hitv.core.domain.usecases

import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.Movie

class ToggleFavoriteMovieUseCase(
    private val movieRepository: MovieRepository
) {
    suspend operator fun invoke(movie: Movie) {
        movieRepository.saveFavoriteMovie(movie)
    }
}
