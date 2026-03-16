package pt.hitv.core.data.manager

/**
 * Interface to provide premium status without depending on BillingManager directly.
 * Platform implementations provide the actual subscription check.
 */
interface PremiumStatusProvider {
    fun hasPremiumSubscription(): Boolean
}
