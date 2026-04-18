package pt.hitv.feature.series.detail.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.LoadStateNotLoading
import app.cash.paging.compose.LazyPagingItems
import app.cash.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.designsystem.compose.AdvancedCategoryBottomSheet
import pt.hitv.core.designsystem.compose.MovieCardHorizontalSkeleton
import pt.hitv.core.designsystem.compose.SharedEmptyMessage
import pt.hitv.core.designsystem.compose.SharedErrorMessage
import pt.hitv.core.designsystem.compose.SharedMobileBottomSheet
import pt.hitv.core.designsystem.compose.SharedMobileTopAppBar
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.MOVIE_FILTER_CONTINUE_WATCHING
import pt.hitv.core.data.paging.MOVIE_FILTER_FAVORITES
import pt.hitv.core.data.paging.MOVIE_FILTER_RECENTLY_VIEWED
import pt.hitv.core.designsystem.model.createUnifiedSortOptions
import pt.hitv.core.model.Category
import pt.hitv.feature.series.list.SeriesViewModel
import pt.hitv.feature.series.ui.components.SeriesCardHorizontal

@Composable
fun SeriesCategoryDetailScreen(
    initialCategoryId: String,
    initialCategoryName: String,
    viewModel: SeriesViewModel,
    analyticsHelper: AnalyticsHelper,
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit,
    onBackPressed: () -> Unit
) {
    val themeColors = getThemeColors()

    var currentCategoryId by remember { mutableStateOf(initialCategoryId) }
    var currentCategoryName by remember { mutableStateOf(initialCategoryName) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val viewModelSearchQuery = uiState.currentSearchQuery
    var isSearchActive by remember { mutableStateOf(viewModelSearchQuery?.isNotEmpty() == true) }
    var searchQuery by remember { mutableStateOf(viewModelSearchQuery ?: "") }
    val sortOrder = uiState.currentSortOrder
    val isSortAscending = uiState.isSortAscending
    val favorites = uiState.favorites
    val favoriteIds = remember(favorites) { favorites.map { it.seriesId }.toSet() }

    // Save previous filter and restore on exit so the list screen isn't affected
    val previousFilter = remember { uiState.currentCategoryFilter }
    val previousSearchQuery = remember { uiState.currentSearchQuery }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCategoryFilter(previousFilter)
            viewModel.setSearchQuery(previousSearchQuery)
        }
    }

    // Set initial category and fetch data
    LaunchedEffect(Unit) {
        viewModel.setCategoryFilter(currentCategoryId)
        viewModel.getFavorites()
        viewModel.fetchContinueWatchingSeries()
        viewModel.fetchRecentlyViewedTvShows()
        viewModel.fetchLastAddedTvShows()
    }

    // Sync search
    LaunchedEffect(viewModelSearchQuery) {
        viewModelSearchQuery?.let {
            searchQuery = it
            isSearchActive = it.isNotEmpty()
        }
    }

    // Category change
    LaunchedEffect(currentCategoryId) {
        viewModel.setCategoryFilter(currentCategoryId)
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        delay(300)
        if (searchQuery != viewModelSearchQuery) {
            viewModel.setSearchQuery(searchQuery.ifEmpty { null })
        }
    }

    // Analytics
    LaunchedEffect(currentCategoryName) {
        delay(100)
        analyticsHelper.logScreenView(ScreenName.SERIES, "SeriesCategoryDetail_$currentCategoryName")
    }

    // Top bar hide on scroll
    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -5f) isTopBarVisible = false
                else if (delta > 5f) isTopBarVisible = true
                return Offset.Zero
            }
        }
    }

    val series = viewModel.tvShowsPagerFlow.collectAsLazyPagingItems()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
            .nestedScroll(nestedScrollConnection)
    ) {
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SharedMobileTopAppBar(
                selectedCategoryName = currentCategoryName,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChanged = {
                    searchQuery = it
                    isSearchActive = it.isNotEmpty()
                },
                onCategoryClick = { showCategorySheet = true },
                onSearchToggle = { active ->
                    isSearchActive = active
                    if (!active) {
                        searchQuery = ""
                        viewModel.setSearchQuery(null)
                    }
                },
                onBackPressed = onBackPressed,
                showBackButton = true,
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort",
                            tint = getThemeColors().textColor
                        )
                    }
                }
            )
        }

        val isInitialLoading = series.loadState.refresh is LoadStateLoading && series.itemCount == 0
        val isError = series.loadState.refresh is LoadStateError
        val isEmpty = series.itemCount == 0 && series.loadState.refresh is LoadStateNotLoading

        when {
            isError -> {
                SharedErrorMessage(
                    message = "Error loading series",
                    onRetry = { series.retry() }
                )
            }
            isInitialLoading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(18) { MovieCardHorizontalSkeleton() }
                }
            }
            isEmpty -> {
                SharedEmptyMessage(message = "No series to show")
            }
            else -> {
                SeriesCategoryGrid(
                    series = series,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked
                )
            }
        }
    }

    // Sort bottom sheet
    if (showSortSheet) {
        SharedMobileBottomSheet(
            showSheet = showSortSheet,
            onDismiss = { showSortSheet = false }
        ) {
            SortSheetContent(
                sortOrder = sortOrder,
                isSortAscending = isSortAscending,
                onSortSelected = { optionId, supportsDirection ->
                    viewModel.updateSort(optionId)
                    if (!supportsDirection) showSortSheet = false
                }
            )
        }
    }

    // Category bottom sheet
    if (showCategorySheet) {
        val categories = uiState.categories
        val recentSeries = uiState.recentlyViewedTvShows
        val lastAddedSeries = uiState.lastAddedTvShows
        val continueWatchingSeries = uiState.continueWatchingSeries

        SharedMobileBottomSheet(
            showSheet = showCategorySheet,
            onDismiss = { showCategorySheet = false }
        ) {
            var categorySearchQuery by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            val allCount = remember { mutableStateOf("...") }
            val categoryCounts = remember { mutableStateMapOf<String, String>() }

            LaunchedEffect(Unit) {
                scope.launch {
                    try { allCount.value = viewModel.getTotalSeriesCount().toString() }
                    catch (_: Exception) { allCount.value = "0" }
                }
                categories.forEach { category ->
                    scope.launch {
                        try {
                            val count = viewModel.getCategorySeriesCount(category.categoryId.toString())
                            categoryCounts[category.categoryId.toString()] = count.toString()
                        } catch (_: Exception) {
                            categoryCounts[category.categoryId.toString()] = "0"
                        }
                    }
                }
            }

            AdvancedCategoryBottomSheet(
                categories = categories,
                selectedCategory = currentCategoryName,
                categorySearchQuery = categorySearchQuery,
                onCategorySearchQueryChanged = { categorySearchQuery = it },
                onCategorySelected = { selectedName ->
                    val filterKey = getFilterKeyForTitle(selectedName, categories)
                    if (filterKey != null) {
                        currentCategoryId = filterKey
                        currentCategoryName = selectedName
                        analyticsHelper.logCategorySelected(selectedName)
                    }
                    showCategorySheet = false
                    categorySearchQuery = ""
                },
                onDismiss = {
                    showCategorySheet = false
                    categorySearchQuery = ""
                },
                allCount = allCount.value,
                favoritesCount = favorites.size.toString(),
                recentCount = recentSeries.size.toString(),
                lastAddedCount = lastAddedSeries.size.toString(),
                continueWatchingCount = continueWatchingSeries.size.toString(),
                categoryCounts = categoryCounts.toMap(),
                contentType = "series",
                onManageCategories = null
            )
        }
    }
}

