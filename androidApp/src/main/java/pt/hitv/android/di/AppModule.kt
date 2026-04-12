package pt.hitv.android.di

import org.koin.core.module.Module
import pt.hitv.core.billing.di.billingPlatformModule
import pt.hitv.core.common.di.commonModule
import pt.hitv.core.data.di.dataModule
import pt.hitv.core.database.di.databaseModule
import pt.hitv.core.database.di.databasePlatformModule
import pt.hitv.core.network.di.networkModule
import pt.hitv.core.sync.di.syncModule
import pt.hitv.core.sync.di.syncPlatformModule
import pt.hitv.feature.auth.di.authFeatureModule
import pt.hitv.feature.channels.di.channelsFeatureModule
import pt.hitv.feature.movies.di.moviesFeatureModule
import pt.hitv.feature.player.di.playerModule
import pt.hitv.feature.premium.di.premiumModule
import pt.hitv.feature.series.di.seriesModule
import pt.hitv.feature.settings.di.settingsModule

/**
 * Root Koin module aggregation for Android.
 * Combines all shared core modules, platform modules, feature modules,
 * and Android-specific bindings into a single list for Koin startup.
 */
fun appModule(): List<Module> = listOf(
    // Core - shared (commonMain)
    commonModule,
    dataModule,
    databaseModule,
    networkModule,
    syncModule,

    // Core - platform (androidMain)
    databasePlatformModule,
    billingPlatformModule,
    syncPlatformModule,

    // Feature modules
    authFeatureModule,
    channelsFeatureModule,
    moviesFeatureModule,
    seriesModule,
    premiumModule,
    settingsModule,
    playerModule,

    // Android-specific bindings
    androidPlatformModule,
    analyticsModule,
)
