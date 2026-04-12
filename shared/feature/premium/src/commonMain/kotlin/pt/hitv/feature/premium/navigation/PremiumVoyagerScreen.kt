package pt.hitv.feature.premium.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import pt.hitv.feature.premium.PremiumScreen

class PremiumVoyagerScreen : Screen {
    override val key = "PremiumSubscription"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        PremiumScreen(
            onNavigateBack = { navigator.pop() },
            onPurchaseClick = { productId ->
                // TODO: Wire to platform billing
            },
            isRootDestination = true
        )
    }
}
