package pt.hitv.feature.settings.options.options.sync

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.sync.BackgroundSyncManager

/**
 * Voyager wrapper around [BackgroundSyncSettingsScreen]. Keeps Koin wiring
 * out of the composable so the screen stays easily previewable.
 */
class BackgroundSyncSettingsVoyagerScreen : Screen {
    override val key = "BackgroundSyncSettings"

    @Composable
    override fun Content() {
        val viewModel: BackgroundSyncSettingsViewModel = koinInject()
        val syncManager: BackgroundSyncManager = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        BackgroundSyncSettingsScreen(
            viewModel = viewModel,
            syncManager = syncManager,
            onNavigateBack = { navigator.pop() }
        )
    }
}
