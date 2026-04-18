package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.settings.components.PinDialog
import pt.hitv.feature.settings.components.SetPinDialog

/**
 * Landing screen for Parental Controls.
 * Shows:
 * - Enabled/disabled state card (driven by existence of PIN + ParentalControlManager.isParentalControlEnabled())
 * - Set/Change/Remove PIN actions
 * - "Locked Categories" entry → [CategoryLockScreen]
 * - "Session Timeout" entry → [SessionTimeoutDialog]
 *
 * Reuses the existing [ParentalControlViewModel] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    viewModel: ParentalControlViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryLock: () -> Unit,
    titleText: String = "Parental Controls",
    enabledText: String = "Enabled",
    disabledText: String = "Disabled",
    setPinText: String = "Set PIN",
    changePinText: String = "Change PIN",
    removePinText: String = "Remove PIN",
    lockedCategoriesText: String = "Locked Categories",
    sessionTimeoutText: String = "Session Timeout",
    lockedCategoriesCountSuffix: String = "protected",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    val state by viewModel.uiState.collectAsState()
    val sessionMinutes by viewModel.sessionTimeoutMinutes.collectAsState()

    var showTimeoutDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = themeColors.backgroundPrimary,
        topBar = {
            TopAppBar(
                title = { Text(titleText, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.backgroundPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Status card
            StatusCard(
                enabled = state.isEnabled && state.hasPinSet,
                enabledText = enabledText,
                disabledText = disabledText
            )

            Spacer(Modifier.height(16.dp))

            // PIN section
            if (!state.hasPinSet) {
                PrimaryActionRow(
                    icon = Icons.Filled.Lock,
                    title = setPinText,
                    subtitle = "Create a 4-digit PIN to protect categories",
                    onClick = { viewModel.showSetPinDialog() }
                )
            } else {
                PrimaryActionRow(
                    icon = Icons.Filled.Lock,
                    title = changePinText,
                    subtitle = "Update your current PIN",
                    onClick = { viewModel.showChangePinDialog() }
                )
                Spacer(Modifier.height(8.dp))
                PrimaryActionRow(
                    icon = Icons.Filled.LockOpen,
                    title = removePinText,
                    subtitle = "Disable parental control",
                    onClick = { viewModel.showRemovePinDialog() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Locked categories
            PrimaryActionRow(
                icon = Icons.Filled.Shield,
                title = lockedCategoriesText,
                subtitle = "${state.protectedCategoriesCount} $lockedCategoriesCountSuffix",
                trailingArrow = true,
                onClick = { if (state.hasPinSet) onNavigateToCategoryLock() }
            )

            Spacer(Modifier.height(8.dp))

            // Session timeout
            val timeoutLabel = when (sessionMinutes) {
                -2 -> "Always ask"
                -1 -> "Until app closes"
                0 -> "Never"
                else -> "$sessionMinutes min"
            }
            PrimaryActionRow(
                icon = Icons.Filled.Timer,
                title = sessionTimeoutText,
                subtitle = timeoutLabel,
                trailingArrow = true,
                onClick = { if (state.hasPinSet) showTimeoutDialog = true }
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    // Set PIN dialog
    if (state.showSetPinDialog) {
        SetPinDialog(
            onPinSet = { pin -> viewModel.setPin(pin) },
            onDismiss = { viewModel.hideSetPinDialog() }
        )
    }

    // Change PIN dialog — two-step: verify current, then set new
    if (state.showChangePinDialog) {
        ChangePinDialog(
            onConfirm = { current, new ->
                viewModel.changePin(
                    currentPin = current,
                    newPin = new,
                    onSuccess = { pinError = null },
                    onError = { pinError = "Current PIN is incorrect" }
                )
            },
            onDismiss = { viewModel.hideChangePinDialog(); pinError = null },
            errorMessage = pinError
        )
    }

    // Remove PIN confirmation (needs current PIN)
    if (state.showRemovePinDialog) {
        PinDialog(
            title = removePinText,
            message = "Enter your current PIN to disable parental control.",
            onPinEntered = { pin ->
                viewModel.validatePin(
                    pin = pin,
                    onSuccess = {
                        viewModel.removePin { pinError = null }
                        viewModel.hideRemovePinDialog()
                    },
                    onError = { pinError = "Incorrect PIN" }
                )
            },
            onDismiss = { viewModel.hideRemovePinDialog(); pinError = null },
            showError = pinError != null,
            errorMessage = pinError.orEmpty()
        )
    }

    if (showTimeoutDialog) {
        SessionTimeoutDialog(
            currentMinutes = sessionMinutes,
            onSelect = { minutes ->
                viewModel.setSessionTimeout(minutes)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }
}

@Composable
private fun StatusCard(enabled: Boolean, enabledText: String, disabledText: String) {
    val themeColors = getThemeColors()
    val accent = if (enabled) themeColors.success else themeColors.textSecondary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (enabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = accent
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = if (enabled) enabledText else disabledText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (enabled) "Protected categories require PIN to view" else "Set a PIN to start protecting categories",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingArrow: Boolean = false,
    onClick: () -> Unit
) {
    val themeColors = getThemeColors()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = themeColors.primaryColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            if (trailingArrow) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    onConfirm: (currentPin: String, newPin: String) -> Unit,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    var step by remember { mutableStateOf(0) }
    var currentPin by remember { mutableStateOf("") }
    when (step) {
        0 -> PinDialog(
            title = "Verify current PIN",
            onPinEntered = {
                currentPin = it
                step = 1
            },
            onDismiss = onDismiss,
            showError = errorMessage != null,
            errorMessage = errorMessage.orEmpty()
        )
        else -> SetPinDialog(
            onPinSet = { newPin -> onConfirm(currentPin, newPin) },
            onDismiss = onDismiss,
            setPinTitle = "Set new PIN"
        )
    }
}
