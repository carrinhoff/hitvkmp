package pt.hitv.feature.auth.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.feature.auth.login.LoginScreen
import pt.hitv.feature.auth.login.LoginViewModel

class LoginVoyagerScreen(private val onLoginSuccess: () -> Unit) : Screen {
    override val key = "Login"

    @Composable
    override fun Content() {
        val viewModel: LoginViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val preferencesHelper: PreferencesHelper = koinInject()

        LoginScreen(
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            preferencesHelper = preferencesHelper,
            onLoginSuccess = onLoginSuccess
        )
    }
}
