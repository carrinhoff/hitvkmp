package pt.hitv.feature.player.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pt.hitv.feature.player.LivePlayerViewModel
import pt.hitv.feature.player.movies.MoviePlayerViewModel
import pt.hitv.feature.player.series.SeriesPlayerViewModel

val playerModule = module {
    viewModel { LivePlayerViewModel(repository = get(), preferencesHelper = get(), accountManagerRepository = get()) }
    viewModel { MoviePlayerViewModel(movieRepository = get()) }
    viewModel { SeriesPlayerViewModel(repository = get()) }
    // Platform-specific player implementations are registered in androidMain/iosMain
}
