package pt.hitv.core.common.di

import org.koin.dsl.module
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.datastore.HitvPreferencesDataStore

/**
 * Koin module providing common utilities available on all platforms:
 * - PreferencesHelper (wraps Settings + encrypted settings)
 * - HitvPreferencesDataStore (reactive preferences via FlowSettings)
 *
 * Platform modules must provide:
 * - Settings (multiplatform-settings)
 * - ObservableSettings (for reactive observation)
 */
val commonModule = module {
    single { PreferencesHelper(settings = get()) }
    single { HitvPreferencesDataStore(observableSettings = get()) }
}
