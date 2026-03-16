package pt.hitv.feature.premium.di

import org.koin.dsl.module

val premiumModule = module {
    // Premium screen has no ViewModel - it uses BillingManager directly (platform-specific)
    // Platform modules will provide billing integration
}
