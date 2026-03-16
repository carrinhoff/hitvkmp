package pt.hitv.feature.series.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import pt.hitv.core.model.Category
import pt.hitv.core.model.TvShow
import pt.hitv.core.data.paging.FILTER_ALL
import pt.hitv.core.data.paging.FILTER_FAVORITES
import pt.hitv.core.data.paging.FILTER_RECENTLY_VIEWED
import pt.hitv.core.data.paging.FILTER_LAST_ADDED
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.compose.CategorySectionSkeleton

/**
 * Main router screen for Series feature (commonMain).
 * Platform detection is handled by the caller, which passes isTvDevice.
 * TV layout composables are in androidMain.
 */
@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel,
    analyticsHelper: AnalyticsHelper,
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit,
    onNavigateToCategory: (categoryId: String, categoryName: String) -> Unit,
    onManageCategoriesClick: () -> Unit,
    scrollToTopSignal: Int = 0,
    onScreenEntered: Boolean = false
) {
    val seriesUiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Fetch last added and continue watching series
    LaunchedEffect(Unit) {
        viewModel.fetchLastAddedTvShows()
        viewModel.fetchContinueWatchingSeries()
    }

    // Set analytics
    LaunchedEffect(Unit) {
        delay(100)
        analyticsHelper.logScreenView(ScreenName.SERIES, "SeriesScreen")
    }

    val themeColors = getThemeColors()

    // Show shimmer skeleton when loading
    if (seriesUiState.categories.isEmpty() && seriesUiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(themeColors.backgroundPrimary)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) { index ->
                    CategorySectionSkeleton(
                        title = when (index) {
                            0 -> "Favorites"
                            1 -> "Recently Viewed"
                            else -> "Category ${index - 1}"
                        }
                    )
                }
            }
        }
        return
    }

    // Mobile Layout (TV layout is in androidMain)
    // The caller is responsible for routing to TV layout on Android TV devices
    MobileSeriesRouter(
        viewModel = viewModel,
        analyticsHelper = analyticsHelper,
        onSeriesClicked = onSeriesClicked,
        onManageCategoriesClick = onManageCategoriesClick,
        onNavigateToCategory = onNavigateToCategory,
        scrollToTopSignal = scrollToTopSignal
    )
}

@Composable
private fun MobileSeriesRouter(
    viewModel: SeriesViewModel,
    analyticsHelper: AnalyticsHelper,
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit,
    onManageCategoriesClick: () -> Unit,
    onNavigateToCategory: (categoryId: String, categoryName: String) -> Unit,
    scrollToTopSignal: Int
) {
    val seriesUiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val viewModelSearchQuery = seriesUiState.currentSearchQuery
    LaunchedEffect(viewModelSearchQuery) { searchQuery = viewModelSearchQuery ?: "" }

    val currentCategoryFilter = seriesUiState.currentCategoryFilter
    val viewModelSelectedCategory = seriesUiState.selectedCategoryName
    var selectedCategory by remember(viewModelSelectedCategory) { mutableStateOf(viewModelSelectedCategory ?: "All") }

    // TODO: Wire to MobileSeriesLayout once paging is available in common
    Box(
        modifier = Modifier.fillMaxSize().background(getThemeColors().backgroundPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text("Series Screen - ${seriesUiState.categories.size} categories loaded", color = getThemeColors().textColor)
    }
}

data class CategoryItemData(val id: String, val name: String, var count: String = "...")
