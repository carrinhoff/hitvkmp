package pt.hitv.core.data.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.data.manager.ParentalControlManagerImpl
import pt.hitv.core.data.manager.UserSessionManager
import pt.hitv.core.data.parser.M3uParser
import pt.hitv.core.data.repository.AccountManagerRepositoryImpl
import pt.hitv.core.data.repository.CategoryPreferenceRepositoryImpl
import pt.hitv.core.data.repository.CustomGroupRepositoryImpl
import pt.hitv.core.data.repository.SearchHistoryRepositoryImpl
import pt.hitv.core.data.repository.MovieRepositoryImpl
import pt.hitv.core.data.repository.ParentalControlRepositoryImpl
import pt.hitv.core.data.repository.StreamRepositoryImpl
import pt.hitv.core.data.repository.TvShowRepositoryImpl
import pt.hitv.core.domain.manager.ParentalControlManager
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.usecases.GetChannelsByCategoryUseCase
import pt.hitv.core.domain.usecases.GetMoviesPagerUseCase
import pt.hitv.core.domain.usecases.GetSeriesPagerUseCase
import pt.hitv.core.domain.usecases.SearchMoviesUseCase
import pt.hitv.core.domain.usecases.SearchSeriesUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteChannelUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteMovieUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteSeriesUseCase
import pt.hitv.core.domain.repositories.CategoryPreferenceRepository
import pt.hitv.core.domain.repositories.CustomGroupRepository
import pt.hitv.core.domain.repositories.SearchHistoryRepository
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.domain.repositories.ParentalControlRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.domain.repositories.TvShowRepository

/**
 * Koin module providing all data layer dependencies:
 * repositories, managers, parsers, and utilities.
 *
 * Platform-specific modules must provide:
 * - CryptoManager (via platformDataModule)
 * - PremiumStatusProvider
 * - PairingAnalyticsTracker
 * - Firebase DatabaseReference for pairingSessions
 */
val dataModule: Module = module {

    // ==================== Parsers ====================

    single { M3uParser() }

    // ==================== Managers ====================

    single<ParentalControlManager> {
        ParentalControlManagerImpl(
            preferencesHelper = get(),
            parentalControlQueries = get(),
            premiumStatusProvider = get()
        )
    }

    single {
        UserSessionManager(
            preferencesHelper = get(),
            dataStore = get()
        )
    }

    // ==================== Repositories ====================

    single<StreamRepository> {
        StreamRepositoryImpl(
            streamRemoteDataSource = get(),
            m3uRemoteDataSource = get(),
            channelQueries = get(),
            categoryQueries = get(),
            customGroupQueries = get(),
            epgChannelQueries = get(),
            programmeQueries = get(),
            userCredentialsQueries = get(),
            database = get(),
            preferencesHelper = get(),
            parentalControlManager = get(),
            m3uParser = get()
        )
    }

    single<MovieRepository> {
        MovieRepositoryImpl(
            movieRemoteDataSource = get(),
            movieQueries = get(),
            categoryVodQueries = get(),
            movieInfoQueries = get(),
            database = get(),
            preferencesHelper = get()
        )
    }

    single<TvShowRepository> {
        TvShowRepositoryImpl(
            tvShowRemoteDataSource = get(),
            tvShowQueries = get(),
            categoryTvShowQueries = get(),
            seriesInfoQueries = get(),
            database = get(),
            preferencesHelper = get()
        )
    }

    single<AccountManagerRepository> {
        AccountManagerRepositoryImpl(
            userCredentialsQueries = get(),
            channelQueries = get(),
            categoryQueries = get(),
            epgChannelQueries = get(),
            programmeQueries = get(),
            movieQueries = get(),
            movieInfoQueries = get(),
            categoryVodQueries = get(),
            tvShowQueries = get(),
            seriesInfoQueries = get(),
            categoryTvShowQueries = get(),
            cryptoManager = get(),
            preferencesHelper = get()
        )
    }

    single<CategoryPreferenceRepository> {
        CategoryPreferenceRepositoryImpl(
            categoryQueries = get(),
            categoryVodQueries = get(),
            categoryTvShowQueries = get(),
            customGroupQueries = get(),
            preferencesHelper = get()
        )
    }

    single<CustomGroupRepository> {
        CustomGroupRepositoryImpl(
            customGroupQueries = get(),
            channelQueries = get()
        )
    }

    single<SearchHistoryRepository> {
        SearchHistoryRepositoryImpl(database = get())
    }

    single<ParentalControlRepository> {
        ParentalControlRepositoryImpl(
            parentalControlQueries = get()
        )
    }

    // ==================== Use Cases ====================

    factory { GetChannelsByCategoryUseCase(streamRepository = get()) }
    factory { GetMoviesPagerUseCase(movieRepository = get()) }
    factory { GetSeriesPagerUseCase(tvShowRepository = get()) }
    factory { SearchMoviesUseCase(movieRepository = get()) }
    factory { SearchSeriesUseCase(tvShowRepository = get()) }
    factory { ToggleFavoriteChannelUseCase(streamRepository = get()) }
    factory { ToggleFavoriteMovieUseCase(movieRepository = get()) }
    factory { ToggleFavoriteSeriesUseCase(tvShowRepository = get()) }
}
