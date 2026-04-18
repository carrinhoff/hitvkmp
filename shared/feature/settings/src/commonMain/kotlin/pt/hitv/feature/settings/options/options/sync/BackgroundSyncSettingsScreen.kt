package pt.hitv.feature.settings.options.options.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.sync.BackgroundSyncManager

/**
 * Full-page Background Sync settings screen. Layout (top to bottom) matches
 * the original hitv project:
 *  1. Header with back button + title + subtitle
 *  2. Master enable toggle
 *  3. EPG interval radio list (6h / 12h / 24h)
 *  4. Content interval radio list (1 day / 3 days / 7 days)
 *  5. Wifi-only toggle
 *  6. Battery optimization row (Android only — iOS `actual` is empty)
 *  7. Sync status indicator (Team α's statusFlow)
 */
@Composable
fun BackgroundSyncSettingsScreen(
    viewModel: BackgroundSyncSettingsViewModel,
    syncManager: BackgroundSyncManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val secondaryBackground = themeColors.backgroundSecondary
    val primaryColor = themeColors.primaryColor
    val textColor = Color.White
    val textSecondaryColor = Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        secondaryBackground,
                        backgroundColor.copy(alpha = 0.9f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Header(
                    onNavigateBack = onNavigateBack,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // Master toggle
            item {
                SectionHeader(title = "Sync", primaryColor = primaryColor)
            }
            item {
                ToggleRow(
                    icon = Icons.Rounded.Sync,
                    title = "Background Sync",
                    subtitle = "Automatically refresh while the app is closed",
                    checked = state.enabled,
                    onToggle = { viewModel.setEnabled(it) },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // EPG interval
            item {
                SectionHeader(title = "EPG refresh interval", primaryColor = primaryColor)
            }
            items(EPG_INTERVAL_OPTIONS_HOURS) { hours ->
                RadioRow(
                    label = formatHours(hours),
                    selected = state.epgIntervalHours == hours,
                    enabled = state.enabled,
                    onClick = { viewModel.setEpgIntervalHours(hours) },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // Content interval
            item {
                SectionHeader(title = "Content refresh interval", primaryColor = primaryColor)
            }
            items(CONTENT_INTERVAL_OPTIONS_DAYS) { days ->
                RadioRow(
                    label = formatDays(days),
                    selected = state.contentIntervalDays == days,
                    enabled = state.enabled,
                    onClick = { viewModel.setContentIntervalDays(days) },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // Wifi-only
            item {
                SectionHeader(title = "Network", primaryColor = primaryColor)
            }
            item {
                ToggleRow(
                    icon = Icons.Rounded.Wifi,
                    title = "Wi-Fi only",
                    subtitle = "Only sync while on Wi-Fi",
                    checked = state.wifiOnly,
                    onToggle = { viewModel.setWifiOnly(it) },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // Battery optimization (Android only — iOS actual returns empty composable)
            item {
                BatteryOptimizationRow(
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    modifier = Modifier
                )
            }

            // Run now action
            item {
                ActionRow(
                    icon = Icons.Rounded.PlayArrow,
                    title = "Sync now",
                    subtitle = "Fire EPG + content sync once",
                    onClick = { viewModel.runNow() },
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            // Status
            item {
                SectionHeader(title = "Status", primaryColor = primaryColor)
            }
            item {
                SyncStatusIndicator(
                    syncManager = syncManager,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Private helpers
// ---------------------------------------------------------------------------

private fun formatHours(hours: Long): String = when (hours) {
    1L -> "Every hour"
    else -> "Every $hours hours"
}

private fun formatDays(days: Long): String = when (days) {
    1L -> "Every day"
    else -> "Every $days days"
}

@Composable
private fun Header(
    onNavigateBack: () -> Unit,
    textColor: Color,
    textSecondaryColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(textColor.copy(alpha = 0.08f), CircleShape)
                .clip(CircleShape)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = textColor.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Background Sync",
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Keep content fresh automatically",
                color = textSecondaryColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, primaryColor: Color) {
    Text(
        text = title,
        color = primaryColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(textColor.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = textSecondaryColor,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            PillToggle(checked = checked, primaryColor = primaryColor, textColor = textColor)
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    val borderColor = if (selected) primaryColor.copy(alpha = 0.6f) else textColor.copy(alpha = 0.1f)
    val bgColor = if (selected) primaryColor.copy(alpha = 0.12f) else textColor.copy(alpha = 0.04f)
    val labelColor = if (enabled) textColor else textSecondaryColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioDot(selected = selected, primaryColor = primaryColor, textColor = textColor)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = labelColor,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean, primaryColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(Color.Transparent, CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) primaryColor else textColor.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(primaryColor, CircleShape)
            )
        }
    }
}

@Composable
private fun PillToggle(checked: Boolean, primaryColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(22.dp)
            .background(
                color = if (checked) primaryColor else textColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(11.dp)
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .background(color = Color.White, shape = CircleShape)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    color = textSecondaryColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}
