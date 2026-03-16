package pt.hitv.feature.channels.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.feature.channels.StreamViewModel

/**
 * Koin module for the channels feature.
 */
val channelsFeatureModule: Module = module {

    factory {
        StreamViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            accountManagerRepository = get(),
            getChannelsByCategoryUseCase = get(),
            toggleFavoriteChannelUseCase = get()
        )
    }
}
