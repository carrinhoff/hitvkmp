package pt.hitv.core.billing.di

import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.billing.BillingManager
import pt.hitv.core.billing.IosBillingManager

actual val billingPlatformModule: Module = module {
    single<BillingManager> { IosBillingManager() }
}
