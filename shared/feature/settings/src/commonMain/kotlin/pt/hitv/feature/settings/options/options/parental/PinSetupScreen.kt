package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Full-screen PIN setup (4-digit PIN + confirm) — mirrors the dialog UX
 * but as a dedicated screen for onboarding flows that need more breathing room.
 * Uses [ParentalControlViewModel.setPin].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupScreen(
    viewModel: ParentalControlViewModel,
    onNavigateBack: () -> Unit,
    onPinSet: () -> Unit,
    titleText: String = "Set PIN",
    subtitleText: String = "Create a 4-digit PIN to protect categories.",
    pinLabel: String = "Enter PIN",
    confirmLabel: String = "Confirm PIN",
    saveText: String = "Save",
    mismatchError: String = "PINs don't match",
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = themeColors.primaryColor,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = subtitleText,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                label = { Text(pinLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirm = it },
                label = { Text(confirmLabel) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = showError,
                supportingText = if (showError) {
                    { Text(mismatchError, color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeColors.primaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (pin == confirm && pin.length >= 4) {
                        viewModel.setPin(pin)
                        onPinSet()
                    } else showError = true
                },
                enabled = pin.length >= 4 && confirm.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(saveText, fontWeight = FontWeight.Bold)
            }
        }
    }
}
