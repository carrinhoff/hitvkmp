package pt.hitv.core.sync.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.sync.AndroidSyncScheduler
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.SyncScheduler

actual val syncPlatformModule: Module = module {
    single<SyncScheduler> { AndroidSyncScheduler(context = get()) }
    single { BackgroundSyncManager() }
}
