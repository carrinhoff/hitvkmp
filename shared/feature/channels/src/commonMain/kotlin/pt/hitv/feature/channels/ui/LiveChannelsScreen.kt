package pt.hitv.feature.channels.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemContentType
import app.cash.paging.compose.itemKey
import kotlinx.coroutines.launch
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.data.paging.CHANNEL_FILTER_ALL
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Channel
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.feature.channels.ui.components.ChannelListItem
import pt.hitv.feature.channels.ui.mobile.MobileChannelsLayout

/**
 * Main entry point for the Live Channels screen.
 * Displays a searchable, filterable list of live TV channels with category tabs.
 */
@Composable
fun LiveChannelsScreen(
    viewModel: StreamViewModel,
    analyticsHelper: AnalyticsHelper,
    onChannelClicked: (Channel, Int) -> Unit,
    onNavigateToEpg: () -> Unit = {},
    scrollToTopSignal: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeColors = getThemeColors()

    // Log screen view + refresh data (handles post-sync reload)
    LaunchedEffect(Unit) {
        analyticsHelper.logScreenView(ScreenName.LIVE_CHANNELS, "LiveChannelsScreen")
        viewModel.getFavorites()
        viewModel.fetchRecentlyViewedChannels()
        viewModel.fetchCategoryCounts()
    }

    MobileChannelsLayout(
        viewModel = viewModel,
        uiState = uiState,
        onChannelClicked = onChannelClicked,
        onCategorySelected = { categoryId ->
            viewModel.setCategoryFilter(categoryId)
        },
        onSearchQueryChanged = { query ->
            viewModel.setSearchQuery(query)
        },
        scrollToTopSignal = scrollToTopSignal
    )
}
