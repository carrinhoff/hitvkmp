package pt.hitv.feature.player.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.player.util.SleepTimerManager
import pt.hitv.core.model.ChannelEpgInfo

/**
 * Shared buffering indicator composable.
 */
@Composable
fun BufferingIndicator() {
    val themeColors = getThemeColors()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = themeColors.primaryColor, strokeWidth = 4.dp, modifier = Modifier.size(64.dp))
    }
}

/**
 * Stateless EPG overlay.
 */
@Composable
fun EpgInfoOverlay(epgInfo: ChannelEpgInfo) {
    val progress = remember(epgInfo.startTime, epgInfo.endTime) { epgInfo.calculateProgress(kotlinx.datetime.Clock.System.now().toEpochMilliseconds()) }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)), shape = RoundedCornerShape(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = epgInfo.programmeTitle ?: "No program info", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = epgInfo.programmeDescription ?: "", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)), color = Color.White, trackColor = Color.White.copy(alpha = 0.3f))
            }
        }
    }
}

/**
 * Error dialog with retry button.
 */
@Composable
fun PlaybackErrorDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    titleText: String = "Playback Error",
    retryText: String = "Retry",
    dismissText: String = "Dismiss"
) {
    val themeColors = getThemeColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText, color = themeColors.textColor, fontWeight = FontWeight.Bold) },
        text = { Text(errorMessage, color = themeColors.textColor.copy(alpha = 0.9f)) },
        confirmButton = { Button(onClick = { onDismiss(); onRetry() }, colors = ButtonDefaults.buttonColors(containerColor = themeColors.primaryColor, contentColor = themeColors.textColor)) { Icon(Icons.Default.Refresh, contentDescription = retryText, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(retryText) } },
        dismissButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = themeColors.textColor.copy(alpha = 0.7f))) { Text(dismissText) } },
        icon = { Icon(Icons.Default.Error, contentDescription = null, tint = themeColors.primaryColor) },
        containerColor = themeColors.backgroundSecondary
    )
}

/**
 * Sleep timer indicator badge.
 */
@Composable
fun SleepTimerIndicator(
    remainingMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    Box(
        modifier = modifier.clip(RoundedCornerShape(6.dp)).background(themeColors.primaryColor.copy(alpha = 0.2f)).clickable(onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Timer, contentDescription = "Sleep Timer Active", tint = themeColors.primaryColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(text = SleepTimerManager.formatRemainingTime(remainingMs), color = themeColors.primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
