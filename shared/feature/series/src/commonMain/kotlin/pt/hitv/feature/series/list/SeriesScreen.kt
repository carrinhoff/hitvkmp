package pt.hitv.feature.series.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pt.hitv.core.model.TvShow
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.data.paging.FILTER_ALL
import pt.hitv.core.data.paging.FILTER_CONTINUE_WATCHING
import pt.hitv.core.data.paging.FILTER_FAVORITES
import pt.hitv.core.data.paging.FILTER_LAST_ADDED
import pt.hitv.core.data.paging.FILTER_RECENTLY_VIEWED
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.MOVIE_FILTER_FAVORITES
import pt.hitv.core.data.paging.MOVIE_FILTER_RECENTLY_VIEWED
import pt.hitv.core.designsystem.compose.AdvancedCategoryBottomSheetContent
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.compose.CategorySectionSkeleton
import pt.hitv.feature.series.ui.components.CategorySeriesSection

/**
 * Main router screen for Series feature (commonMain).
 * Displays: TopAppBar with category selector + search,
 * then Continue Watching, Favorites, Recently Viewed, Last Added, then category rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    val seriesUiState by viewModel.uiState.collectAsState()
    val themeColors = getThemeColors()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Scroll spy: track visible category name based on scroll position
    var scrollSpyCategoryName by remember { mutableStateOf("All") }

    // The displayed category name: driven by scroll spy when in "All" view
    val isHomeFeed = seriesUiState.currentCategoryFilter == MOVIE_FILTER_ALL

    val selectedCategoryName = if (isHomeFeed) {
        scrollSpyCategoryName
    } else {
        remember(seriesUiState.currentCategoryFilter, seriesUiState.categories) {
            when (seriesUiState.currentCategoryFilter) {
                MOVIE_FILTER_ALL -> "All"
                MOVIE_FILTER_FAVORITES -> "Favorites"
                MOVIE_FILTER_RECENTLY_VIEWED -> "Recently Viewed"
                else -> seriesUiState.categories.find {
                    it.categoryId.toString() == seriesUiState.currentCategoryFilter
                }?.categoryName ?: "All"
            }
        }
    }

    // Track scroll-to-category request from bottom sheet
    var scrollToCategoryId by remember { mutableStateOf<String?>(null) }

    // Force full refresh on every composition — covers first-time composition
    // right after sync completes, screen recreation via syncVersion, and
    // back-navigation. refreshAfterSync also re-runs the category load.
    LaunchedEffect(Unit) {
        viewModel.refreshAfterSync()
        viewModel.fetchLastAddedTvShows()
        viewModel.fetchContinueWatchingSeries()
    }

    // Drive search results — see MoviesScreen for the same wiring. Without
    // this call, typing in the search box updates currentSearchQuery but the
    // searchResultSeries map stays empty and the UI shows nothing.
    LaunchedEffect(searchQuery, seriesUiState.categories) {
        viewModel.updateSearchQueryWithCategories(searchQuery, seriesUiState.categories)
    }

    // Analytics
    LaunchedEffect(Unit) {
        delay(100)
        analyticsHelper.logScreenView(ScreenName.SERIES, "SeriesScreen")
    }

    // Scroll to top on signal
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Track favorites as a set for efficient lookups (seriesId is Int)
    val favoriteIds = remember(seriesUiState.favorites) {
        seriesUiState.favorites.map { it.seriesId }.toSet()
    }

    // === Scroll spy: detect visible category from scroll position ===
    val categories = seriesUiState.categories
    val hasContinueWatching = seriesUiState.continueWatchingSeries.isNotEmpty()
    val hasFavorites = seriesUiState.favorites.isNotEmpty()
    val hasRecentlyViewed = seriesUiState.recentlyViewedTvShows.isNotEmpty()
    val hasLastAdded = seriesUiState.lastAddedTvShows.isNotEmpty()

    LaunchedEffect(categories.size, hasContinueWatching, hasFavorites, hasRecentlyViewed, hasLastAdded) {
        if (!isHomeFeed) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { visibleIndex ->
                var currentIndex = 0
                var detectedCategory = "All"

                // "All" section is always at index 0
                if (visibleIndex == currentIndex) { scrollSpyCategoryName = "All"; return@collect }
                currentIndex++

                // Continue Watching
                if (hasContinueWatching) {
                    if (visibleIndex == currentIndex) { scrollSpyCategoryName = "Continue Watching"; return@collect }
                    currentIndex++
                }
                // Favorites
                if (hasFavorites) {
                    if (visibleIndex == currentIndex) { scrollSpyCategoryName = "Favorites"; return@collect }
                    currentIndex++
                }
                // Recently Viewed
                if (hasRecentlyViewed) {
                    if (visibleIndex == currentIndex) { scrollSpyCategoryName = "Recently Viewed"; return@collect }
                    currentIndex++
                }
                // Last Added
                if (hasLastAdded) {
                    if (visibleIndex == currentIndex) { scrollSpyCategoryName = "Last Added"; return@collect }
                    currentIndex++
                }
                // Category sections
                val categoryIndex = visibleIndex - currentIndex
                if (categoryIndex >= 0 && categoryIndex < categories.size) {
                    detectedCategory = categories[categoryIndex].categoryName
                }
                scrollSpyCategoryName = detectedCategory
            }
    }

    // === Scroll to category when user picks from bottom sheet ===
    LaunchedEffect(scrollToCategoryId) {
        val targetId = scrollToCategoryId ?: return@LaunchedEffect
        var targetIndex = 0

        if (targetId == MOVIE_FILTER_ALL) {
            targetIndex = 0
        } else {
            // Start after the "All" section which is always at index 0
            targetIndex = 1
            if (targetId == MOVIE_FILTER_FAVORITES && hasFavorites) {
                if (hasContinueWatching) targetIndex++
            } else if (targetId == MOVIE_FILTER_RECENTLY_VIEWED && hasRecentlyViewed) {
                if (hasContinueWatching) targetIndex++
                if (hasFavorites) targetIndex++
            } else {
                // It's a real category ID — compute offset
                if (hasContinueWatching) targetIndex++
                if (hasFavorites) targetIndex++
                if (hasRecentlyViewed) targetIndex++
                if (hasLastAdded) targetIndex++
                val catIdx = categories.indexOfFirst { it.categoryId.toString() == targetId }
                if (catIdx >= 0) targetIndex += catIdx
            }
        }

        listState.animateScrollToItem(targetIndex)
        scrollToCategoryId = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
    ) {
        // === Top App Bar — Category selector + search ===
        TopAppBar(
            title = {
                Box(modifier = Modifier.padding(start = 4.dp)) {
                    if (isSearchActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = themeColors.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { query ->
                                            searchQuery = query
                                            viewModel.setSearchQuery(query.takeIf { it.isNotBlank() })
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = themeColors.textColor
                                        ),
                                        cursorBrush = SolidColor(themeColors.primaryColor),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    "Search series...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = themeColors.textColor.copy(alpha = 0.7f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                searchQuery = ""
                                                viewModel.setSearchQuery(null)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = themeColors.textColor.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Category selector card
                        Card(
                            onClick = { showCategorySheet = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = "Category",
                                    modifier = Modifier.size(18.dp),
                                    tint = themeColors.primaryColor
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCategoryName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    color = themeColors.textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select category",
                                    modifier = Modifier.size(20.dp),
                                    tint = themeColors.primaryColor
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                if (!isSearchActive) {
                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = themeColors.textColor)
                    }
                } else {
                    IconButton(onClick = {
                        isSearchActive = false
                        searchQuery = ""
                        viewModel.setSearchQuery(null)
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close search", tint = themeColors.textColor)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = themeColors.backgroundPrimary,
                titleContentColor = themeColors.textColor,
                actionIconContentColor = themeColors.textColor
            )
        )

        // Show shimmer skeleton when loading
        if (seriesUiState.categories.isEmpty() && seriesUiState.isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(4) { index ->
                    CategorySectionSkeleton(
                        title = when (index) {
                            0 -> "Favorites"
                            1 -> "Recently Viewed"
                            2 -> "Last Added"
                            else -> "Category"
                        }
                    )
                }
            }
            return@Column
        }

    val searchActive = searchQuery.isNotBlank()
    val searchMatchedCategories = seriesUiState.searchMatchedCategories
    val searchResultSeries = seriesUiState.searchResultSeries

    // Main content - home feed layout
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        if (searchActive) {
            if (searchMatchedCategories.isEmpty()) {
                item(key = "__search_empty") {
                    androidx.compose.material3.Text(
                        text = "No series found for \"$searchQuery\"",
                        color = themeColors.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                items(searchMatchedCategories.size, key = { "search_${searchMatchedCategories[it].categoryId}" }) { index ->
                    val category = searchMatchedCategories[index]
                    val series = searchResultSeries[category.categoryId.toString()].orEmpty()
                    if (series.isNotEmpty()) {
                        CategorySeriesSection(
                            categoryTitle = category.categoryName,
                            series = series,
                            favoriteIds = favoriteIds,
                            onSeriesClicked = onSeriesClicked,
                            onViewAllClicked = { onNavigateToCategory(category.categoryId.toString(), category.categoryName) }
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // "All" section — first item, matches original
        item(key = "all_section") {
            val allFilterId = "All"
            val allSeries = seriesUiState.categorySeriesMap[allFilterId]

            LaunchedEffect(Unit) {
                if (allSeries == null) {
                    viewModel.getSeriesByCategory(allFilterId)
                }
            }

            if (!allSeries.isNullOrEmpty()) {
                CategorySeriesSection(
                    categoryTitle = "All",
                    series = allSeries,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = { onNavigateToCategory(FILTER_ALL, "All") }
                )
            }
        }

        // Continue Watching section
        if (seriesUiState.continueWatchingSeries.isNotEmpty()) {
            item(key = "continue_watching") {
                CategorySeriesSection(
                    categoryTitle = "Continue Watching",
                    series = seriesUiState.continueWatchingSeries,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = { onNavigateToCategory(FILTER_CONTINUE_WATCHING, "Continue Watching") }
                )
            }
        }

        // Favorites section
        if (seriesUiState.favorites.isNotEmpty()) {
            item(key = "favorites") {
                CategorySeriesSection(
                    categoryTitle = "Favorites",
                    series = seriesUiState.favorites,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = { onNavigateToCategory(FILTER_FAVORITES, "Favorites") }
                )
            }
        }

        // Recently Viewed section
        if (seriesUiState.recentlyViewedTvShows.isNotEmpty()) {
            item(key = "recently_viewed") {
                CategorySeriesSection(
                    categoryTitle = "Recently Viewed",
                    series = seriesUiState.recentlyViewedTvShows,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = { onNavigateToCategory(FILTER_RECENTLY_VIEWED, "Recently Viewed") }
                )
            }
        }

        // Last Added section
        if (seriesUiState.lastAddedTvShows.isNotEmpty()) {
            item(key = "last_added") {
                CategorySeriesSection(
                    categoryTitle = "Last Added",
                    series = seriesUiState.lastAddedTvShows,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = { onNavigateToCategory(FILTER_LAST_ADDED, "Last Added") },
                    showTopBadge = true
                )
            }
        }

        // Category sections
        items(categories.size, key = { "cat_${categories[it].categoryId}" }) { index ->
            val category = categories[index]
            val categoryIdStr = category.categoryId.toString()
            val categorySeries = seriesUiState.categorySeriesMap[categoryIdStr]

            // Fetch series for this category if not loaded
            LaunchedEffect(categoryIdStr) {
                if (categorySeries == null) {
                    viewModel.getSeriesByCategory(categoryIdStr)
                }
            }

            if (categorySeries == null) {
                CategorySectionSkeleton(title = category.categoryName)
            } else if (categorySeries.isNotEmpty()) {
                CategorySeriesSection(
                    categoryTitle = category.categoryName,
                    series = categorySeries,
                    favoriteIds = favoriteIds,
                    onSeriesClicked = onSeriesClicked,
                    onViewAllClicked = {
                        onNavigateToCategory(categoryIdStr, category.categoryName)
                    }
                )
            }
        }
    }
    } // end Column

    // === Category Bottom Sheet ===
    if (showCategorySheet) {
        var categorySearchQuery by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            viewModel.getTotalSeriesCount()
            seriesUiState.categories.forEach { cat ->
                viewModel.getCategorySeriesCount(cat.categoryId.toString())
            }
        }

        ModalBottomSheet(
            onDismissRequest = {
                categorySearchQuery = ""
                showCategorySheet = false
            },
            containerColor = themeColors.backgroundSecondary,
            contentColor = themeColors.textColor
        ) {
            AdvancedCategoryBottomSheetContent(
                categories = seriesUiState.categories,
                selectedCategoryFilter = seriesUiState.currentCategoryFilter ?: MOVIE_FILTER_ALL,
                categorySearchQuery = categorySearchQuery,
                onCategorySearchQueryChanged = { categorySearchQuery = it },
                onCategorySelected = { filterId ->
                    // In "All" mode, scroll to the section instead of switching filter
                    if (filterId == MOVIE_FILTER_ALL || isHomeFeed) {
                        if (filterId != MOVIE_FILTER_ALL) {
                            scrollToCategoryId = filterId
                        } else {
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        }
                        // Keep filter on "All" so we stay in home feed view
                        viewModel.setCategoryFilter(MOVIE_FILTER_ALL)
                    } else {
                        viewModel.setCategoryFilter(filterId)
                    }
                    categorySearchQuery = ""
                    showCategorySheet = false
                },
                onDismiss = {
                    categorySearchQuery = ""
                    showCategorySheet = false
                },
                allFilterId = MOVIE_FILTER_ALL,
                favoritesFilterId = MOVIE_FILTER_FAVORITES,
                recentlyViewedFilterId = MOVIE_FILTER_RECENTLY_VIEWED,
                allCount = seriesUiState.categoryCounts[MOVIE_FILTER_ALL]?.toString() ?: "...",
                favoritesCount = seriesUiState.favorites.size.toString(),
                recentCount = seriesUiState.recentlyViewedTvShows.size.toString(),
                categoryCounts = seriesUiState.categoryCounts.mapValues { it.value.toString() }
            )
        }
    }
}
