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

    // Read theme SYNCHRONOUSLY on first composition to avoid flashing default theme
    var currentTheme by remember { mutableStateOf(themeManager.getCurrentTheme()) }

    // Continue to observe changes
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val newTheme = themeManager.getCurrentTheme()
            if (newTheme != currentTheme) {
                currentTheme = newTheme
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

    var currentTheme by remember { mutableStateOf(themeManager.getCurrentTheme()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            val newTheme = themeManager.getCurrentTheme()
            if (newTheme != currentTheme) {
                currentTheme = newTheme
            }
        }
    }

    return currentTheme
}
