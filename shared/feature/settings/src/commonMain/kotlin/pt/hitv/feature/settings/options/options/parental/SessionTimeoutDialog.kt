package pt.hitv.feature.settings.options.options.parental

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Radio-list dialog for picking the parental-control session timeout.
 * Options: 5 min / 15 min / 30 min / 1 hour / never (0).
 * Calls [ParentalControlViewModel.setSessionTimeout] via the [onSelect] callback.
 */
@Composable
fun SessionTimeoutDialog(
    currentMinutes: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    titleText: String = "Session Timeout",
    cancelText: String = "Cancel"
) {
    val themeColors = getThemeColors()

    data class Option(val minutes: Int, val label: String)
    val options = listOf(
        Option(5, "5 minutes"),
        Option(15, "15 minutes"),
        Option(30, "30 minutes"),
        Option(60, "1 hour"),
        Option(0, "Never (always ask)")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.backgroundSecondary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    titleText,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.minutes) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option.minutes == currentMinutes,
                            onClick = { onSelect(option.minutes) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = themeColors.primaryColor,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option.label, color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(cancelText, color = themeColors.primaryColor)
                    }
                }
            }
        }
    }
}
