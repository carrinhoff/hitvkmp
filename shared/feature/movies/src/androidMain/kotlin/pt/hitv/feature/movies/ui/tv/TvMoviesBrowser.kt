package pt.hitv.feature.movies.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paging.compose.LazyPagingItems
import app.cash.paging.compose.itemKey
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Border
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.hitv.core.model.Movie
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.MOVIE_FILTER_FAVORITES
import pt.hitv.core.data.paging.MOVIE_FILTER_LAST_ADDED
import pt.hitv.core.data.paging.MOVIE_FILTER_RECENTLY_VIEWED
import pt.hitv.feature.movies.list.MovieViewModel
import pt.hitv.core.ui.components.TvMaterialMovieCard
import pt.hitv.core.designsystem.model.SortOption
import pt.hitv.core.designsystem.model.createUnifiedSortOptions
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

private const val GRID_COLS = 4
private const val SIDEBAR_WIDTH_EXPANDED = 280
private const val SIDEBAR_WIDTH_COLLAPSED = 80
private const val FILTER_SEARCH = "SEARCH"

data class SidebarCategory(
    val id: String,
    val name: String,
    val icon: ImageVector? = null,
    val count: Int = 0
)

/**
 * Android TV-specific Movies Browser with sidebar navigation.
 * Uses AndroidX TV Foundation/Material3 components.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMoviesBrowser(
    movies: LazyPagingItems<Movie>,
    viewModel: MovieViewModel,
    analyticsHelper: AnalyticsHelper,
    onMovieClicked: (Movie, Int, ClickType) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val movieUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories = movieUiState.categories
    val favorites = movieUiState.favorites
    val recentMovies = movieUiState.recentlyViewedMovies
    val lastAddedMovies = movieUiState.lastAddedMovies
    val currentCategoryFilter = movieUiState.currentCategoryFilter
    val isSidebarFocused = movieUiState.isSidebarFocused
    val categoryCounts = movieUiState.categoryCounts
    val currentSortOrder = movieUiState.currentSortOrder
    val isSortAscending = movieUiState.isSortAscending

    val sidebarListFocusRequester = remember { FocusRequester() }
    val gridFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    val sortButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(categories, favorites.size, recentMovies.size, lastAddedMovies.size) {
        viewModel.getTotalMovieCount()
        categories.forEach { category -> viewModel.getCategoryMovieCount(category.categoryId.toString()) }
    }

    val sidebarCategories = remember(categories, favorites, recentMovies, lastAddedMovies, categoryCounts) {
        buildList {
            add(SidebarCategory(FILTER_SEARCH, "Search", Icons.Default.Search, 0))
            add(SidebarCategory(MOVIE_FILTER_ALL, "All", Icons.Default.Movie, categoryCounts[MOVIE_FILTER_ALL] ?: 0))
            add(SidebarCategory(MOVIE_FILTER_FAVORITES, "Favorites", Icons.Default.Favorite, favorites.size))
            add(SidebarCategory(MOVIE_FILTER_RECENTLY_VIEWED, "Recently Viewed", Icons.Default.History, recentMovies.size))
            add(SidebarCategory(MOVIE_FILTER_LAST_ADDED, "Last Added", Icons.Default.NewReleases, lastAddedMovies.size))
            categories.forEach { add(SidebarCategory(it.categoryId.toString(), it.categoryName, count = categoryCounts[it.categoryId.toString()] ?: 0)) }
        }
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategoryName = remember(selectedCategoryIndex, sidebarCategories) {
        sidebarCategories.getOrNull(selectedCategoryIndex)?.name ?: ""
    }

    LaunchedEffect(currentCategoryFilter) {
        val index = sidebarCategories.indexOfFirst { it.id == currentCategoryFilter }
        if (index >= 0) selectedCategoryIndex = index
        if (currentCategoryFilter == FILTER_SEARCH) {
            delay(100); try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
        }
    }

    val gridState = rememberTvLazyGridState()

    LaunchedEffect(movies.loadState.refresh) {
        if (movies.loadState.refresh is app.cash.paging.LoadStateLoading && gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0)
    }

    val activeFocusIndex = movieUiState.focusState.activeFocusIndex

    LaunchedEffect(activeFocusIndex) {
        if (activeFocusIndex >= 0) {
            delay(150)
            try { if (activeFocusIndex < movies.itemCount) gridState.scrollToItem(activeFocusIndex); delay(50); viewModel.setSidebarFocused(false) } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) { viewModel.setSidebarFocused(true); delay(100); sidebarListFocusRequester.requestFocus() }

    val sidebarWidth = if (isSidebarFocused) SIDEBAR_WIDTH_EXPANDED.dp else SIDEBAR_WIDTH_COLLAPSED.dp

    Box(modifier = modifier.fillMaxSize().background(getThemeColors().backgroundPrimary)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            TvSidebar(
                categories = sidebarCategories, selectedIndex = selectedCategoryIndex, isExpanded = isSidebarFocused, currentWidth = sidebarWidth,
                listFocusRequester = sidebarListFocusRequester,
                onCategorySelected = { index, category -> selectedCategoryIndex = index; viewModel.setCategoryFilter(category.id) },
                onFocusChanged = { viewModel.setSidebarFocused(it) },
                onNavigateRight = { if (currentCategoryFilter == FILTER_SEARCH) try { searchFocusRequester.requestFocus() } catch (_: Exception) {} else try { gridFocusRequester.requestFocus() } catch (_: Exception) {} },
                modifier = Modifier.fillMaxHeight().padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            )

            // Main content
            Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                val isSearchActive = currentCategoryFilter == FILTER_SEARCH

                if (isSearchActive) {
                    val tvSearchQuery = movieUiState.tvSearchQuery
                    var searchText by remember { mutableStateOf(tvSearchQuery) }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    LaunchedEffect(searchText) { delay(500); if (searchText != tvSearchQuery) viewModel.setTvSearchQuery(searchText) }
                    TvModernSearchBar(value = searchText, onValueChange = { searchText = it }, onSearch = { viewModel.setTvSearchQuery(searchText); keyboardController?.hide() }, focusRequester = searchFocusRequester, onNavigateLeft = { sidebarListFocusRequester.requestFocus() }, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 20.dp))
                } else {
                    TvFilterHeader(title = currentCategoryName, currentSort = currentSortOrder, isAscending = isSortAscending, onSortSelected = { viewModel.updateSort(it) }, focusRequester = sortButtonFocusRequester, onNavigateLeft = { sidebarListFocusRequester.requestFocus() }, onNavigateDown = { gridFocusRequester.requestFocus() }, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 16.dp))
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (movies.loadState.refresh is app.cash.paging.LoadStateLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = getThemeColors().primaryColor) }
                    } else if (movies.itemCount == 0 && movies.loadState.refresh is app.cash.paging.LoadStateNotLoading) {
                        TvEmptyState(message = "No movies to show")
                    } else {
                        TvMovieGrid(movies = movies, viewModel = viewModel, gridState = gridState, gridFocusRequester = gridFocusRequester, columns = GRID_COLS, activeFocusIndex = activeFocusIndex,
                            onMovieClicked = { movie, index, clickType -> viewModel.savePendingFocus(index, currentCategoryFilter, movie.streamId); onMovieClicked(movie, index, clickType) },
                            onMovieFocused = { if (isSidebarFocused) viewModel.setSidebarFocused(false) },
                            onNavigateToSidebar = { coroutineScope.launch { sidebarListFocusRequester.requestFocus() } },
                            onNavigateUp = { if (!isSearchActive) try { sortButtonFocusRequester.requestFocus() } catch (_: Exception) {} },
                            modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun TvFilterHeader(title: String, currentSort: String, isAscending: Boolean, onSortSelected: (String) -> Unit, focusRequester: FocusRequester, onNavigateLeft: () -> Unit, onNavigateDown: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = getThemeColors()
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        androidx.tv.material3.Text(text = title, style = androidx.tv.material3.MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Box {
            TvFilterChip(label = getSortLabel(currentSort, isAscending), icon = Icons.Default.Sort, onClick = { isSortMenuExpanded = true }, focusRequester = focusRequester, onNavigateLeft = onNavigateLeft, onNavigateDown = onNavigateDown)
            val sortOptions = createUnifiedSortOptions()
            androidx.compose.material3.DropdownMenu(expanded = isSortMenuExpanded, onDismissRequest = { isSortMenuExpanded = false }, modifier = Modifier.background(Color.Black.copy(0.9f))) {
                sortOptions.forEach { option ->
                    val isSelected = option.id == currentSort
                    androidx.compose.material3.DropdownMenuItem(text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Text(option.label, color = if (isSelected) themeColors.primaryColor else Color.White, modifier = Modifier.weight(1f))
                            if (isSelected && option.supportsDirection) androidx.compose.material3.Icon(if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = themeColors.primaryColor, modifier = Modifier.size(16.dp))
                        }
                    }, onClick = { onSortSelected(option.id); if (!option.supportsDirection) isSortMenuExpanded = false })
                }
            }
        }
    }
}

@Composable
fun getSortLabel(sort: String, isAscending: Boolean): String {
    val sortOptions = createUnifiedSortOptions()
    val option = sortOptions.find { it.id == sort } ?: return "Sort by"
    val label = option.label
    return if (option.supportsDirection) { val direction = if (isAscending) "Ascending" else "Descending"; "$label ($direction)" } else label
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFilterChip(label: String, icon: ImageVector, onClick: () -> Unit, focusRequester: FocusRequester, onNavigateLeft: () -> Unit, onNavigateDown: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = getThemeColors()
    var isFocused by remember { mutableStateOf(false) }
    val keyMod = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown) when (e.key) { Key.DirectionLeft -> { onNavigateLeft(); true }; Key.DirectionDown -> { onNavigateDown(); true }; else -> false } else false }
    Surface(onClick = onClick, modifier = modifier.focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused }.then(keyMod), shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        border = ClickableSurfaceDefaults.border(border = Border(BorderStroke(1.dp, Color.White.copy(0.1f))), focusedBorder = Border(BorderStroke(2.dp, themeColors.primaryColor))),
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(0.05f), focusedContainerColor = Color.White.copy(0.2f), contentColor = Color.White.copy(0.7f), focusedContentColor = Color.White)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.tv.material3.Icon(icon, null, modifier = Modifier.size(18.dp)); androidx.tv.material3.Text(label, style = androidx.tv.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun TvModernSearchBar(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, focusRequester: FocusRequester, onNavigateLeft: () -> Unit, modifier: Modifier = Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val themeColors = getThemeColors()
    val bgColor by animateColorAsState(if (isFocused) Color.White.copy(0.2f) else Color.Black.copy(0.3f), tween(300), label = "bg")
    val borderColor by animateColorAsState(if (isFocused) themeColors.primaryColor else Color.Transparent, tween(300), label = "border")
    val keyMod = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) { onNavigateLeft(); true } else false }
    BasicTextField(value = value, onValueChange = onValueChange, modifier = modifier.height(56.dp).focusRequester(focusRequester).onFocusChanged { isFocused = it.isFocused }.then(keyMod), textStyle = TextStyle(Color.White, 18.sp, FontWeight.Medium), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onSearch() }), cursorBrush = SolidColor(themeColors.primaryColor), singleLine = true,
        decorationBox = { innerTextField -> Box(Modifier.fillMaxSize().background(bgColor, RoundedCornerShape(50)).border(if (isFocused) 2.dp else 0.dp, borderColor, RoundedCornerShape(50)).padding(horizontal = 20.dp), contentAlignment = Alignment.CenterStart) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { androidx.compose.material3.Icon(Icons.Default.Search, "Search", tint = if (isFocused) themeColors.primaryColor else Color.White.copy(0.6f), modifier = Modifier.size(24.dp)); Box(Modifier.weight(1f)) { if (value.isEmpty()) androidx.compose.material3.Text("Search", color = Color.White.copy(0.4f), fontSize = 18.sp); innerTextField() } } } })
}

@Composable
fun TvEmptyState(message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        androidx.tv.material3.Icon(Icons.Default.Search, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp)); Spacer(Modifier.height(16.dp)); androidx.tv.material3.Text(message, color = Color.White.copy(0.5f), fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvSidebar(categories: List<SidebarCategory>, selectedIndex: Int, isExpanded: Boolean, currentWidth: androidx.compose.ui.unit.Dp, listFocusRequester: FocusRequester, onCategorySelected: (Int, SidebarCategory) -> Unit, onFocusChanged: (Boolean) -> Unit, onNavigateRight: () -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberTvLazyListState()
    LaunchedEffect(selectedIndex) { if (selectedIndex in categories.indices) listState.animateScrollToItem(selectedIndex) }
    Surface(modifier = modifier.width(currentWidth).onFocusChanged { onFocusChanged(it.hasFocus) }, shape = RoundedCornerShape(12.dp), colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(0.7f))) {
        Column(Modifier.fillMaxSize()) {
            if (isExpanded) androidx.tv.material3.Text("Movies", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, start = 20.dp, bottom = 4.dp))
            TvLazyColumn(state = listState, modifier = Modifier.fillMaxSize().focusRequester(listFocusRequester), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                itemsIndexed(items = categories, key = { _, c -> c.id }) { index, category ->
                    val navMod = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { onNavigateRight(); true } else false }
                    SidebarCategoryItem(category, index == selectedIndex, isExpanded, { onCategorySelected(index, category) }, navMod)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SidebarCategoryItem(category: SidebarCategory, isSelected: Boolean, isExpanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val themeColors = getThemeColors(); var isFocused by remember { mutableStateOf(false) }
    val bg = when { isFocused -> themeColors.primaryColor.copy(0.3f); isSelected -> themeColors.primaryColor.copy(0.15f); else -> Color.Transparent }
    val iconTint = if (isFocused || isSelected) themeColors.primaryColor else Color.White.copy(0.7f)
    Surface(onClick = onClick, modifier = modifier.fillMaxWidth().height(44.dp).onFocusChanged { isFocused = it.isFocused }, shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)), colors = ClickableSurfaceDefaults.colors(containerColor = bg, focusedContainerColor = bg)) {
        Row(Modifier.fillMaxSize().padding(horizontal = if (isExpanded) 12.dp else 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center) {
            if (category.icon != null) androidx.tv.material3.Icon(category.icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) else CategoryAvatar(category.name, isSelected || isFocused, Modifier.size(20.dp))
            if (isExpanded) { Spacer(Modifier.width(10.dp)); androidx.tv.material3.Text(category.name, color = Color.White, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); if (category.count > 0) androidx.tv.material3.Text(category.count.toString(), color = Color.Gray, fontSize = 12.sp) }
        }
    }
}

@Composable
fun CategoryAvatar(name: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    val initials = remember(name) { getCategoryInitials(name) }; val baseColor = remember(name) { getCategoryColor(name) }; val themeColors = getThemeColors()
    Box(modifier.background(if (isSelected) themeColors.primaryColor else baseColor, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) { androidx.compose.material3.Text(initials, color = if (isSelected) Color.White else Color.Black.copy(0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TvMovieGrid(movies: LazyPagingItems<Movie>, viewModel: MovieViewModel, gridState: androidx.tv.foundation.lazy.grid.TvLazyGridState, gridFocusRequester: FocusRequester, columns: Int, activeFocusIndex: Int, onMovieClicked: (Movie, Int, ClickType) -> Unit, onMovieFocused: (Movie) -> Unit, onNavigateToSidebar: () -> Unit, onNavigateUp: () -> Unit, modifier: Modifier = Modifier) {
    val gridUiState by viewModel.uiState.collectAsStateWithLifecycle(); val isSidebarOpen = gridUiState.isSidebarFocused
    val favoriteIds = remember(gridUiState.favorites) { gridUiState.favorites.map { it.streamId }.toSet() }
    TvLazyVerticalGrid(columns = TvGridCells.Fixed(columns), state = gridState, modifier = modifier.focusRequester(gridFocusRequester).fillMaxSize().background(Color.Black.copy(0.2f), RoundedCornerShape(12.dp)), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), userScrollEnabled = true) {
        items(count = movies.itemCount, key = movies.itemKey { it.streamId }) { index ->
            val movie = movies[index]; if (movie != null) {
                val isLeftEdge = index % columns == 0; val isTopRow = index < columns; val itemFR = remember { FocusRequester() }; val isRestoring = index == activeFocusIndex
                LaunchedEffect(isRestoring) { if (isRestoring) { delay(250); try { itemFR.requestFocus(); viewModel.clearActiveFocus() } catch (_: Exception) {} } }
                val keyHandler = remember(isLeftEdge, isTopRow, isRestoring) { Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown) when (e.key) { Key.DirectionLeft -> if (isLeftEdge) { onNavigateToSidebar(); true } else false; Key.DirectionUp -> if (isTopRow) { onNavigateUp(); true } else false; else -> false } else false }.then(if (isRestoring) Modifier.focusRequester(itemFR) else Modifier) }
                TvMaterialMovieCard(movie = movie, isFavorite = favoriteIds.contains(movie.streamId), onMovieClicked = { type -> onMovieClicked(movie, index, type) }, onFocusRestored = { viewModel.clearLastClickedItem() }, onFocused = { if (isSidebarOpen) onMovieFocused(movie) }, modifier = keyHandler)
            } else { Box(Modifier.fillMaxWidth().height(160.dp).background(Color.White.copy(0.1f), RoundedCornerShape(12.dp)).focusable()) }
        }
    }
}

fun getCategoryColor(name: String): Color { val h = name.hashCode(); return Color((h and 0xFF0000 shr 16) / 2 + 128, (h and 0x00FF00 shr 8) / 2 + 128, (h and 0x0000FF) / 2 + 128, 255) }
fun getCategoryInitials(name: String): String { val clean = name.replace(Regex("^(PT/BR|PT|BR|EN|UK|FR|USA|VOD)\\s*[-:|]\\s*", RegexOption.IGNORE_CASE), "").trim(); val words = clean.split(" ").filter { it.isNotEmpty() }; return if (words.size >= 2) (words[0].take(1) + words[1].take(1)).uppercase() else clean.take(2).uppercase() }
