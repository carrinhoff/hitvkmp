package pt.hitv.core.data.manager

/**
 * Interface to provide premium status without depending on BillingManager directly.
 * Platform implementations provide the actual subscription check.
 *
 * The only consumer is [ParentalControlManagerImpl].
 */
interface PremiumStatusProvider {
    fun hasPremiumSubscription(): Boolean
}

/**
 * Treats every user as entitled, because **this build has no working purchase flow**.
 *
 * ## Why this exists
 *
 * Both platform DI modules previously bound an anonymous `hasPremiumSubscription() = false`.
 * Combined with [ParentalControlManagerImpl.isParentalControlEnabled] returning `false` without
 * premium, that made the entire parental-control feature inert: `validatePin()` accepted **any**
 * input, `isCategoryProtected()` always returned `false`, and `getProtectedCategoryIds()` was
 * always empty. A parent could set a PIN, mark categories as locked, see them displayed as
 * protected — and the app would never ask for the PIN or hide anything.
 *
 * That is worse than the feature being absent: it's a false assurance about child safety. And
 * it wasn't reachable-by-purchase either, since nothing on either platform is wired to billing
 * (`PremiumVoyagerScreen`'s `onPurchaseClick` is an empty TODO), so no user could ever have
 * flipped the flag to `true`.
 *
 * Returning `true` makes the shipped UI tell the truth. It is also consistent with how the port
 * already treats the other paid tier — Theme Studio dropped the original's premium gate and
 * offers all six themes free.
 *
 * ## Divergence from the original — flagged deliberately
 *
 * The original `hitv` app *does* gate parental controls behind a real Play Billing entitlement.
 * This is therefore a knowing divergence, not a faithful port, chosen because the alternative in
 * a build with no purchase flow is a feature that lies to the user.
 *
 * ## When billing lands
 *
 * Replace this binding in `AndroidPlatformModule` / `KoinIOS` with one backed by the real
 * entitlement and the original's behaviour returns with no other changes. That is the whole
 * revert — one line per platform. See KMP_MIGRATION_AUDIT.md P0 #5 / #6.
 */
class UngatedPremiumStatusProvider : PremiumStatusProvider {
    override fun hasPremiumSubscription(): Boolean = true
}
