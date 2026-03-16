package pt.hitv.android.di

import org.koin.dsl.module
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.manager.PairingAnalyticsTracker
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.android.analytics.FirebasePairingAnalyticsTrackerImpl

/**
 * Android-specific Koin bindings that are not provided by shared modules.
 *
 * Provides:
 * - ThemeManager (requires PreferencesHelper from shared)
 * - PairingAnalyticsTracker (bridges to Firebase Analytics)
 * - Any other Android-only platform implementations
 */
val androidPlatformModule = module {

    // ThemeManager - shared class instantiated with Android PreferencesHelper
    single { ThemeManager(preferencesHelper = get()) }

    // PairingAnalyticsTracker - bridges feature analytics to Firebase
    single<PairingAnalyticsTracker> { FirebasePairingAnalyticsTrackerImpl(analyticsHelper = get()) }
}
