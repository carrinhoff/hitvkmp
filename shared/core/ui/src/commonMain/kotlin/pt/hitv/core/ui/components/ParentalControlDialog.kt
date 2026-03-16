package pt.hitv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * PIN entry dialog for parental control.
 * Adapts width based on isTv parameter.
 *
 * @param title Dialog title
 * @param message Optional message below title
 * @param onPinEntered Callback with the entered PIN
 * @param onDismiss Callback when dialog is dismissed
 * @param showError Whether to show error state
 * @param errorMessage Error message to display
 * @param isTv Whether running on TV (wider dialog)
 * @param enterPinLabel Label for PIN input field
 * @param showPinLabel Label for show PIN toggle
 * @param hidePinLabel Label for hide PIN toggle
 * @param pinHintLabel Hint text below PIN input
 * @param cancelLabel Cancel button label
 * @param confirmLabel Confirm button label
 */
@Composable
fun PinDialog(
    title: String,
    message: String = "",
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    showError: Boolean = false,
    errorMessage: String = "",
    isTv: Boolean = false,
    enterPinLabel: String = "Enter PIN",
    showPinLabel: String = "Show PIN",
    hidePinLabel: String = "Hide PIN",
    pinHintLabel: String = "PIN must be 4-6 digits",
    cancelLabel: String = "Cancel",
    confirmLabel: String = "Confirm"
) {
    val themeColors = getThemeColors()

    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isTv) 0.5f else 0.9f)
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

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                    label = { Text(enterPinLabel) },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (pin.length >= 4) {
                                onPinEntered(pin)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (showPin) hidePinLabel else showPinLabel,
                                tint = themeColors.textColor
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { hasFocus = it.isFocused }
                        .focusable()
                        .then(
                            if (isTv && hasFocus)
                                Modifier.border(2.dp, themeColors.primaryColor, RoundedCornerShape(4.dp))
                            else Modifier
                        ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor,
                        focusedContainerColor = themeColors.backgroundPrimary,
                        unfocusedContainerColor = themeColors.backgroundPrimary,
                        cursorColor = themeColors.primaryColor,
                        focusedIndicatorColor = themeColors.primaryColor,
                        unfocusedIndicatorColor = themeColors.textColor.copy(alpha = 0.3f),
                        focusedLabelColor = themeColors.textColor,
                        unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                    ),
                    isError = showError
                )

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
 * Set PIN dialog - allows user to create a new PIN with confirmation.
 *
 * @param onPinSet Callback with the new PIN
 * @param onDismiss Callback when dialog is dismissed
 * @param isTv Whether running on TV
 * @param titleLabel Dialog title
 * @param messageLabel Dialog description
 * @param newPinLabel Label for new PIN field
 * @param confirmPinLabel Label for confirm PIN field
 * @param pinTooShortError Error when PIN is too short
 * @param pinMismatchError Error when PINs don't match
 * @param pinRequirementsLabel PIN requirements description
 * @param cancelLabel Cancel button label
 * @param setPinLabel Set PIN button label
 */
@Composable
fun SetPinDialog(
    onPinSet: (String) -> Unit,
    onDismiss: () -> Unit,
    isTv: Boolean = false,
    titleLabel: String = "Set Parental PIN",
    messageLabel: String = "Create a PIN to protect content",
    newPinLabel: String = "New PIN",
    confirmPinLabel: String = "Confirm PIN",
    pinTooShortError: String = "PIN must be at least 4 digits",
    pinMismatchError: String = "PINs do not match",
    pinRequirementsLabel: String = "PIN must be 4-6 digits",
    cancelLabel: String = "Cancel",
    setPinLabel: String = "Set PIN"
) {
    val themeColors = getThemeColors()

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showConfirmPin by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(if (isTv) 0.5f else 0.9f)
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
                    text = titleLabel,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.textColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = messageLabel,
                    fontSize = 14.sp,
                    color = themeColors.textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            pin = it
                            showError = false
                        }
                    },
                    label = { Text(newPinLabel) },
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showPin = !showPin }) {
                            Icon(
                                imageVector = if (showPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = themeColors.textColor
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor,
                        focusedContainerColor = themeColors.backgroundPrimary,
                        unfocusedContainerColor = themeColors.backgroundPrimary,
                        cursorColor = themeColors.primaryColor,
                        focusedIndicatorColor = themeColors.primaryColor,
                        unfocusedIndicatorColor = themeColors.textColor.copy(alpha = 0.3f),
                        focusedLabelColor = themeColors.textColor,
                        unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            showError = false
                        }
                    },
                    label = { Text(confirmPinLabel) },
                    visualTransformation = if (showConfirmPin) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            when {
                                pin.length < 4 -> {
                                    showError = true
                                    errorMessage = pinTooShortError
                                }
                                pin != confirmPin -> {
                                    showError = true
                                    errorMessage = pinMismatchError
                                }
                                else -> onPinSet(pin)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPin = !showConfirmPin }) {
                            Icon(
                                imageVector = if (showConfirmPin) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                                tint = themeColors.textColor
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor,
                        focusedContainerColor = themeColors.backgroundPrimary,
                        unfocusedContainerColor = themeColors.backgroundPrimary,
                        cursorColor = themeColors.primaryColor,
                        focusedIndicatorColor = themeColors.primaryColor,
                        unfocusedIndicatorColor = themeColors.textColor.copy(alpha = 0.3f),
                        focusedLabelColor = themeColors.textColor,
                        unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                    ),
                    isError = showError
                )

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
                    text = pinRequirementsLabel,
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
                            when {
                                pin.length < 4 -> {
                                    showError = true
                                    errorMessage = pinTooShortError
                                }
                                pin != confirmPin -> {
                                    showError = true
                                    errorMessage = pinMismatchError
                                }
                                else -> onPinSet(pin)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = pin.length >= 4 && confirmPin.length >= 4,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryColor,
                            contentColor = themeColors.textColor,
                            disabledContainerColor = themeColors.primaryColor.copy(alpha = 0.5f),
                            disabledContentColor = themeColors.textColor.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(setPinLabel)
                    }
                }
            }
        }
    }
}
