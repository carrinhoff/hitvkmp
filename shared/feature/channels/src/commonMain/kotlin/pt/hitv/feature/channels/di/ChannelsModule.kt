package pt.hitv.feature.channels.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.ui.customgroups.AddChannelsViewModel
import pt.hitv.core.ui.customgroups.CustomGroupsViewModel
import pt.hitv.feature.channels.StreamViewModel

val channelsFeatureModule: Module = module {
    single {
        StreamViewModel(
            userSessionManager = get(),
            repository = get(),
            preferencesHelper = get(),
            accountManagerRepository = get(),
            getChannelsByCategoryUseCase = get(),
            toggleFavoriteChannelUseCase = get(),
            searchHistoryRepository = get(),
            syncStateManager = get()
        )
    }
    // Custom Groups feature — wired into the Channels tab's category sheet.
    single { CustomGroupsViewModel(customGroupRepository = get()) }
    single { AddChannelsViewModel(customGroupRepository = get()) }
}
