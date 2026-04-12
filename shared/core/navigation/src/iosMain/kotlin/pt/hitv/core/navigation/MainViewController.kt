package pt.hitv.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.navigation.adaptive.AdaptiveScaffold

/**
 * iOS entry point creating a UIViewController hosting the shared Compose UI.
 * Checks login state and shows LoginScreen or main navigation accordingly.
 */
fun MainViewController() = ComposeUIViewController {
    val preferencesHelper: PreferencesHelper = koinInject()
    var isLoggedIn by remember { mutableStateOf(preferencesHelper.getUserId() != -1) }

    AdaptiveScaffold(
        isLoggedIn = isLoggedIn,
        onLoginSuccess = { isLoggedIn = true },
        hasAnnualOrLifetime = false,
        modifier = Modifier.fillMaxSize()
    )
}
