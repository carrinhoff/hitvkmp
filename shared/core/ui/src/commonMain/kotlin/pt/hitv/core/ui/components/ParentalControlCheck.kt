package pt.hitv.core.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import pt.hitv.core.domain.manager.ParentalControlManager

/**
 * Helper composable to handle parental control checks.
 *
 * Usage:
 * ```
 * ParentalControlCheck(
 *     categoryId = category.categoryId,
 *     userId = userId,
 *     parentalControlManager = parentalControlManager,
 *     onAccessGranted = { navController.navigate(...) }
 * )
 * ```
 *
 * @param categoryId The category to check
 * @param userId Current user ID
 * @param parentalControlManager Manager for parental control operations
 * @param onAccessGranted Callback when access is granted
 * @param onDismiss Callback when dialog is dismissed
 * @param protectedTitle Title for protected category dialog
 * @param protectedMessage Message for protected category dialog
 * @param incorrectPinError Error message for incorrect PIN
 */
@Composable
fun ParentalControlCheck(
    categoryId: Long,
    userId: Int,
    parentalControlManager: ParentalControlManager,
    onAccessGranted: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    protectedTitle: String = "Protected Category",
    protectedMessage: String = "This category is locked",
    incorrectPinError: String = "Incorrect PIN"
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var requiresPin by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Check if category requires PIN
    LaunchedEffect(categoryId) {
        requiresPin = parentalControlManager.requiresPinForCategory(categoryId, userId)
        if (!requiresPin) {
            onAccessGranted()
        } else {
            showPinDialog = true
        }
    }

    if (showPinDialog && requiresPin) {
        PinDialog(
            title = protectedTitle,
            message = protectedMessage,
            onPinEntered = { pin ->
                scope.launch {
                    if (parentalControlManager.validatePin(pin)) {
                        showPinDialog = false
                        showError = false
                        onAccessGranted()
                    } else {
                        showError = true
                        errorMessage = incorrectPinError
                    }
                }
            },
            onDismiss = {
                showPinDialog = false
                onDismiss?.invoke()
            },
            showError = showError,
            errorMessage = errorMessage
        )
    }
}

/**
 * Check if category requires PIN without showing dialog.
 * Returns true if PIN is required.
 */
suspend fun checkCategoryRequiresPin(
    categoryId: Long,
    userId: Int,
    parentalControlManager: ParentalControlManager
): Boolean {
    return parentalControlManager.requiresPinForCategory(categoryId, userId)
}
