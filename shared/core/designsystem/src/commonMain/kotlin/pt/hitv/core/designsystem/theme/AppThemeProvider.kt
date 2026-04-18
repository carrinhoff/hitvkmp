package pt.hitv.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

@Composable
fun AppThemeProvider(
    content: @Composable () -> Unit
) {
    val themeManager: ThemeManager = koinInject()

    // Observe the ThemeManager StateFlow for instant hot-swap when the user picks
    // a theme in ThemeStudioScreen. Falls back to a 1s reconciliation loop to catch
    // any out-of-band writes to the underlying preference.
    val currentTheme by themeManager.currentThemeFlow.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val persisted = themeManager.getCurrentTheme()
            if (persisted != themeManager.currentThemeFlow.value) {
                // Reconcile if the preference was written without going through setTheme.
                themeManager.selectThemeUngated(persisted)
            }
        }
    }

    val colorScheme = darkColorScheme(
        primary = currentTheme.primaryColor,
        background = currentTheme.backgroundPrimary,
        surface = currentTheme.cardColor,
        onPrimary = Color.White,
        onBackground = currentTheme.textColor,
        onSurface = currentTheme.textColor,
        secondary = currentTheme.backgroundSecondary,
        surfaceVariant = currentTheme.backgroundSecondary,
        primaryContainer = currentTheme.primaryColor.copy(alpha = 0.8f),
        secondaryContainer = currentTheme.backgroundSecondary,
        onPrimaryContainer = Color.White,
        onSecondaryContainer = currentTheme.textColor,
        tertiary = currentTheme.primaryColor.copy(alpha = 0.7f),
        onTertiary = Color.White,
        outline = currentTheme.primaryColor.copy(alpha = 0.5f),
        outlineVariant = currentTheme.textColor.copy(alpha = 0.3f)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * Extension to get theme colors easily - now reactive to theme changes.
 * Uses Koin to inject ThemeManager.
 */
@Composable
fun getThemeColors(): ThemeManager.AppTheme {
    val themeManager: ThemeManager = koinInject()
    val currentTheme by themeManager.currentThemeFlow.collectAsState()
    return currentTheme
}
