package pt.hitv.feature.movies.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.feature.movies.list.MovieViewModel

/**
 * Koin module for the movies feature.
 */
val moviesFeatureModule: Module = module {

    factory {
        MovieViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            getMoviesPagerUseCase = get(),
            searchMoviesUseCase = get(),
            toggleFavoriteMovieUseCase = get()
        )
    }
}
