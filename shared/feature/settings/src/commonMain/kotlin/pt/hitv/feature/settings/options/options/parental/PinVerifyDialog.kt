package pt.hitv.feature.settings.options.options.parental

import androidx.compose.runtime.*
import pt.hitv.feature.settings.components.PinDialog

/**
 * PIN verification dialog — shown before entering protected categories or
 * re-entering a protected tab after session timeout. Thin wrapper over
 * [PinDialog] that drives [ParentalControlViewModel.validatePin] and exposes
 * success/error callbacks to the caller.
 */
@Composable
fun PinVerifyDialog(
    viewModel: ParentalControlViewModel,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "Enter PIN",
    message: String = "Enter your parental control PIN to continue."
) {
    var error by remember { mutableStateOf<String?>(null) }
    PinDialog(
        title = title,
        message = message,
        onPinEntered = { pin ->
            viewModel.validatePin(
                pin = pin,
                onSuccess = {
                    error = null
                    onSuccess()
                },
                onError = { error = "Incorrect PIN" }
            )
        },
        onDismiss = onDismiss,
        showError = error != null,
        errorMessage = error.orEmpty()
    )
}
