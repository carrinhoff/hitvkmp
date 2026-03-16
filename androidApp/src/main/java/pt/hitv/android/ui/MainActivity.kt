package pt.hitv.android.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import pt.hitv.core.designsystem.theme.AppThemeProvider

/**
 * Main entry point Activity for the Android app.
 *
 * Sets up:
 * - Splash screen (via AndroidX SplashScreen)
 * - Edge-to-edge display
 * - Compose content with shared theme and navigation
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeProvider {
                HitvApp()
            }
        }
    }
}
