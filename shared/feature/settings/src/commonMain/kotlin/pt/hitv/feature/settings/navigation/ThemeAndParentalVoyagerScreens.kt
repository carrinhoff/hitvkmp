package pt.hitv.feature.settings.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.navigation.navigateToParentalCategoryLock
import pt.hitv.feature.settings.options.options.parental.CategoryLockScreen
import pt.hitv.feature.settings.options.options.parental.ParentalControlScreen
import pt.hitv.feature.settings.options.options.parental.ParentalControlViewModel
import pt.hitv.feature.settings.options.options.parental.PinSetupScreen
import pt.hitv.feature.settings.options.options.theme.ThemeSettingsViewModel
import pt.hitv.feature.settings.options.options.theme.ThemeStudioScreen

class ThemeStudioVoyagerScreen : Screen {
    override val key = "ThemeStudio"

    @Composable
    override fun Content() {
        val viewModel: ThemeSettingsViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        ThemeStudioScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.pop() }
        )
    }
}

class ParentalControlVoyagerScreen : Screen {
    override val key = "ParentalControl"

    @Composable
    override fun Content() {
        val viewModel: ParentalControlViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        ParentalControlScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.pop() },
            onNavigateToCategoryLock = { navigator.navigateToParentalCategoryLock() }
        )
    }
}

class ParentalPinSetupVoyagerScreen : Screen {
    override val key = "ParentalPinSetup"

    @Composable
    override fun Content() {
        val viewModel: ParentalControlViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        PinSetupScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.pop() },
            onPinSet = { navigator.pop() }
        )
    }
}

class ParentalCategoryLockVoyagerScreen : Screen {
    override val key = "ParentalCategoryLock"

    @Composable
    override fun Content() {
        val viewModel: ParentalControlViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        CategoryLockScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.pop() }
        )
    }
}
