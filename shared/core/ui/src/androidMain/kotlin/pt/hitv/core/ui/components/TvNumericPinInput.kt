package pt.hitv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.theme.ThemeManager

/**
 * TV-optimized numeric PIN input with grid layout (Android-only).
 * Makes PIN entry easier on Android TV with D-pad navigation.
 *
 * @param pin Current PIN value
 * @param onPinChange Callback when PIN changes
 * @param maxLength Maximum PIN length
 * @param showPin Whether to show PIN digits
 */
@Composable
fun TvNumericPinInput(
    pin: String,
    onPinChange: (String) -> Unit,
    maxLength: Int = 6,
    showPin: Boolean = false
) {
    val themeColors = getThemeColors()
    val focusRequesters = remember { List(12) { FocusRequester() } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // PIN Display
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(maxLength) { index ->
                PinDot(
                    isFilled = index < pin.length,
                    digit = if (showPin && index < pin.length) pin[index].toString() else null,
                    themeColors = themeColors
                )
            }
        }

        // Numeric Grid (3x4 layout like a phone dialer)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (col in 0..2) {
                        val digit = (row * 3 + col + 1).toString()
                        val index = row * 3 + col

                        NumericButton(
                            text = digit,
                            onClick = {
                                if (pin.length < maxLength) {
                                    onPinChange(pin + digit)
                                }
                            },
                            focusRequester = focusRequesters[index],
                            themeColors = themeColors,
                            requestFocusOnStart = index == 0
                        )
                    }
                }
            }

            // Bottom row (Clear, 0, Backspace)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(80.dp))

                NumericButton(
                    text = "0",
                    onClick = {
                        if (pin.length < maxLength) {
                            onPinChange(pin + "0")
                        }
                    },
                    focusRequester = focusRequesters[9],
                    themeColors = themeColors
                )

                IconNumericButton(
                    icon = Icons.Default.Backspace,
                    onClick = {
                        if (pin.isNotEmpty()) {
                            onPinChange(pin.dropLast(1))
                        }
                    },
                    focusRequester = focusRequesters[10],
                    themeColors = themeColors
                )
            }
        }
    }
}

@Composable
private fun PinDot(
    isFilled: Boolean,
    digit: String?,
    themeColors: ThemeManager.AppTheme
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFilled) themeColors.primaryColor.copy(alpha = 0.3f)
                else themeColors.backgroundSecondary
            )
            .border(
                width = 2.dp,
                color = if (isFilled) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
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
                    .clip(CircleShape)
                    .background(themeColors.primaryColor)
            )
        }
    }
}

@Composable
private fun NumericButton(
    text: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    themeColors: ThemeManager.AppTheme,
    requestFocusOnStart: Boolean = false
) {
    var hasFocus by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.isFocused }
            .focusable()
            .then(
                if (hasFocus) {
                    Modifier.border(
                        width = 3.dp,
                        color = themeColors.textColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hasFocus) themeColors.primaryColor else themeColors.backgroundSecondary,
            contentColor = themeColors.textColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

    if (requestFocusOnStart) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun IconNumericButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    themeColors: ThemeManager.AppTheme
) {
    var hasFocus by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { hasFocus = it.isFocused }
            .focusable()
            .then(
                if (hasFocus) {
                    Modifier.border(
                        width = 3.dp,
                        color = themeColors.textColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (hasFocus) themeColors.primaryColor else themeColors.backgroundSecondary,
            contentColor = themeColors.textColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
    }
}
