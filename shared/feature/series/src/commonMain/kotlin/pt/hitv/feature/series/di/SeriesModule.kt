package pt.hitv.feature.series.di

import org.koin.dsl.module
import pt.hitv.feature.series.detail.SeriesInfoViewModel
import pt.hitv.feature.series.list.SeriesViewModel

val seriesModule = module {
    single {
        SeriesViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            getSeriesPagerUseCase = get(),
            searchSeriesUseCase = get(),
            toggleFavoriteSeriesUseCase = get()
        )
    }

    factory {
        SeriesInfoViewModel(
            repository = get(),
            analyticsHelper = get()
        )
    }
}
