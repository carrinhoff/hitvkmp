package pt.hitv.feature.movies.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.feature.movies.detail.MovieInfoViewModel
import pt.hitv.feature.movies.list.MovieViewModel

val moviesFeatureModule: Module = module {
    single {
        MovieViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            getMoviesPagerUseCase = get(),
            searchMoviesUseCase = get(),
            toggleFavoriteMovieUseCase = get()
        )
    }

    factory {
        MovieInfoViewModel(
            repository = get(),
            analyticsHelper = get(),
            preferencesHelper = get()
        )
    }
}
