package pt.hitv.feature.settings.options.options.more.dialogs

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.settings.options.options.more.MoreOptionsViewModel

@Composable
fun RefreshDataConfirmDialog(
    viewModel: MoreOptionsViewModel,
    onDismiss: () -> Unit
) {
    val theme = getThemeColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                viewModel.triggerRefreshData()
                onDismiss()
            }) {
                Text(
                    text = "Confirm",
                    color = theme.primaryColor,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = theme.textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        title = {
            Text(
                text = "Refresh data?",
                color = theme.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "This will re-sync your channel list, EPG and VOD catalogue. It may take a while depending on your connection.",
                color = theme.textColor.copy(alpha = 0.9f),
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        },
        containerColor = theme.backgroundPrimary,
        shape = RoundedCornerShape(20.dp)
    )
}
