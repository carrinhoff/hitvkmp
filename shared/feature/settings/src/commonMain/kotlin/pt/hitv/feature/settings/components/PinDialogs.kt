package pt.hitv.feature.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.hitv.core.designsystem.theme.getThemeColors

@Composable
fun PinDialog(
    title: String,
    message: String = "",
    onPinEntered: (String) -> Unit,
    onDismiss: () -> Unit,
    showError: Boolean = false,
    errorMessage: String = "",
    enterPinLabel: String = "Enter PIN",
    cancelText: String = "Cancel",
    confirmText: String = "Confirm"
) {
    val themeColors = getThemeColors()
    var pin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = themeColors.primaryColor, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                if (message.isNotEmpty()) { Spacer(modifier = Modifier.height(8.dp)); Text(text = message, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center) }
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = pin, onValueChange = { if (it.length <= 6) pin = it }, label = { Text(enterPinLabel) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPin = !showPin }) { Icon(imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) } },
                    isError = showError,
                    supportingText = if (showError) { { Text(errorMessage, color = MaterialTheme.colorScheme.error) } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.primaryColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(cancelText) }
                    Button(onClick = { onPinEntered(pin) }, modifier = Modifier.weight(1f), enabled = pin.length >= 4, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor)) { Text(confirmText) }
                }
            }
        }
    }
}

@Composable
fun SetPinDialog(
    onPinSet: (String) -> Unit,
    onDismiss: () -> Unit,
    setPinTitle: String = "Set PIN",
    enterPinLabel: String = "Enter PIN",
    confirmPinLabel: String = "Confirm PIN",
    pinsDontMatchError: String = "PINs don't match",
    cancelText: String = "Cancel",
    saveText: String = "Save"
) {
    val themeColors = getThemeColors()
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = themeColors.primaryColor, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = setPinTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6) pin = it }, label = { Text(enterPinLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showPin = !showPin }) { Icon(imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) } }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.primaryColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 6) confirmPin = it }, label = { Text(confirmPinLabel) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(), isError = showError, supportingText = if (showError) { { Text(pinsDontMatchError, color = MaterialTheme.colorScheme.error) } } else null, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColors.primaryColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text(cancelText) }
                    Button(onClick = { if (pin == confirmPin && pin.length >= 4) onPinSet(pin) else showError = true }, modifier = Modifier.weight(1f), enabled = pin.length >= 4 && confirmPin.length >= 4, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor)) { Text(saveText) }
                }
            }
        }
    }
}
