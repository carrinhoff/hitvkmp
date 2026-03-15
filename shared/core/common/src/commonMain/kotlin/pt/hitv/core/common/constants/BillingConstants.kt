package pt.hitv.core.common.constants

object BillingConstants {
    // Set to true in debug for real Play Billing dialog, false to skip to instant/unlocked logic
    const val DEBUG_FORCE_PLAY_DIALOG = true

    // Google Play Billing active? This will be set by the app module based on BuildConfig.DEBUG
    // Default to false (real billing) - app module should override this in debug builds
    var USE_FAKE_BILLING: Boolean = false

    // Google Play Product IDs
    // Legacy - kept for backwards compatibility (no longer offered in UI)
    const val THEME_PREMIUM = "theme_premium"  // Product Type: INAPP

    // Pricing: remove ads (single open app ad - not intrusive, lower value)
    const val REMOVE_ADS = "remove_ads"  // Product Type: INAPP

    // Premium Subscription Tiers (Includes: Themes, Ad-free, Priority Support, Early Access)
    const val ANNUAL_PREMIUM = "annual_premium_sub"  // Product Type: SUBS (Subscription)

    // Lifetime premium (pay once, enjoy forever)
    const val LIFETIME_PREMIUM = "lifetime_premium"  // Product Type: INAPP
}
