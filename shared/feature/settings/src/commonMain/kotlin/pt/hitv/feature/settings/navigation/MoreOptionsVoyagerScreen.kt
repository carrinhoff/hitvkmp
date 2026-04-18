package pt.hitv.feature.settings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.navigation.navigateToFeedback
import pt.hitv.core.navigation.navigateToLiveEpg
import pt.hitv.core.navigation.navigateToManageCategories
import pt.hitv.core.navigation.navigateToParentalControl
import pt.hitv.core.navigation.navigateToSwitchAccount
import pt.hitv.core.navigation.navigateToThemeSettings
import pt.hitv.core.navigation.navigateToTipsAndFeatures
import pt.hitv.feature.settings.options.options.more.MobileMoreOptionsScreen
import pt.hitv.feature.settings.options.options.more.MoreOptionsViewModel
import pt.hitv.feature.settings.options.options.more.dialogs.AppLanguageDialog
import pt.hitv.feature.settings.options.options.more.dialogs.LiveBufferSizeDialog
import pt.hitv.feature.settings.options.options.more.dialogs.PlayerEngineDialog
import pt.hitv.feature.settings.options.options.more.dialogs.RefreshDataConfirmDialog
import pt.hitv.feature.settings.options.options.sync.BackgroundSyncSettingsViewModel

private const val DISCORD_URL = "https://discord.gg/Ptg9VXyMT7"

class MoreOptionsVoyagerScreen : Screen {
    override val key = "MoreOptions"

    @Composable
    override fun Content() {
        val viewModel: MoreOptionsViewModel = koinInject()
        val syncViewModel: BackgroundSyncSettingsViewModel = koinInject()
        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow

        var showLanguageDialog by remember { mutableStateOf(false) }
        var showPlayerEngineDialog by remember { mutableStateOf(false) }
        var showBufferDialog by remember { mutableStateOf(false) }
        var showRefreshDialog by remember { mutableStateOf(false) }

        MobileMoreOptionsScreen(
            viewModel = viewModel,
            syncViewModel = syncViewModel,
            onSwitchAccountClick = { navigator.navigateToSwitchAccount() },
            onManageCategoriesClick = { navigator.navigateToManageCategories() },
            onParentalControlClick = { navigator.navigateToParentalControl() },
            onThemeClick = { navigator.navigateToThemeSettings() },
            onLanguageClick = { showLanguageDialog = true },
            onPlayerEngineClick = { showPlayerEngineDialog = true },
            onBufferSizeClick = { showBufferDialog = true },
            onRefreshDataClick = { showRefreshDialog = true },
            onTipsAndFeaturesClick = { navigator.navigateToTipsAndFeatures() },
            onEpgClick = { navigator.navigateToLiveEpg() },
            onFeedbackClick = { navigator.navigateToFeedback() },
            onDiscordClick = {
                runCatching { uriHandler.openUri(DISCORD_URL) }
            }
        )

        if (showLanguageDialog) {
            AppLanguageDialog(
                viewModel = viewModel,
                onDismiss = { showLanguageDialog = false }
            )
        }
        if (showPlayerEngineDialog) {
            PlayerEngineDialog(
                viewModel = viewModel,
                onDismiss = { showPlayerEngineDialog = false }
            )
        }
        if (showBufferDialog) {
            LiveBufferSizeDialog(
                viewModel = viewModel,
                onDismiss = { showBufferDialog = false }
            )
        }
        if (showRefreshDialog) {
            RefreshDataConfirmDialog(
                viewModel = viewModel,
                onDismiss = { showRefreshDialog = false }
            )
        }
    }
}
