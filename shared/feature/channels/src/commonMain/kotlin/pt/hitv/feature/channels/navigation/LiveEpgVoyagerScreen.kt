package pt.hitv.feature.channels.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.feature.channels.epg.EpgCategorySelectionScreen
import pt.hitv.feature.channels.epg.EpgScreenMobile
import pt.hitv.feature.channels.epg.EpgThemeColors
import pt.hitv.epg.data.filterEpgData
import pt.hitv.epg.domain.EPGChannel
import pt.hitv.feature.player.platform.launchChannelPlayer

/**
 * The "Live with EPG" destination, registered against `HitvScreen.LIVE_EPG`.
 *
 * Two-step flow, matching the original's `DetailRoutes` EPG route: pick a category, then render
 * that category's programme grid. Back from the grid returns to the category list; back from the
 * category list leaves the screen.
 *
 * Deferred against the original, and deliberately so — see `EpgScreenMobile`'s KDoc:
 * programme reminders (need an iOS notification `expect`/`actual`) and the catch-up paywall
 * (needs billing). Neither is stubbed with a dead button.
 */
class LiveEpgVoyagerScreen : Screen {
    override val key = "LiveEpg"

    @Composable
    override fun Content() {
        val viewModel: StreamViewModel = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uiState by viewModel.uiState.collectAsState()

        val appTheme = getThemeColors()
        val themeColors = remember(appTheme) {
            EpgThemeColors(
                backgroundPrimary = appTheme.backgroundPrimary,
                backgroundSecondary = appTheme.backgroundSecondary,
                cardColor = appTheme.cardColor,
                primaryColor = appTheme.primaryColor,
                textColor = appTheme.textColor,
                textSecondaryColor = appTheme.textColor.copy(alpha = 0.8f),
            )
        }

        var selectedCategoryId by remember { mutableStateOf<String?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        var scrollIndex by remember { mutableStateOf(0) }
        var scrollOffset by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) { viewModel.getCategoriesWithEpgData() }

        // Tuning is driven through state rather than called straight from the click lambda: the
        // EPG grid works in EPG identifiers, so the real Channel row has to be looked up by
        // `epgChannelId` first, and that is a suspend DB call.
        var channelToPlay by remember { mutableStateOf<EPGChannel?>(null) }
        LaunchedEffect(channelToPlay) {
            val pending = channelToPlay ?: return@LaunchedEffect
            channelToPlay = null
            val resolved = viewModel.getChannelByEpgIdDirect(pending.channelID)
            val url = resolved?.streamUrl
            if (!url.isNullOrBlank()) {
                launchChannelPlayer(
                    url = url,
                    name = resolved.name ?: pending.name,
                    logoUrl = resolved.streamIcon ?: pending.imageURL,
                )
            }
        }

        val categoryId = selectedCategoryId
        if (categoryId == null) {
            EpgCategorySelectionScreen(
                categoriesWithEpg = uiState.categoriesWithEpg,
                themeColors = themeColors,
                isLoading = uiState.isLoadingEpgCategories,
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                scrollIndex = scrollIndex,
                scrollOffset = scrollOffset,
                onScrollPositionChanged = { index, offset ->
                    scrollIndex = index
                    scrollOffset = offset
                },
                onCategorySelected = { id ->
                    selectedCategoryId = id
                    viewModel.getProgrammesForCategory(id)
                },
                onBackPressed = { navigator.pop() },
            )
        } else {
            val epgData = remember(uiState.programmesForCategory) {
                filterEpgData(uiState.programmesForCategory)
            }

            EpgScreenMobile(
                epgData = epgData,
                themeColors = themeColors,
                onChannelClick = { channel -> channelToPlay = channel },
                onChannelLongClick = null,
                onProgramActionClick = { _, channel ->
                    // Live programme -> tune to the channel. Past programme on a catch-up
                    // channel -> the same, since resuming a *specific* past programme needs the
                    // catch-up deeplink that is still outstanding (KMP_MIGRATION_AUDIT.md §5).
                    // Future programmes surface no action at all, so they never reach here.
                    channelToPlay = channel
                },
                onBackPressed = { selectedCategoryId = null },
            )
        }
    }
}
