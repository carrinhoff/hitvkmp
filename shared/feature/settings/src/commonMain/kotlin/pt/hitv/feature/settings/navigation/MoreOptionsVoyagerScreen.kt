package pt.hitv.feature.settings.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.navigation.navigateToThemeSettings
import pt.hitv.core.navigation.navigateToSwitchAccount
import pt.hitv.core.navigation.navigateToParentalControl
import pt.hitv.core.navigation.navigateToManageCategories
import pt.hitv.core.navigation.navigateToFeedback
import pt.hitv.core.navigation.navigateToLiveEpg
import pt.hitv.feature.settings.options.options.more.MobileMoreOptionsScreen
import pt.hitv.feature.settings.options.options.more.MoreOptionsViewModel

class MoreOptionsVoyagerScreen : Screen {
    override val key = "MoreOptions"

    @Composable
    override fun Content() {
        val viewModel: MoreOptionsViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        MobileMoreOptionsScreen(
            viewModel = viewModel,
            onRefreshDataClick = { /* TODO: Wire to sync */ },
            onEpgClick = { navigator.navigateToLiveEpg() },
            onManageCategoriesClick = { navigator.navigateToManageCategories() },
            onThemeClick = { navigator.navigateToThemeSettings() },
            onParentalControlClick = { navigator.navigateToParentalControl() },
            onFeedbackClick = { navigator.navigateToFeedback() },
            onSwitchAccountClick = { navigator.navigateToSwitchAccount() }
        )
    }
}
