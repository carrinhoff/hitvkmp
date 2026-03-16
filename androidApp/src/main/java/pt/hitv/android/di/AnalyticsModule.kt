package pt.hitv.android.di

import org.koin.dsl.module
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.crashreporting.CrashReportingHelper
import pt.hitv.core.common.featureflags.FeatureFlagManager
import pt.hitv.android.analytics.FirebaseAnalyticsHelperImpl
import pt.hitv.android.analytics.FirebaseCrashReportingHelperImpl
import pt.hitv.android.analytics.FirebaseFeatureFlagManagerImpl

/**
 * Koin module providing Firebase-backed implementations of
 * analytics, crash reporting, and feature flags for Android.
 */
val analyticsModule = module {
    single<AnalyticsHelper> { FirebaseAnalyticsHelperImpl() }
    single<CrashReportingHelper> { FirebaseCrashReportingHelperImpl() }
    single<FeatureFlagManager> { FirebaseFeatureFlagManagerImpl() }
}
