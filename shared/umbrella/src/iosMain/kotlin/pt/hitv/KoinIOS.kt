package pt.hitv

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.NoOpAnalyticsHelper
import pt.hitv.core.common.crashreporting.CrashReportingHelper
import pt.hitv.core.common.crashreporting.NoOpCrashReportingHelper
import pt.hitv.core.common.featureflags.FeatureFlagManager
import pt.hitv.core.common.featureflags.NoOpFeatureFlagManager
import pt.hitv.core.common.di.commonModule
import pt.hitv.core.data.security.CryptoManager
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults
import pt.hitv.core.billing.di.billingPlatformModule
import pt.hitv.core.data.di.dataModule
import pt.hitv.core.database.di.databaseModule
import pt.hitv.core.database.di.databasePlatformModule
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.network.di.networkModule
import pt.hitv.core.sync.di.syncModule
import pt.hitv.core.sync.di.syncPlatformModule
// Feature modules
import pt.hitv.feature.auth.di.authFeatureModule
import pt.hitv.feature.auth.navigation.registerAuthScreens
import pt.hitv.feature.channels.di.channelsFeatureModule
import pt.hitv.feature.channels.navigation.registerChannelsScreens
import pt.hitv.feature.movies.di.moviesFeatureModule
import pt.hitv.feature.movies.navigation.registerMoviesScreens
import pt.hitv.feature.series.di.seriesModule
import pt.hitv.feature.series.navigation.registerSeriesScreens
import pt.hitv.feature.player.di.playerModule
import pt.hitv.feature.premium.di.premiumModule
import pt.hitv.feature.premium.navigation.registerPremiumScreens
import pt.hitv.feature.settings.di.settingsModule
import pt.hitv.feature.settings.navigation.registerSettingsScreens

/**
 * Initialize Koin for iOS.
 * Called from Swift via `KoinIOSKt.doInitKoinIOS()`.
 *
 * This lives in the umbrella module which depends on ALL modules,
 * so there are no circular dependency issues.
 */
fun initKoinIOS() {
    // Register all Voyager screen factories
    registerAuthScreens()
    registerChannelsScreens()
    registerMoviesScreens()
    registerSeriesScreens()
    registerPremiumScreens()
    registerSettingsScreens()

    // Start Koin with all modules
    startKoin {
        modules(
            // Core
            commonModule,
            dataModule,
            databaseModule,
            networkModule,
            syncModule,
            // Core platform
            databasePlatformModule,
            billingPlatformModule,
            syncPlatformModule,
            // Feature
            authFeatureModule,
            channelsFeatureModule,
            moviesFeatureModule,
            seriesModule,
            playerModule,
            premiumModule,
            settingsModule,
            // iOS platform
            iosPlatformModule,
        )
    }
}

private val iosPlatformModule = module {
    // Settings (multiplatform-settings backed by NSUserDefaults)
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<ObservableSettings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    // Analytics/Crash/FeatureFlags — NoOp for iOS
    single<AnalyticsHelper> { NoOpAnalyticsHelper() }
    single<CrashReportingHelper> { NoOpCrashReportingHelper() }
    single<FeatureFlagManager> { NoOpFeatureFlagManager() }
    single { ThemeManager(preferencesHelper = get()) }
    // CryptoManager — no encryption on iOS (same as Android KMP)
    single { CryptoManager() }
    // PremiumStatusProvider — always false for now
    single<pt.hitv.core.data.manager.PremiumStatusProvider> {
        object : pt.hitv.core.data.manager.PremiumStatusProvider {
            override fun hasPremiumSubscription(): Boolean = false
        }
    }
}
