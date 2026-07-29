package pt.hitv.core.sync.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.sync.BackgroundSyncBootstrapper
import pt.hitv.core.sync.SyncManager
import pt.hitv.core.sync.SyncManagerImpl
import pt.hitv.core.sync.SyncStateManager

/**
 * Platform-specific Koin module for sync scheduling.
 * Each platform provides the actual SyncScheduler binding.
 */
expect val syncPlatformModule: Module

/**
 * Common sync module that provides SyncManager and SyncStateManager.
 */
val syncModule: Module = module {
    single { SyncStateManager() }
    single<SyncManager> {
        SyncManagerImpl(
            syncScheduler = get(),
            streamRepository = get(),
            movieRepository = get(),
            tvShowRepository = get(),
            preferencesHelper = get()
        )
    }
    // Re-arms the OS periodic tasks at app start when the user has sync enabled. Lives here
    // rather than in the settings feature so the boot path can resolve it — see
    // BackgroundSyncBootstrapper's KDoc for why the old location silently stopped scheduling.
    single {
        BackgroundSyncBootstrapper(
            preferencesHelper = get(),
            backgroundSyncManager = get()
        )
    }
}
