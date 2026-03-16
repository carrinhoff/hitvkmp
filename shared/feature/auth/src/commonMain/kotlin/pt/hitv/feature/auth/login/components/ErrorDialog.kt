package pt.hitv.feature.auth.login.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pt.hitv.feature.auth.util.LoginValidator
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Modern error dialog that displays validation and authentication errors
 * with appropriate styling based on error type.
 */
@Composable
fun ErrorDialog(
    errorType: LoginValidator.ErrorType,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    val errorColor = themeColors.primaryColor

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.backgroundSecondary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        errorColor.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = themeColors.textColor.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(errorColor.copy(alpha = 0.15f))
                                .border(
                                    width = 2.dp,
                                    color = errorColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(36.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getErrorIcon(errorType),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = errorColor
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = getErrorTitle(errorType),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.textColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = message,
                            fontSize = 15.sp,
                            color = themeColors.textColor.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primaryColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text(
                                text = "OK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = themeColors.textColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact error dialog for less critical errors
 */
@Composable
fun CompactErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(300)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(200)
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.backgroundSecondary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = themeColors.primaryColor
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = themeColors.textColor,
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "OK",
                            color = themeColors.primaryColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private fun getErrorIcon(errorType: LoginValidator.ErrorType): ImageVector {
    return when (errorType) {
        LoginValidator.ErrorType.AUTH_ERROR,
        LoginValidator.ErrorType.INVALID_CREDENTIALS -> Icons.Default.Error

        LoginValidator.ErrorType.NETWORK_ERROR,
        LoginValidator.ErrorType.TIMEOUT_ERROR,
        LoginValidator.ErrorType.SSL_ERROR,
        LoginValidator.ErrorType.SERVER_ERROR -> Icons.Default.Warning

        LoginValidator.ErrorType.PARSING_ERROR,
        LoginValidator.ErrorType.INVALID_URL,
        LoginValidator.ErrorType.EMPTY_FIELD -> Icons.Default.Info

        LoginValidator.ErrorType.UNKNOWN_ERROR -> Icons.Default.Error
    }
}

private fun getErrorTitle(errorType: LoginValidator.ErrorType): String {
    return when (errorType) {
        LoginValidator.ErrorType.AUTH_ERROR -> "Authentication Failed"
        LoginValidator.ErrorType.NETWORK_ERROR -> "Connection Error"
        LoginValidator.ErrorType.TIMEOUT_ERROR -> "Request Timeout"
        LoginValidator.ErrorType.SSL_ERROR -> "Security Error"
        LoginValidator.ErrorType.SERVER_ERROR -> "Server Error"
        LoginValidator.ErrorType.PARSING_ERROR -> "Invalid Format"
        LoginValidator.ErrorType.INVALID_URL -> "Invalid URL"
        LoginValidator.ErrorType.INVALID_CREDENTIALS -> "Invalid Input"
        LoginValidator.ErrorType.EMPTY_FIELD -> "Missing Information"
        LoginValidator.ErrorType.UNKNOWN_ERROR -> "Error"
    }
}
