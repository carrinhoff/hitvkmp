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

/**
 * Initialize Koin for iOS.
 *
 * Called from Swift via `KoinIOSKt.doInitKoinIOS()`.
 * Moved from core:common to core:navigation because this module
 * has access to all core dependencies (data, database, network, sync, billing).
 */
fun initKoinIOS() {
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
