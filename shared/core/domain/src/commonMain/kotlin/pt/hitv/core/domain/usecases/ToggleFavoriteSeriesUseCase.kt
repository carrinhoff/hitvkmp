package pt.hitv.core.domain.usecases

import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.TvShow

class ToggleFavoriteSeriesUseCase(
    private val tvShowRepository: TvShowRepository
) {
    suspend operator fun invoke(tvShow: TvShow) {
        tvShowRepository.saveFavoriteTvShow(tvShow)
    }
}
