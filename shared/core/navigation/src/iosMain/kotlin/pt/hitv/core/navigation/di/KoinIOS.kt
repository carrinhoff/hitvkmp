package pt.hitv.core.navigation.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.NoOpAnalyticsHelper
import pt.hitv.core.common.crashreporting.CrashReportingHelper
import pt.hitv.core.common.crashreporting.NoOpCrashReportingHelper
import pt.hitv.core.common.featureflags.FeatureFlagManager
import pt.hitv.core.common.featureflags.NoOpFeatureFlagManager
import pt.hitv.core.billing.di.billingPlatformModule
import pt.hitv.core.data.di.dataModule
import pt.hitv.core.database.di.databaseModule
import pt.hitv.core.database.di.databasePlatformModule
import pt.hitv.core.network.di.networkModule
import pt.hitv.core.sync.di.syncModule
import pt.hitv.core.sync.di.syncPlatformModule
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.data.manager.PairingAnalyticsTracker
import pt.hitv.core.common.di.commonModule
// Feature modules
import pt.hitv.feature.auth.di.authFeatureModule
import pt.hitv.feature.channels.di.channelsFeatureModule
import pt.hitv.feature.movies.di.moviesFeatureModule
import pt.hitv.feature.series.di.seriesModule
import pt.hitv.feature.player.di.playerModule
import pt.hitv.feature.premium.di.premiumModule
import pt.hitv.feature.settings.di.settingsModule
// Screen registrations
import pt.hitv.feature.auth.navigation.registerAuthScreens
import pt.hitv.feature.channels.navigation.registerChannelsScreens
import pt.hitv.feature.movies.navigation.registerMoviesScreens
import pt.hitv.feature.series.navigation.registerSeriesScreens
import pt.hitv.feature.premium.navigation.registerPremiumScreens
import pt.hitv.feature.settings.navigation.registerSettingsScreens

/**
 * Initialize Koin for iOS.
 *
 * Called from Swift via `KoinIOSKt.doInitKoinIOS()`.
 * Registers all Voyager screens and starts Koin with all modules.
 */
fun initKoinIOS() {
    // Register Voyager screen factories (must be before Koin)
    registerAuthScreens()
    registerChannelsScreens()
    registerMoviesScreens()
    registerSeriesScreens()
    registerPremiumScreens()
    registerSettingsScreens()

    startKoin {
        modules(
            // Core - shared (commonMain)
            commonModule,
            dataModule,
            databaseModule,
            networkModule,
            syncModule,

            // Core - platform (iosMain)
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

            // iOS-specific bindings
            iosPlatformModule,
        )
    }
}

/**
 * iOS platform module providing NoOp implementations.
 */
val iosPlatformModule = module {
    single<AnalyticsHelper> { NoOpAnalyticsHelper() }
    single<CrashReportingHelper> { NoOpCrashReportingHelper() }
    single<FeatureFlagManager> { NoOpFeatureFlagManager() }
    single { ThemeManager(preferencesHelper = get()) }
    single<PairingAnalyticsTracker> {
        object : PairingAnalyticsTracker {
            override fun logCredentialsReceivedDetailed(
                sessionId: String,
                pairingType: String,
                url: String?,
                username: String?,
                password: String?,
                m3uUrl: String?
            ) {}
        }
    }
}
