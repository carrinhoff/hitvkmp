package pt.hitv.feature.auth.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.feature.auth.login.LoginViewModel
import pt.hitv.feature.auth.qr.QRPairingViewModel
import pt.hitv.feature.auth.switchaccount.SwitchAccountViewModel

/**
 * Koin module for the auth feature.
 * Provides ViewModels with their dependencies injected from core modules.
 */
val authFeatureModule: Module = module {

    factory {
        LoginViewModel(
            repository = get(),
            movieRepository = get(),
            tvShowRepository = get(),
            userRepository = get(),
            preferencesHelper = get(),
            userSessionManager = get()
        )
    }

    factory {
        QRPairingViewModel(
            pairingManager = get(),
            preferencesHelper = get(),
            analyticsTracker = get()
        )
    }

    factory {
        SwitchAccountViewModel(
            repository = get(),
            userSessionManager = get(),
            preferencesHelper = get()
        )
    }
}
