package pt.hitv.feature.settings.options.options.categories

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.ui.categories.ManageCategoriesScreen
import pt.hitv.core.ui.categories.ManageCategoriesViewModel

/**
 * Voyager wrapper hosting the shared [ManageCategoriesScreen] (core/ui) under
 * the settings feature. Delegates all behaviour (tabs Channels/Movies/Series,
 * pin/hide, set-default, bulk show/hide) to the existing shared composable,
 * so the settings feature only owns the navigation + Koin wiring.
 *
 * Reorder-by-drag is not used: the underlying [ManageCategoriesViewModel] /
 * [CategoryPreference] data model has no `order` column, and the original
 * hitv project treats "reorder" as the combination of pin + default —
 * pinned rows float to the top. Pin + hide via the existing Row actions is
 * the canonical reorder affordance for this codebase.
 */
class ManageCategoriesVoyagerScreen : Screen {
    override val key = "ManageCategories"

    @Composable
    override fun Content() {
        val viewModel: ManageCategoriesViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val colors = getThemeColors()

        ManageCategoriesScreen(
            viewModel = viewModel,
            backgroundColor = colors.backgroundPrimary,
            primaryColor = colors.primaryColor,
            secondaryBackgroundColor = colors.backgroundSecondary,
            onNavigateBack = { navigator.pop() }
        )
    }
}
