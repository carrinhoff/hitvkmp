package pt.hitv.feature.premium

import androidx.compose.runtime.*

/**
 * Main router screen for Premium feature (commonMain).
 * Platform-specific layouts (TV) are in androidMain.
 * The caller is responsible for routing to TV layout on Android TV devices.
 */
@Composable
fun PremiumScreen(
    onNavigateBack: () -> Unit,
    onPurchaseClick: (productId: String) -> Unit = {},
    isRootDestination: Boolean = false,
    scrollToTopSignal: Int = 0,
    hasAnnualPremium: Boolean = false,
    hasLifetimePremium: Boolean = false,
    isInTrial: Boolean = false,
    trialExpirationTime: Long? = null,
    annualPrice: String = "4,99 \u20AC",
    lifetimePrice: String = "9,99 \u20AC"
) {
    MobilePremiumRouter(
        onNavigateBack = onNavigateBack,
        onPurchaseClick = onPurchaseClick,
        isRootDestination = isRootDestination,
        scrollToTopSignal = scrollToTopSignal,
        hasAnnualPremium = hasAnnualPremium,
        hasLifetimePremium = hasLifetimePremium,
        isInTrial = isInTrial,
        trialExpirationTime = trialExpirationTime,
        annualPrice = annualPrice,
        lifetimePrice = lifetimePrice
    )
}

@Composable
private fun MobilePremiumRouter(
    onNavigateBack: () -> Unit,
    onPurchaseClick: (productId: String) -> Unit,
    isRootDestination: Boolean,
    scrollToTopSignal: Int,
    hasAnnualPremium: Boolean,
    hasLifetimePremium: Boolean,
    isInTrial: Boolean,
    trialExpirationTime: Long?,
    annualPrice: String,
    lifetimePrice: String
) {
    pt.hitv.feature.premium.mobile.MobilePremiumPortraitCommon(
        onNavigateBack = onNavigateBack,
        onPurchaseClick = onPurchaseClick,
        isRootDestination = isRootDestination,
        scrollToTopSignal = scrollToTopSignal,
        hasAnnualPremium = hasAnnualPremium,
        hasLifetimePremium = hasLifetimePremium,
        isInTrial = isInTrial,
        trialExpirationTime = trialExpirationTime,
        annualPrice = annualPrice,
        lifetimePrice = lifetimePrice
    )
}
