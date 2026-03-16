package pt.hitv.core.network.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pt.hitv.core.network.api.M3uDownloadService
import pt.hitv.core.network.api.MovieApiService
import pt.hitv.core.network.api.StreamApiService
import pt.hitv.core.network.api.TvShowApiService
import pt.hitv.core.network.datasource.M3uRemoteDataSource
import pt.hitv.core.network.datasource.MovieRemoteDataSource
import pt.hitv.core.network.datasource.StreamRemoteDataSource
import pt.hitv.core.network.datasource.TvShowRemoteDataSource
import pt.hitv.core.network.plugin.SslFallbackPlugin

/**
 * Koin module providing the Ktor HttpClient, API services, and remote data sources.
 * Replaces the Hilt/Dagger NetworkModule.
 */
val networkModule = module {

    // JSON configuration
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            coerceInputValues = true
            explicitNulls = false
        }
    }

    // Ktor HttpClient
    single {
        HttpClient(createPlatformHttpEngine()) {
            install(ContentNegotiation) {
                json(get<Json>())
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.HEADERS
            }

            install(SslFallbackPlugin)
        }
    }

    // API Services
    single { StreamApiService(get()) }
    single { MovieApiService(get()) }
    single { TvShowApiService(get()) }
    single { M3uDownloadService(get()) }

    // Remote Data Sources
    single { StreamRemoteDataSource(get(), get()) }
    single {
        MovieRemoteDataSource(
            movieApiService = get(),
            preferencesHelper = get(),
            tmdbApiKey = getOrNull(named("tmdbApiKey")) ?: ""
        )
    }
    single { TvShowRemoteDataSource(get(), get()) }
    single { M3uRemoteDataSource(get()) }
}
