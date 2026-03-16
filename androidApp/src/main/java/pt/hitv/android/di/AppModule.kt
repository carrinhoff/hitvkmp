package pt.hitv.android.di

import org.koin.core.module.Module
import pt.hitv.core.billing.di.billingPlatformModule
import pt.hitv.core.data.di.dataModule
import pt.hitv.core.database.di.databaseModule
import pt.hitv.core.database.di.databasePlatformModule
import pt.hitv.core.network.di.networkModule
import pt.hitv.core.sync.di.syncModule
import pt.hitv.core.sync.di.syncPlatformModule

/**
 * Root Koin module aggregation for Android.
 * Combines all shared core modules, platform modules, feature modules,
 * and Android-specific bindings into a single list for Koin startup.
 */
fun appModule(): List<Module> = listOf(
    // Core - shared (commonMain)
    dataModule,
    databaseModule,
    networkModule,
    syncModule,

    // Core - platform (androidMain)
    databasePlatformModule,
    billingPlatformModule,
    syncPlatformModule,

    // Android-specific bindings
    androidPlatformModule,
    analyticsModule,
)
