package pt.hitv.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.voyager.navigator.Navigator
import pt.hitv.core.designsystem.theme.AppThemeProvider

/**
 * iOS entry point creating a UIViewController hosting the shared Compose UI.
 *
 * Called from Swift via `MainViewControllerKt.MainViewController()`.
 * Wraps the app in the shared theme provider and Voyager Navigator.
 */
fun MainViewController() = ComposeUIViewController {
    AppThemeProvider {
        val initialScreen = ScreenRegistry.create(HitvScreen.CHANNELS)
        Navigator(initialScreen)
    }
}
