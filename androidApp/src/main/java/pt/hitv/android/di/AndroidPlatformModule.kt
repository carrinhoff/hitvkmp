package pt.hitv.android.di

import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pt.hitv.core.data.manager.PremiumStatusProvider
import pt.hitv.core.data.manager.UngatedPremiumStatusProvider
import pt.hitv.core.designsystem.theme.ThemeManager

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

    // PremiumStatusProvider — un-gated until there is a working purchase flow. Binding `false`
    // here silently disabled the whole parental-control feature (PIN accepted any input, no
    // category ever locked). See UngatedPremiumStatusProvider's KDoc for the full rationale and
    // the one-line revert once billing is wired.
    single<PremiumStatusProvider> { UngatedPremiumStatusProvider() }

    // CryptoManager - simplified (no encryption in KMP)
    single { pt.hitv.core.data.security.CryptoManager() }

}
