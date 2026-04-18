package pt.hitv.feature.settings.options.options.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.SyncTaskStatus
import pt.hitv.core.sync.TASK_CONTENT
import pt.hitv.core.sync.TASK_EPG

/**
 * Compact status indicator observing [BackgroundSyncManager.statusFlow].
 * Shows a card with one row per task (EPG + content). Each row renders:
 *  - an icon
 *  - the task label
 *  - a status dot (color-coded)
 *  - the textual status (Running / Succeeded / Failed / Scheduled / Not Scheduled).
 *
 * Pure commonMain — no platform dependencies. Consumes the [syncManager]
 * statusFlow directly so this composable can be dropped into any screen.
 */
@Composable
fun SyncStatusIndicator(
    syncManager: BackgroundSyncManager,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val statusMap by syncManager.statusFlow.collectAsState()
    val epg = statusMap[TASK_EPG] ?: SyncTaskStatus.NotScheduled
    val content = statusMap[TASK_CONTENT] ?: SyncTaskStatus.NotScheduled

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Sync Status",
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))
        StatusRow(
            icon = Icons.Rounded.LiveTv,
            label = "EPG",
            status = epg,
            textColor = textColor,
            textSecondaryColor = textSecondaryColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        StatusRow(
            icon = Icons.Rounded.Cloud,
            label = "Content",
            status = content,
            textColor = textColor,
            textSecondaryColor = textSecondaryColor
        )
    }
}

@Composable
private fun StatusRow(
    icon: ImageVector,
    label: String,
    status: SyncTaskStatus,
    textColor: Color,
    textSecondaryColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textSecondaryColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        StatusDot(status)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.label(),
            color = textSecondaryColor,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatusDot(status: SyncTaskStatus) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(status.dotColor(), CircleShape)
    )
}

private fun SyncTaskStatus.label(): String = when (this) {
    SyncTaskStatus.NotScheduled -> "Not scheduled"
    SyncTaskStatus.Scheduled -> "Scheduled"
    SyncTaskStatus.Running -> "Running"
    SyncTaskStatus.Succeeded -> "Succeeded"
    SyncTaskStatus.Failed -> "Failed"
}

private fun SyncTaskStatus.dotColor(): Color = when (this) {
    SyncTaskStatus.NotScheduled -> Color(0xFF9E9E9E)
    SyncTaskStatus.Scheduled -> Color(0xFF03A9F4)
    SyncTaskStatus.Running -> Color(0xFFFFC107)
    SyncTaskStatus.Succeeded -> Color(0xFF4CAF50)
    SyncTaskStatus.Failed -> Color(0xFFF44336)
}
