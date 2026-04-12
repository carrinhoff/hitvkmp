package pt.hitv.feature.channels.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import pt.hitv.core.data.paging.CHANNEL_FILTER_ALL
import pt.hitv.core.data.paging.CHANNEL_FILTER_FAVORITES
import pt.hitv.core.data.paging.CHANNEL_FILTER_RECENTLY_VIEWED
import pt.hitv.core.designsystem.compose.AdvancedCategoryBottomSheetContent
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Channel
import pt.hitv.feature.channels.ui.components.ChannelPreviewComposable
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.core.model.Category
import pt.hitv.core.model.enums.ClickType
import pt.hitv.feature.channels.StreamUiState
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.feature.channels.ui.components.ChannelListItem

/**
 * Mobile channels layout — matches original MobileChannelsLayout.
 *
 * Top bar: Category dropdown ("All" ▼) + search icon
 * List: LazyColumn with 12dp spacing, 16dp padding
 * Channel items: 100dp height with 76dp logo placeholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileChannelsLayout(
    viewModel: StreamViewModel,
    uiState: StreamUiState,
    onChannelClicked: (Channel, Int) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChanged: (String?) -> Unit,
    scrollToTopSignal: Int = 0
) {
    val themeColors = getThemeColors()
    val lazyPagingItems = viewModel.channelsPagerFlow.collectAsLazyPagingItems()
    val listState = rememberLazyListState()

    // EPG cache shared across all channel list items
    val epgCache = remember { SnapshotStateMap<String, ChannelEpgInfo?>() }
    val epgLoadingSet = remember { mutableStateListOf<String>() }

    // Refresh paging when favorites toggle (or other events)
    LaunchedEffect(Unit) {
        viewModel.refreshPagingEvent.collect {
            lazyPagingItems.refresh()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Category dropdown
    var showCategorySheet by remember { mutableStateOf(false) }
    val selectedCategoryName = remember(uiState.currentCategoryFilter, uiState.categories) {
        when (uiState.currentCategoryFilter) {
            CHANNEL_FILTER_ALL -> "All"
            CHANNEL_FILTER_FAVORITES -> "Favorites"
            CHANNEL_FILTER_RECENTLY_VIEWED -> "Recently Viewed"
            else -> uiState.categories.find {
                it.categoryId.toString() == uiState.currentCategoryFilter
            }?.categoryName ?: "All"
        }
    }

    // Scroll to top on signal
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.backgroundPrimary)
    ) {
        // === Top App Bar — ported from original SharedMobileTopAppBar ===
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
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { query ->
                                            searchQuery = query
                                            onSearchQueryChanged(query.takeIf { it.isNotBlank() })
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = themeColors.textColor
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(themeColors.primaryColor),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    "Search...",
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
                                                onSearchQueryChanged(null)
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
                        // Category selector card — matches original exactly
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
                        onSearchQueryChanged(null)
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

        // Track which channel has expanded preview
        var expandedChannelId by remember { mutableStateOf<String?>(null) }

        // === Channel List — matches original spacing ===
        if (uiState.isLoading && lazyPagingItems.itemCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColors.primaryColor)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = lazyPagingItems.itemKey { it.name + it.categoryId }
                ) { index ->
                    val channel = lazyPagingItems[index]
                    if (channel != null) {
                        var lastClickTime by remember { mutableLongStateOf(0L) }
                        var lastClickedId by remember { mutableStateOf<String?>(null) }

                        Column {
                            ChannelListItem(
                                channel = channel,
                                onChannelClicked = { clickType ->
                                    when (clickType) {
                                        ClickType.CLICK -> {
                                            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                            val isDoubleClick = (now - lastClickTime < 500L) && lastClickedId == channel.id

                                            if (isDoubleClick) {
                                                // Double click → full player
                                                onChannelClicked(channel, index)
                                                expandedChannelId = null
                                            } else {
                                                // Single click → toggle preview
                                                expandedChannelId = if (expandedChannelId == channel.id) null else channel.id
                                            }
                                            lastClickTime = now
                                            lastClickedId = channel.id
                                        }
                                        ClickType.LONG_CLICK -> viewModel.saveFavoriteChannel(
                                            channel,
                                            uiState.currentCategoryFilter == CHANNEL_FILTER_FAVORITES
                                        )
                                    }
                                },
                                epgCache = epgCache,
                                epgLoadingSet = epgLoadingSet,
                                isExpanded = expandedChannelId == channel.id,
                                viewModel = viewModel
                            )

                            // Expandable preview
                            AnimatedVisibility(
                                visible = expandedChannelId == channel.id && !channel.streamUrl.isNullOrBlank(),
                                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                            ) {
                                ChannelPreviewComposable(
                                    channel = channel,
                                    onClose = { expandedChannelId = null },
                                    onPreviewClicked = {
                                        expandedChannelId = null
                                        onChannelClicked(channel, index)
                                    }
                                )
                            }
                        }
                    }
                }

                // Loading more indicator
                if (lazyPagingItems.loadState.append is app.cash.paging.LoadStateLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = themeColors.primaryColor,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }

    // === Category Bottom Sheet ===
    if (showCategorySheet) {
        var categorySearchQuery by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            viewModel.fetchCategoryCounts()
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
                categories = uiState.categories,
                selectedCategoryFilter = uiState.currentCategoryFilter ?: CHANNEL_FILTER_ALL,
                categorySearchQuery = categorySearchQuery,
                onCategorySearchQueryChanged = { categorySearchQuery = it },
                onCategorySelected = { filterId ->
                    onCategorySelected(filterId)
                    categorySearchQuery = ""
                    showCategorySheet = false
                },
                onDismiss = {
                    categorySearchQuery = ""
                    showCategorySheet = false
                },
                allFilterId = CHANNEL_FILTER_ALL,
                favoritesFilterId = CHANNEL_FILTER_FAVORITES,
                recentlyViewedFilterId = CHANNEL_FILTER_RECENTLY_VIEWED,
                allCount = uiState.categoryCounts[CHANNEL_FILTER_ALL]?.toString() ?: "...",
                favoritesCount = uiState.favorites.size.toString(),
                recentCount = uiState.recentlyViewedChannels.size.toString(),
                categoryCounts = uiState.categoryCounts.mapValues { it.value.toString() }
            )
        }
    }
}
