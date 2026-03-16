package pt.hitv.core.sync.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.sync.SyncManager
import pt.hitv.core.sync.SyncManagerImpl

/**
 * Platform-specific Koin module for sync scheduling.
 * Each platform provides the actual SyncScheduler binding.
 */
expect val syncPlatformModule: Module

/**
 * Common sync module that provides SyncManager using the platform scheduler.
 */
val syncModule: Module = module {
    single<SyncManager> { SyncManagerImpl(syncScheduler = get()) }
}
