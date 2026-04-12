package pt.hitv.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.navigation.adaptive.AdaptiveScaffold

/**
 * Root composable for the AxonStream IPTV Android app.
 *
 * Checks login state via PreferencesHelper and shows:
 * - LoginScreen if not logged in (userId == -1)
 * - AdaptiveScaffold with tab navigation if logged in
 */
@Composable
fun HitvApp() {
    val preferencesHelper: PreferencesHelper = koinInject()
    var isLoggedIn by remember { mutableStateOf(preferencesHelper.getUserId() != -1) }

    AdaptiveScaffold(
        isLoggedIn = isLoggedIn,
        onLoginSuccess = { isLoggedIn = true },
        hasAnnualOrLifetime = false,
        modifier = Modifier.fillMaxSize()
    )
}
