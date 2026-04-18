package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.koin.compose.koinInject

/**
 * Wrap protected content (e.g., Channels / Movies / Series tab roots) with
 * [ParentalSessionGuard] to gate rendering behind a session PIN check.
 *
 * Behavior:
 * - If parental control is disabled or the session is authenticated → render [content].
 * - Otherwise → render a dimmed placeholder behind [PinVerifyDialog]; once the user
 *   enters the correct PIN, [ParentalControlViewModel.validatePin] refreshes
 *   `isSessionActive` and the content is shown.
 *
 * The guard is intentionally cheap: it observes StateFlows the view model already
 * exposes, so wrapping is a pure Composable boundary — removing it should never
 * affect existing tab lifecycles. If the `hasPinSet` flag is false the guard is a
 * no-op pass-through.
 */
@Composable
fun ParentalSessionGuard(
    viewModel: ParentalControlViewModel = koinInject(),
    content: @Composable () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionActive by viewModel.isSessionActive.collectAsState()

    // Gate only when parental control is enabled AND a PIN has been set AND the
    // current session is not yet authenticated.
    val gated = uiState.hasPinSet && uiState.isEnabled && !sessionActive

    if (!gated) {
        content()
        return
    }

    var dismissed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
    ) {
        // Placeholder: hide content while gated. Consumers may replace this slot
        // in future revisions; for now a plain scrim is simplest + safe.
        Box(Modifier.fillMaxSize().background(Color.Transparent))
    }

    if (!dismissed) {
        PinVerifyDialog(
            viewModel = viewModel,
            onSuccess = { /* sessionActive flips on next refresh tick */ },
            onDismiss = { dismissed = true }
        )
    }
}
