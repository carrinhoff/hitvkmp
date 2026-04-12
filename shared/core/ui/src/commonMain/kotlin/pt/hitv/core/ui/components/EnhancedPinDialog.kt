package pt.hitv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Enhanced PIN dialog that delegates to PinDialog for mobile
 * and provides a TV-optimized version with numeric grid on TV.
 *
 * On mobile, this is identical to PinDialog.
 * On TV (isTv=true), it shows a larger dialog with inline PIN display
 * and show/hide toggle via Switch.
 *
 * @param title Dialog title
 * @param message Optional message
 * @param onPinEntered Callback with entered PIN
 * @param onDismiss Callback when dismissed
 * @param showError Whether to show error state
 * @param errorMessage Error text
 * @param isTv Whether running on TV
 * @param pinHintLabel Hint text for PIN
 * @param showPinLabel Label for show PIN toggle
 * @param hidePinLabel Label for hide PIN toggle
 * @param cancelLabel Cancel button label
 * @param confirmLabel Confirm button label
 */
@Composable
fun EnhancedPinDialog(
    title: String,
    message: String = "",
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    showError: Boolean = false,
    errorMessage: String = "",
    isTv: Boolean = false,
    pinHintLabel: String = "PIN must be 4-6 digits",
    showPinLabel: String = "Show PIN",
    hidePinLabel: String = "Hide PIN",
    cancelLabel: String = "Cancel",
    confirmLabel: String = "Confirm"
) {
    val themeColors = getThemeColors()

    // Mobile uses regular PinDialog
    if (!isTv) {
        PinDialog(
            title = title,
            message = message,
            onPinEntered = onPinEntered,
            onDismiss = onDismiss,
            showError = showError,
            errorMessage = errorMessage,
            isTv = false
        )
        return
    }

    // TV-specific implementation with larger layout
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.backgroundSecondary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = themeColors.primaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textColor,
                    textAlign = TextAlign.Center
                )

                if (message.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = themeColors.textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // PIN display dots
                PinDotsDisplay(
                    pin = pin,
                    maxLength = 6,
                    showPin = showPin
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Numeric keypad
                NumericKeypad(
                    onDigitPressed = { digit ->
                        if (pin.length < 6) {
                            pin += digit
                        }
                    },
                    onBackspace = {
                        if (pin.isNotEmpty()) {
                            pin = pin.dropLast(1)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Show/Hide toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showPin) hidePinLabel else showPinLabel,
                        fontSize = 14.sp,
                        color = themeColors.textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showPin,
                        onCheckedChange = { showPin = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = themeColors.textColor,
                            checkedTrackColor = themeColors.primaryColor
                        )
                    )
                }

                if (showError && errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = Color.Red,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = pinHintLabel,
                    fontSize = 12.sp,
                    color = themeColors.textColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeColors.textColor
                        )
                    ) {
                        Text(cancelLabel)
                    }

                    Button(
                        onClick = {
                            if (pin.length >= 4) {
                                onPinEntered(pin)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pin.length >= 4,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryColor,
                            contentColor = themeColors.textColor,
                            disabledContainerColor = themeColors.primaryColor.copy(alpha = 0.5f),
                            disabledContentColor = themeColors.textColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

/**
 * PIN dots display showing filled/empty states.
 */
@Composable
private fun PinDotsDisplay(
    pin: String,
    maxLength: Int,
    showPin: Boolean
) {
    val themeColors = getThemeColors()

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 24.dp)
    ) {
        repeat(maxLength) { index ->
            val isFilled = index < pin.length
            val digit = if (showPin && index < pin.length) pin[index].toString() else null

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isFilled) themeColors.primaryColor.copy(alpha = 0.3f)
                        else themeColors.backgroundSecondary,
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (digit != null) {
                    Text(
                        text = digit,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textColor
                    )
                } else if (isFilled) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(themeColors.primaryColor, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Simple numeric keypad for PIN entry.
 */
@Composable
private fun NumericKeypad(
    onDigitPressed: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val themeColors = getThemeColors()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0..2) {
                    val digit = (row * 3 + col + 1).toString()
                    Button(
                        onClick = { onDigitPressed(digit) },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.backgroundSecondary,
                            contentColor = themeColors.textColor
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = digit, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        // Bottom row: spacer, 0, backspace
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(64.dp))

            Button(
                onClick = { onDigitPressed("0") },
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.backgroundSecondary,
                    contentColor = themeColors.textColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(text = "0", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onBackspace,
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColors.backgroundSecondary,
                    contentColor = themeColors.textColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "⌫",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
