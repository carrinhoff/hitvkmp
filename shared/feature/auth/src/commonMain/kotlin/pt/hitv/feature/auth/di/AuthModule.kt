package pt.hitv.feature.auth.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.feature.auth.login.LoginViewModel
import pt.hitv.feature.auth.switchaccount.SwitchAccountViewModel

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
        SwitchAccountViewModel(
            repository = get(),
            userSessionManager = get(),
            preferencesHelper = get()
        )
    }
}
