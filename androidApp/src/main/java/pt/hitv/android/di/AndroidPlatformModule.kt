package pt.hitv.android.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pt.hitv.core.data.manager.PairingAnalyticsTracker
import pt.hitv.core.data.manager.PremiumStatusProvider
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.android.analytics.FirebasePairingAnalyticsTrackerImpl

/**
 * Android-specific Koin bindings that are not provided by shared modules.
 */
val androidPlatformModule = module {

    // Settings - multiplatform-settings backed by SharedPreferences
    single<Settings> {
        val prefs = androidContext().getSharedPreferences("SPFile", android.content.Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    }

    // ObservableSettings - same SharedPreferences instance, cast to ObservableSettings for reactive observation
    single<ObservableSettings> {
        val prefs = androidContext().getSharedPreferences("SPFile", android.content.Context.MODE_PRIVATE)
        SharedPreferencesSettings(prefs)
    }

    // ThemeManager
    single { ThemeManager(preferencesHelper = get()) }

    // PairingAnalyticsTracker
    single<PairingAnalyticsTracker> { FirebasePairingAnalyticsTrackerImpl(analyticsHelper = get()) }

    // PremiumStatusProvider - always false for now (billing integration TODO)
    single<PremiumStatusProvider> {
        object : PremiumStatusProvider {
            override fun hasPremiumSubscription(): Boolean = false
        }
    }

    // Firebase DatabaseReference for pairing sessions
    single<DatabaseReference> {
        Firebase.database.reference("pairingSessions")
    }

    // CryptoManager - simplified (no encryption in KMP)
    single { pt.hitv.core.data.security.CryptoManager() }

}
