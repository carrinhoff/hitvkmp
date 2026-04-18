package pt.hitv.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.navigation.adaptive.AdaptiveScaffold

/**
 * Root composable for the HITV Android app.
 *
 * Boots to a short "checking" state while we verify the persisted state is
 * consistent, so AdaptiveScaffold never composes with a stale `isLoggedIn` flag:
 *
 *   1. If `userId` pref is set but DB has no matching credentials row → clear
 *      prefs and force login.
 *   2. If credentials exist but the channel table is empty → clear the
 *      `initial_sync_complete` flag so AdaptiveScaffold's sync path re-runs.
 *
 * This handles EncryptedSharedPreferences surviving an app uninstall on
 * Android (where the Keystore-backed prefs persist) and partial-wipe states
 * where the DB schema was recreated but prefs weren't cleared.
 */
@Composable
fun HitvApp() {
    val preferencesHelper: PreferencesHelper = koinInject()
    val accountManagerRepository: AccountManagerRepository = koinInject()
    val streamRepository: StreamRepository = koinInject()

    // Tri-state boot: Checking (gate AdaptiveScaffold until we know), LoggedIn, LoggedOut.
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
            // Credentials vanished — stale userId. Wipe it and route to login.
            preferencesHelper.setStoredIntTag("userId", -1)
            preferencesHelper.setStoredBoolean("initial_sync_complete", false)
            bootState = BootState.LoggedOut
            return@LaunchedEffect
        }
        // Credentials exist but the channel table might be empty (e.g. after a
        // schema upgrade that recreated tables). If so, clear the initial_sync
        // flag so AdaptiveScaffold re-runs sync on entry.
        val channelCount = runCatching { streamRepository.getTotalChannelCount() }.getOrNull() ?: 0
        if (channelCount == 0) {
            preferencesHelper.setStoredBoolean("initial_sync_complete", false)
        }
        bootState = BootState.LoggedIn
    }

    when (bootState) {
        BootState.Checking -> {
            // Empty frame — fast check usually completes within a frame or two.
            // Avoids showing login/tabs with stale state.
        }
        else -> {
            AdaptiveScaffold(
                isLoggedIn = bootState == BootState.LoggedIn,
                onLoginSuccess = { bootState = BootState.LoggedIn },
                hasAnnualOrLifetime = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private enum class BootState { Checking, LoggedIn, LoggedOut }
