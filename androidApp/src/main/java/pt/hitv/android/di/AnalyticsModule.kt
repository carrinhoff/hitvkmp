package pt.hitv.android.di

import org.koin.dsl.module
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.NoOpAnalyticsHelper
import pt.hitv.core.common.crashreporting.CrashReportingHelper
import pt.hitv.core.common.crashreporting.NoOpCrashReportingHelper
import pt.hitv.core.common.featureflags.FeatureFlagManager
import pt.hitv.core.common.featureflags.NoOpFeatureFlagManager

/**
 * Koin module providing analytics, crash reporting, and feature flags.
 * Using NoOp implementations until Firebase is configured with google-services.json.
 */
val analyticsModule = module {
    single<AnalyticsHelper> { NoOpAnalyticsHelper() }
    single<CrashReportingHelper> { NoOpCrashReportingHelper() }
    single<FeatureFlagManager> { NoOpFeatureFlagManager() }
}
