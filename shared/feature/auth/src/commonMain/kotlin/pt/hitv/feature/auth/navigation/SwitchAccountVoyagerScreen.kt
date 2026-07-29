package pt.hitv.feature.auth.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.feature.auth.switchaccount.SwitchAccountScreen
import pt.hitv.feature.auth.switchaccount.SwitchAccountViewModel

class SwitchAccountVoyagerScreen : Screen {
    override val key = "SwitchAccount"

    @Composable
    override fun Content() {
        val viewModel: SwitchAccountViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        SwitchAccountScreen(
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            onBackPressed = { navigator.pop() },
            onAddNewAccount = {
                // Push the login screen so the user can add another account.
                navigator.push(LoginVoyagerScreen(onLoginSuccess = { navigator.pop() }))
            },
            onAccountSwitched = { navigator.pop() },
            onNavigateToLogin = {
                // Last account was deleted — replace the back stack with login.
                navigator.replaceAll(LoginVoyagerScreen(onLoginSuccess = {}))
            }
        )
    }
}
