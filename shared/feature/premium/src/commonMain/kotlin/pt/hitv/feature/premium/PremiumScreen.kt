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
    scrollToTopSignal: Int = 0
) {
    // Mobile layout is the default in common.
    // The Android app module routes to TV layout via platform detection.
    MobilePremiumRouter(
        onNavigateBack = onNavigateBack,
        onPurchaseClick = onPurchaseClick,
        isRootDestination = isRootDestination,
        scrollToTopSignal = scrollToTopSignal
    )
}

@Composable
private fun MobilePremiumRouter(
    onNavigateBack: () -> Unit,
    onPurchaseClick: (productId: String) -> Unit,
    isRootDestination: Boolean,
    scrollToTopSignal: Int
) {
    // TODO: Wire to MobilePremiumLayout with billing state from platform layer
    // For now, a placeholder that works on all platforms
    pt.hitv.feature.premium.mobile.MobilePremiumPortraitCommon(
        onNavigateBack = onNavigateBack,
        onPurchaseClick = onPurchaseClick,
        isRootDestination = isRootDestination,
        scrollToTopSignal = scrollToTopSignal
    )
}
