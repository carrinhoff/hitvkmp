package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

/**
 * Wrap protected content (e.g., Channels / Movies / Series tab roots) with
 * [ParentalSessionGuard] to gate rendering behind a session PIN check.
 *
 * Behavior:
 * - If parental control is disabled or the session is authenticated → render [content].
 * - Otherwise → render an explicit "Content locked" state with an **Enter PIN** button, behind
 *   [PinVerifyDialog]. Once the user enters the correct PIN,
 *   [ParentalControlViewModel.validatePin] refreshes `isSessionActive` and the content is shown.
 *   Dismissing the dialog returns to the locked state, from which the prompt can be reopened —
 *   there is deliberately no path that leaves the tab blank and unrecoverable.
 *
 * The guard is intentionally cheap: it observes StateFlows the view model already
 * exposes, so wrapping is a pure Composable boundary — removing it should never
 * affect existing tab lifecycles. If the `hasPinSet` flag is false the guard is a
 * no-op pass-through.
 *
 * ## Scope note
 *
 * Whole-tab gating is a divergence from the original, which gates per protected *category* via
 * `requiresPinForCategory` (ported, and live in `MobileChannelsLayout`). It is kept here because
 * the Movies and Series features have no per-category gating of their own, so this guard is
 * currently their only protection. If per-category gating lands there, this wrapper becomes
 * redundant and should be removed from the three tab roots. See KMP_MIGRATION_AUDIT.md §5.
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

    // Whether the PIN dialog is currently up. Starts true so the prompt appears immediately on
    // entering a gated tab.
    var promptVisible by remember { mutableStateOf(true) }

    // Locked state. Dismissing the PIN dialog used to set a `dismissed` latch and render an
    // empty transparent scrim, which left the tab permanently blank with no way to retry and no
    // way out short of killing the app. That was unreachable while PremiumStatusProvider was
    // hardcoded false; now that parental control actually activates, it is a real dead end.
    // So the locked state is explicit and always offers a way forward.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Content locked",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Enter your parental control PIN to view this section.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { promptVisible = true }) {
                Text("Enter PIN")
            }
        }
    }

    if (promptVisible) {
        PinVerifyDialog(
            viewModel = viewModel,
            onSuccess = {
                // sessionActive flips on the next refresh tick, which un-gates `content`.
                promptVisible = false
            },
            onDismiss = { promptVisible = false }
        )
    }
}