@Composable
private fun SeriesCategoryGrid(
    series: LazyPagingItems<TvShow>,
    favoriteIds: Set<Int>,
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = rememberLazyGridState(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = series.itemCount,
            key = { index -> series.peek(index)?.seriesId ?: "index_$index" }
        ) { index ->
            val tvShow = series[index]
            if (tvShow != null) {
                val seriesWithFavorite = tvShow.copy(isFavorite = favoriteIds.contains(tvShow.seriesId))
                SeriesCardHorizontal(
                    series = seriesWithFavorite,
                    onSeriesClicked = { clickType -> onSeriesClicked(tvShow, index, clickType) },
                    showTopBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (series.loadState.append is LoadStateError) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "append_error") {
                SharedErrorMessage(
                    message = "Error loading more series",
                    onRetry = { series.retry() }
                )
            }
        }
    }
}

@Composable
private fun SortSheetContent(
    sortOrder: String,
    isSortAscending: Boolean,
    onSortSelected: (optionId: String, supportsDirection: Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = getThemeColors().textColor
        )
        createUnifiedSortOptions().forEach { option ->
            val isSelected = sortOrder == option.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSortSelected(option.id, option.supportsDirection) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else getThemeColors().textColor
                )
                if (isSelected) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (option.supportsDirection) {
                            Icon(
                                imageVector = if (isSortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = if (isSortAscending) "Ascending" else "Descending",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp).size(20.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private fun getFilterKeyForTitle(
    title: String?,
    categories: List<Category>
): String? {
    return when (title) {
        "Favorites" -> MOVIE_FILTER_FAVORITES
        "Recently Viewed" -> MOVIE_FILTER_RECENTLY_VIEWED
        "Continue Watching" -> MOVIE_FILTER_CONTINUE_WATCHING
        "All" -> MOVIE_FILTER_ALL
        else -> categories.find { it.categoryName == title }?.categoryId?.toString()
    }
}
