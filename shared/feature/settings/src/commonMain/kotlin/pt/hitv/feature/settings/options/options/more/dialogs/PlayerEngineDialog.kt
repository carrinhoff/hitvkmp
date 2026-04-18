package pt.hitv.feature.settings.options.options.more.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.settings.options.options.more.MoreOptionsViewModel

@Composable
fun PlayerEngineDialog(
    viewModel: MoreOptionsViewModel,
    onDismiss: () -> Unit
) {
    val theme = getThemeColors()
    val currentEngine by viewModel.playerEngine.collectAsState()

    val engines = listOf(
        Triple("exoplayer", "ExoPlayer", "Google's ExoPlayer — recommended for most streams."),
        Triple("vlc", "VLC", "VLC libVLC — supports more formats, higher battery usage.")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = theme.textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        title = {
            Text(
                text = "Select Player Engine",
                color = theme.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                engines.forEach { (engineId, engineName, description) ->
                    val isSelected = currentEngine == engineId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setPlayerEngine(engineId) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) theme.primaryColor.copy(alpha = 0.15f)
                                else theme.textColor.copy(alpha = 0.04f),
                        border = if (isSelected) BorderStroke(2.dp, theme.primaryColor.copy(alpha = 0.4f))
                                 else BorderStroke(1.dp, theme.textColor.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = engineName,
                                    color = theme.textColor,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    color = theme.textColor.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = theme.backgroundPrimary,
        shape = RoundedCornerShape(20.dp)
    )
}
