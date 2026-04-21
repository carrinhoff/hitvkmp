package pt.hitv.feature.settings.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import pt.hitv.core.ui.categories.ManageCategoriesViewModel
import pt.hitv.feature.settings.options.feedback.SuggestionViewModel
import pt.hitv.feature.settings.options.options.more.MoreOptionsViewModel
import pt.hitv.feature.settings.options.options.parental.ParentalControlViewModel
import pt.hitv.feature.settings.options.options.sync.BackgroundSyncSettingsViewModel
import pt.hitv.feature.settings.options.options.theme.ThemeSettingsViewModel

val settingsModule = module {
    viewModel { SuggestionViewModel() }
    viewModel {
        MoreOptionsViewModel(
            preferencesHelper = get(),
            accountManagerRepository = get(),
            themeManager = get(),
            localeController = get(),
            appInfoProvider = get(),
            backgroundSyncManager = get(),
            syncStateManager = get(),
            syncManager = get(),
        )
    }
    viewModel { ParentalControlViewModel(parentalControlManager = get(), preferencesHelper = get(), streamRepository = get()) }
    viewModel { ThemeSettingsViewModel(themeManager = get()) }
    viewModel { ManageCategoriesViewModel(categoryPreferenceRepository = get()) }
    viewModel {
        BackgroundSyncSettingsViewModel(
            preferencesHelper = get(),
            backgroundSyncManager = get()
        )
    }
}
