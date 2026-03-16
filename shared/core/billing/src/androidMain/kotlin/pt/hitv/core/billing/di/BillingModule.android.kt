package pt.hitv.core.billing.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.billing.AndroidBillingManager
import pt.hitv.core.billing.BillingManager

actual val billingPlatformModule: Module = module {
    single<BillingManager> { AndroidBillingManager(context = get()) }
}
