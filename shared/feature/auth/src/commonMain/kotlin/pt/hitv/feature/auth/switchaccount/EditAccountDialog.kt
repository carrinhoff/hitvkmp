package pt.hitv.feature.auth.switchaccount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.UserCredentials
import pt.hitv.feature.auth.util.LoginValidator

/**
 * Edit Account dialog — KMP port of the original `EditAccountDialog`.
 *
 * Pre-fills fields from the existing account. Password is left empty —
 * entering a new password changes it, leaving it blank keeps the current one.
 *
 * For M3U accounts (detected by hostname suffix or query string), only the
 * server URL and EPG URL fields are shown.
 */
@Composable
fun EditAccountDialog(
    user: UserCredentials,
    isLoading: Boolean,
    errorMessage: String?,
    onSave: (username: String, password: String, hostname: String, epgUrl: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = getThemeColors()

    var serverUrl by remember { mutableStateOf(user.hostname) }
    var username by remember { mutableStateOf(user.username) }
    var password by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf(user.epgUrl ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val isM3uAccount = remember(user) {
        user.hostname.endsWith(".m3u", ignoreCase = true) ||
                user.hostname.endsWith(".m3u8", ignoreCase = true) ||
                user.hostname.contains("?")
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = "Edit Account",
                color = themeColors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        validationError = null
                    },
                    label = { Text("Server URL") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primaryColor,
                        focusedLabelColor = themeColors.primaryColor,
                        cursorColor = themeColors.primaryColor,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor,
                        unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                if (!isM3uAccount) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            validationError = null
                        },
                        label = { Text("Username") },
                        singleLine = true,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primaryColor,
                            focusedLabelColor = themeColors.primaryColor,
                            cursorColor = themeColors.primaryColor,
                            focusedTextColor = themeColors.textColor,
                            unfocusedTextColor = themeColors.textColor,
                            unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Password") },
                        placeholder = {
                            Text(
                                "Leave blank to keep current password",
                                color = themeColors.textColor.copy(alpha = 0.4f)
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColors.primaryColor,
                            focusedLabelColor = themeColors.primaryColor,
                            cursorColor = themeColors.primaryColor,
                            focusedTextColor = themeColors.textColor,
                            unfocusedTextColor = themeColors.textColor,
                            unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }

                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = {
                        epgUrl = it
                        validationError = null
                    },
                    label = { Text("EPG URL (optional)") },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primaryColor,
                        focusedLabelColor = themeColors.primaryColor,
                        cursorColor = themeColors.primaryColor,
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor,
                        unfocusedLabelColor = themeColors.textColor.copy(alpha = 0.6f)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )

                val displayError = errorMessage ?: validationError
                if (displayError != null) {
                    Text(
                        text = displayError,
                        color = themeColors.primaryColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val sanitizedUrl = LoginValidator.sanitizeUrl(serverUrl)
                    val sanitizedUsername = LoginValidator.sanitizeUsername(username)
                    val sanitizedPassword = LoginValidator.sanitizePassword(password)
                    val sanitizedEpgUrl = epgUrl.trim().ifEmpty { null }

                    if (sanitizedUrl.isBlank()) {
                        validationError = "Server URL is required"
                        return@TextButton
                    }
                    if (!isM3uAccount && sanitizedUsername.isBlank()) {
                        validationError = "Username is required"
                        return@TextButton
                    }

                    onSave(sanitizedUsername, sanitizedPassword, sanitizedUrl, sanitizedEpgUrl)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.primaryColor
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(4.dp),
                        color = themeColors.primaryColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save changes", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.textColor.copy(alpha = 0.7f)
                )
            ) {
                Text("Cancel")
            }
        },
        containerColor = themeColors.backgroundSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}
