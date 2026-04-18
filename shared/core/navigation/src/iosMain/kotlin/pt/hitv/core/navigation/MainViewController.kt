package pt.hitv.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.navigation.adaptive.AdaptiveScaffold

/**
 * iOS entry point — mirror of HitvApp boot check.
 * Tri-state boot (Checking / LoggedIn / LoggedOut) validates credentials
 * exist AND the channel table has data before composing AdaptiveScaffold.
 */
fun MainViewController() = ComposeUIViewController {
    val preferencesHelper: PreferencesHelper = koinInject()
    val accountManagerRepository: AccountManagerRepository = koinInject()
    val streamRepository: StreamRepository = koinInject()

    var bootState by remember { mutableStateOf(BootState.Checking) }

    LaunchedEffect(Unit) {
        val userId = preferencesHelper.getUserId()
        if (userId == -1) {
            bootState = BootState.LoggedOut
            return@LaunchedEffect
        }
        val creds = runCatching { accountManagerRepository.getCredentialsByUserId(userId) }
            .getOrNull()
        if (creds == null) {
            preferencesHelper.setStoredIntTag("userId", -1)
            preferencesHelper.setStoredBoolean("initial_sync_complete", false)
            bootState = BootState.LoggedOut
            return@LaunchedEffect
        }
        val channelCount = runCatching { streamRepository.getTotalChannelCount() }.getOrNull() ?: 0
        if (channelCount == 0) {
            preferencesHelper.setStoredBoolean("initial_sync_complete", false)
        }
        bootState = BootState.LoggedIn
    }

    when (bootState) {
        BootState.Checking -> { /* short blank frame while verifying */ }
        else -> AdaptiveScaffold(
            isLoggedIn = bootState == BootState.LoggedIn,
            onLoginSuccess = { bootState = BootState.LoggedIn },
            hasAnnualOrLifetime = false,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private enum class BootState { Checking, LoggedIn, LoggedOut }
