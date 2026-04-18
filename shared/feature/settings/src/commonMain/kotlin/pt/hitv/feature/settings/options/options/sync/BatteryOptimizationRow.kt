package pt.hitv.feature.settings.options.options.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Android: renders a clickable row that opens
 * [Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS] for the app.
 *
 * iOS: empty composable — iOS has no equivalent system setting, so the row is
 * hidden entirely per the plan.
 */
@Composable
expect fun BatteryOptimizationRow(
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier
)
