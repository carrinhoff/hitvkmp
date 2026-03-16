package pt.hitv.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A composable that displays a paging error state with an error icon, message, and retry button.
 *
 * @param message The error message to display, or null to use the default message
 * @param onRetry Callback invoked when the user taps the "Retry" button
 * @param modifier Optional modifier for the root layout
 * @param defaultErrorMessage Default error message when message is null
 * @param retryLabel Label for the retry button
 */
@Composable
fun PagingErrorState(
    message: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    defaultErrorMessage: String = "Failed to load",
    retryLabel: String = "Retry"
) {
    val themeColors = getThemeColors()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = themeColors.textColor.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message ?: defaultErrorMessage,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = themeColors.textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColors.primaryColor
            )
        ) {
            Text(
                text = retryLabel,
                color = themeColors.textColor
            )
        }
    }
}
