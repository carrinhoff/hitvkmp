package pt.hitv.feature.movies.detail.category

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.paging.LoadStateError
import app.cash.paging.LoadStateLoading
import app.cash.paging.LoadStateNotLoading
import app.cash.paging.compose.LazyPagingItems
import app.cash.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.MOVIE_FILTER_CONTINUE_WATCHING
import pt.hitv.core.data.paging.MOVIE_FILTER_FAVORITES
import pt.hitv.core.data.paging.MOVIE_FILTER_RECENTLY_VIEWED
import pt.hitv.core.designsystem.compose.AdvancedCategoryBottomSheet
import pt.hitv.core.designsystem.compose.SharedEmptyMessage
import pt.hitv.core.designsystem.compose.SharedErrorMessage
import pt.hitv.core.designsystem.compose.SharedMobileBottomSheet
import pt.hitv.core.designsystem.compose.SharedMobileTopAppBar
import pt.hitv.core.designsystem.model.createUnifiedSortOptions
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Category
import pt.hitv.core.model.Movie
import pt.hitv.core.model.enums.ClickType
import pt.hitv.feature.movies.list.MovieViewModel
import pt.hitv.feature.movies.ui.components.MovieCardHorizontal

@Composable
fun MovieCategoryDetailScreen(
    initialCategoryId: String,
    initialCategoryName: String,
    viewModel: MovieViewModel,
    analyticsHelper: AnalyticsHelper,
    onMovieClicked: (Movie, Int, ClickType) -> Unit,
    onBackPressed: () -> Unit
) {
    val themeColors = getThemeColors()

    var currentCategoryId by remember { mutableStateOf(initialCategoryId) }
    var currentCategoryName by remember { mutableStateOf(initialCategoryName) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val viewModelSearchQuery = uiState.currentSearchQuery
    var isSearchActive by remember { mutableStateOf(viewModelSearchQuery?.isNotEmpty() == true) }
    var searchQuery by remember { mutableStateOf(viewModelSearchQuery ?: "") }
    val sortOrder = uiState.currentSortOrder
    val isSortAscending = uiState.isSortAscending
    val favorites = uiState.favorites
    val favoriteIds = remember(favorites) { favorites.map { it.streamId }.toSet() }

    // Save previous filter and restore on exit so the list screen isn't affected
    val previousFilter = remember { uiState.currentCategoryFilter }
    val previousSearchQuery = remember { uiState.currentSearchQuery }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setCategoryFilter(previousFilter)
            viewModel.setSearchQuery(previousSearchQuery)
        }
    }

    // Set initial category
    LaunchedEffect(Unit) {
        viewModel.setCategoryFilter(currentCategoryId)
        viewModel.getFavorites()
        viewModel.fetchContinueWatchingMovies()
        viewModel.fetchRecentlyViewedMovies()
        viewModel.fetchLastAddedMovies()
    }

    // Sync search with ViewModel
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
        analyticsHelper.logScreenView(ScreenName.MOVIES, "MovieCategoryDetail_$currentCategoryName")
    }

    // Top bar hide on landscape scroll
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

    val movies = viewModel.moviesPagerFlow.collectAsLazyPagingItems()
    val listState = rememberLazyGridState()

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
                    viewModel.setCategorySearchActive(active)
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

        val isInitialLoading = movies.loadState.refresh is LoadStateLoading && movies.itemCount == 0
        val isError = movies.loadState.refresh is LoadStateError
        val isEmpty = movies.itemCount == 0 && movies.loadState.refresh is LoadStateNotLoading

        when {
            isError -> {
                SharedErrorMessage(
                    message = "Error loading movies",
                    onRetry = { movies.retry() }
                )
            }
            isInitialLoading -> {
                // Skeleton grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(18) {
                        pt.hitv.core.designsystem.compose.MovieCardHorizontalSkeleton()
                    }
                }
            }
            isEmpty -> {
                SharedEmptyMessage(message = "No movies to show")
            }
            else -> {
                MovieCategoryGrid(
                    movies = movies,
                    favoriteIds = favoriteIds,
                    onMovieClicked = onMovieClicked
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
        val recentMovies = uiState.recentlyViewedMovies
        val lastAddedMovies = uiState.lastAddedMovies
        val continueWatchingMovies = uiState.continueWatchingMovies

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
                    try { allCount.value = viewModel.getTotalMovieCount().toString() }
                    catch (_: Exception) { allCount.value = "0" }
                }
                categories.forEach { category ->
                    scope.launch {
                        try {
                            val count = viewModel.getCategoryMovieCount(category.categoryId.toString())
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
                recentCount = recentMovies.size.toString(),
                lastAddedCount = lastAddedMovies.size.toString(),
                continueWatchingCount = continueWatchingMovies.size.toString(),
                categoryCounts = categoryCounts.toMap(),
                contentType = "movies",
                onManageCategories = null
            )
        }
    }
}

@Composable
private fun MovieCategoryGrid(
    movies: LazyPagingItems<Movie>,
    favoriteIds: Set<String?>,
    onMovieClicked: (Movie, Int, ClickType) -> Unit
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
            count = movies.itemCount,
            key = { index -> movies.peek(index)?.streamId ?: "placeholder_$index" }
        ) { index ->
            val movie = movies[index]
            if (movie != null) {
                val movieWithFavorite = movie.copy(isFavorite = favoriteIds.contains(movie.streamId))
                MovieCardHorizontal(
                    movie = movieWithFavorite,
                    onMovieClicked = { clickType -> onMovieClicked(movie, index, clickType) },
                    showTopBadge = false,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (movies.loadState.append is LoadStateError) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "append_error") {
                SharedErrorMessage(
                    message = "Error loading more movies",
                    onRetry = { movies.retry() }
                )
            }
        }
    }
}

@Composable
internal fun SortSheetContent(
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
            SortOptionItem(
                title = option.label,
                isSelected = isSelected,
                isAscending = isSortAscending,
                supportsDirection = option.supportsDirection,
                onClick = { onSortSelected(option.id, option.supportsDirection) }
            )
        }
    }
}

@Composable
private fun SortOptionItem(
    title: String,
    isSelected: Boolean,
    isAscending: Boolean,
    supportsDirection: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else getThemeColors().textColor
        )
        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (supportsDirection) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (isAscending) "Ascending" else "Descending",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp)
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

internal fun getFilterKeyForTitle(
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
