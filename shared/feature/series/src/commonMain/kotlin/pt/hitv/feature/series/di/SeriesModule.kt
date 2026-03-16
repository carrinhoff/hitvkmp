package pt.hitv.feature.series.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pt.hitv.feature.series.list.SeriesViewModel

val seriesModule = module {
    viewModel {
        SeriesViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            getSeriesPagerUseCase = get(),
            searchSeriesUseCase = get(),
            toggleFavoriteSeriesUseCase = get()
        )
    }
}
