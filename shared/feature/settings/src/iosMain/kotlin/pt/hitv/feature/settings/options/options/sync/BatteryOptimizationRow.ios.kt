package pt.hitv.feature.settings.options.options.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * iOS actual: empty. iOS has no equivalent system setting to
 * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; BGTaskScheduler cadence is OS-managed
 * and cannot be whitelisted by the app, so the row is hidden entirely.
 */
@Composable
actual fun BatteryOptimizationRow(
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier
) {
    // Intentionally empty.
}
