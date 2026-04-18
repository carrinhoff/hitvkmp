package pt.hitv.feature.channels.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.cash.paging.compose.collectAsLazyPagingItems
import app.cash.paging.compose.itemKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.paging.CHANNEL_FILTER_ALL
import pt.hitv.core.data.paging.CHANNEL_FILTER_CATCH_UP
import pt.hitv.core.data.paging.CHANNEL_FILTER_FAVORITES
import pt.hitv.core.data.paging.CHANNEL_FILTER_RECENTLY_VIEWED
import pt.hitv.core.designsystem.compose.AdvancedCategoryBottomSheetContent
import pt.hitv.core.designsystem.compose.ChannelListItemSkeleton
import pt.hitv.core.designsystem.compose.SharedEmptyMessage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.domain.manager.ParentalControlManager
import pt.hitv.core.model.Channel
import pt.hitv.core.sync.SyncStateManager
import pt.hitv.feature.channels.ui.components.ChannelPreviewComposable
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.core.model.Category
import pt.hitv.core.model.ContentType
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.ui.categories.ManageCategoriesScreen
import pt.hitv.core.ui.categories.ManageCategoriesViewModel
import pt.hitv.core.ui.components.PagingErrorState
import pt.hitv.core.ui.components.ParentalControlCheck
import pt.hitv.core.ui.components.SearchHistoryChips
import pt.hitv.core.ui.customgroups.AddChannelsScreen
import pt.hitv.core.ui.customgroups.AddChannelsViewModel
import pt.hitv.core.ui.customgroups.CreateGroupDialog
import pt.hitv.core.ui.customgroups.CustomGroupsScreen
import pt.hitv.core.ui.customgroups.CustomGroupsViewModel
import pt.hitv.core.ui.customgroups.EditGroupScreen
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.data.paging.CHANNEL_FILTER_CUSTOM_GROUP_PREFIX
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
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
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

    // Koin-injected deps for parental / manage categories / custom groups
    val parentalControlManager: ParentalControlManager = koinInject()
    val preferencesHelper: PreferencesHelper = koinInject()
    val manageCategoriesViewModel: ManageCategoriesViewModel = koinInject()
    val customGroupsViewModel: CustomGroupsViewModel = koinInject()
    val addChannelsViewModel: AddChannelsViewModel = koinInject()
    val syncStateManager: SyncStateManager = koinInject()
    val userId = remember { preferencesHelper.getUserId() }
    val coroutineScope = rememberCoroutineScope()

    // React to sync completion — Voyager caches the Navigator across the
    // sync overlay's dispose/restore, so LaunchedEffect(Unit) only fires once
    // (pre-sync, when the DB was empty). Keying on syncVersion guarantees we
    // re-trigger the paging refresh the moment sync increments it.
    val syncVersion by syncStateManager.syncVersion.collectAsState()
    LaunchedEffect(syncVersion) {
        // Pull fresh categories/favorites/recent + invalidate paging cache.
        viewModel.refreshAfterSync()
        lazyPagingItems.refresh()
    }

    val customGroups by customGroupsViewModel.customGroups.collectAsState()

    // EPG cache shared across all channel list items
    val epgCache = remember { SnapshotStateMap<String, ChannelEpgInfo?>() }
    val epgLoadingSet = remember { mutableStateListOf<String>() }

    // Unconditional first-composition refresh. The channels PagingData is
    // cached in viewModelScope, and if the VM was created while the DB was
    // still empty (right after login / during sync), the cached empty result
    // sticks until something invalidates. This guarantees a fresh load.
    LaunchedEffect(Unit) {
        lazyPagingItems.refresh()
    }

    // Refresh paging when favorites toggle or refreshAfterSync fires.
    LaunchedEffect(Unit) {
        viewModel.refreshPagingEvent.collect {
            lazyPagingItems.refresh()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Original behaviour: 1500 ms debounce + 3-char minimum (avoids hammering FTS
    // on every keystroke and fetching results for one or two characters).
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(1500)
            .distinctUntilChanged()
            .collect { raw ->
                val trimmed = raw.trim()
                when {
                    trimmed.isEmpty() -> onSearchQueryChanged(null)
                    trimmed.length >= 3 -> onSearchQueryChanged(trimmed)
                    else -> { /* 1-2 chars: hold until the user types more or clears */ }
                }
            }
    }

    // Category dropdown + manage categories + custom groups
    var showCategorySheet by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var showCustomGroupsDialog by remember { mutableStateOf(false) }

    // Parental control PIN flow — pending category selection
    var pendingCategoryFilter by remember { mutableStateOf<String?>(null) }
    var pendingCategoryId by remember { mutableStateOf<Long?>(null) }

    // Hide top bar on scroll-down, show on scroll-up (kicks in on any orientation
    // — original does this only in landscape but CMP has no simple config.orientation).
    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dy = available.y
                if (dy < -5f) isTopBarVisible = false
                else if (dy > 5f) isTopBarVisible = true
                return Offset.Zero
            }
        }
    }
    val selectedCategoryName = remember(uiState.currentCategoryFilter, uiState.categories, customGroups) {
        when (val filter = uiState.currentCategoryFilter) {
            CHANNEL_FILTER_ALL -> "All"
            CHANNEL_FILTER_FAVORITES -> "Favorites"
            CHANNEL_FILTER_RECENTLY_VIEWED -> "Recently Viewed"
            CHANNEL_FILTER_CATCH_UP -> "Catch Up"
            null -> "All"
            else -> {
                if (filter.startsWith(CHANNEL_FILTER_CUSTOM_GROUP_PREFIX)) {
                    val gid = filter.removePrefix(CHANNEL_FILTER_CUSTOM_GROUP_PREFIX).toLongOrNull()
                    customGroups.find { it.id == gid }?.name ?: "All"
                } else {
                    uiState.categories.find {
                        it.categoryId.toString() == filter
                    }?.categoryName ?: "All"
                }
            }
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
            .nestedScroll(nestedScrollConnection)
    ) {
        // === Top App Bar — ported from original SharedMobileTopAppBar ===
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                                            // Just update local state — the debounced
                                            // LaunchedEffect above fires the actual query.
                                            searchQuery = query
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
        }

        // Recent searches strip — only when search is focused + query is empty.
        if (isSearchActive && searchQuery.isEmpty()) {
            val historyItems by viewModel.searchHistory.collectAsState()
            SearchHistoryChips(
                items = historyItems,
                onItemClick = { item ->
                    searchQuery = item.query
                    onSearchQueryChanged(item.query)
                },
                onItemDelete = { viewModel.deleteSearchHistoryItem(it.id) },
                onClearAll = { viewModel.clearSearchHistory() },
                primaryColor = themeColors.primaryColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Track which channel has expanded preview (portrait inline) AND which is
        // currently mirrored to the side pane (landscape).
        var expandedChannelId by remember { mutableStateOf<String?>(null) }
        var selectedPreviewChannel by remember { mutableStateOf<Channel?>(null) }

        // === Channel List — matches original spacing ===
        val hasAnyContent = uiState.categories.isNotEmpty() ||
            uiState.favorites.isNotEmpty() ||
            uiState.recentlyViewedChannels.isNotEmpty() ||
            lazyPagingItems.itemCount > 0

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Mirrors the original `MobileChannelsLandscape` 40/60 split. Threshold of
            // 600 dp catches landscape phones (≥640 dp typical) and tablets — matches
            // the spanCount guard the original used in the legacy XML grid.
            val isWide = maxWidth >= 600.dp
            val refreshState = lazyPagingItems.loadState.refresh

            val listBlock: @Composable () -> Unit = {
                if (!hasAnyContent && refreshState is app.cash.paging.LoadStateError) {
            // Initial load failed — show retry instead of empty/skeletons.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PagingErrorState(
                    message = refreshState.error.message,
                    onRetry = { lazyPagingItems.retry() }
                )
            }
        } else if (!hasAnyContent && uiState.isLoading) {
            // Shimmer skeletons while initial data loads
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(10) { ChannelListItemSkeleton() }
            }
        } else if (!hasAnyContent) {
            // Empty state after loading completes
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SharedEmptyMessage(
                    message = "No channels available",
                    subtitle = "Pull to refresh or try a different category."
                )
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
                                                // Single click → toggle preview (portrait)
                                                // and mirror the selection to the side pane (landscape).
                                                expandedChannelId = if (expandedChannelId == channel.id) null else channel.id
                                                selectedPreviewChannel = channel
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

                            // Expandable preview — only inline in portrait. In landscape
                            // the preview lives in the side pane.
                            AnimatedVisibility(
                                visible = !isWide && expandedChannelId == channel.id && !channel.streamUrl.isNullOrBlank(),
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

                // Append error — small inline retry so the user can recover without
                // losing already-loaded items.
                val appendState = lazyPagingItems.loadState.append
                if (appendState is app.cash.paging.LoadStateError) {
                    item {
                        PagingErrorState(
                            message = appendState.error.message,
                            onRetry = { lazyPagingItems.retry() }
                        )
                    }
                }
            }
        }
            } // end listBlock lambda

            if (isWide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.4f).fillMaxHeight()) { listBlock() }
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .background(themeColors.backgroundSecondary)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val ch = selectedPreviewChannel
                        if (ch != null && !ch.streamUrl.isNullOrBlank()) {
                            ChannelPreviewComposable(
                                channel = ch,
                                onClose = { selectedPreviewChannel = null },
                                onPreviewClicked = { onChannelClicked(ch, -1) }
                            )
                        } else {
                            Text(
                                text = "Select a channel to preview",
                                color = themeColors.textColor.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                listBlock()
            }
        } // end BoxWithConstraints
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
                    // Parental PIN check — only for real category IDs (numeric), not for
                    // All / Favorites / RecentlyViewed / custom groups.
                    val numericId = filterId.toLongOrNull()
                    if (numericId != null) {
                        coroutineScope.launch {
                            val locked = parentalControlManager.requiresPinForCategory(numericId, userId)
                            if (locked) {
                                pendingCategoryFilter = filterId
                                pendingCategoryId = numericId
                                showCategorySheet = false
                                categorySearchQuery = ""
                            } else {
                                onCategorySelected(filterId)
                                showCategorySheet = false
                                categorySearchQuery = ""
                            }
                        }
                    } else {
                        onCategorySelected(filterId)
                        showCategorySheet = false
                        categorySearchQuery = ""
                    }
                },
                onDismiss = {
                    categorySearchQuery = ""
                    showCategorySheet = false
                },
                allFilterId = CHANNEL_FILTER_ALL,
                favoritesFilterId = CHANNEL_FILTER_FAVORITES,
                recentlyViewedFilterId = CHANNEL_FILTER_RECENTLY_VIEWED,
                catchUpFilterId = CHANNEL_FILTER_CATCH_UP,
                allCount = uiState.categoryCounts[CHANNEL_FILTER_ALL]?.toString() ?: "...",
                favoritesCount = uiState.favorites.size.toString(),
                recentCount = uiState.recentlyViewedChannels.size.toString(),
                catchUpCount = (uiState.categoryCounts[CHANNEL_FILTER_CATCH_UP] ?: 0).toString(),
                showCatchUp = (uiState.categoryCounts[CHANNEL_FILTER_CATCH_UP] ?: 0) > 0,
                categoryCounts = uiState.categoryCounts.mapValues { it.value.toString() },
                customGroups = customGroups,
                customGroupPrefix = CHANNEL_FILTER_CUSTOM_GROUP_PREFIX,
                onManageCategories = {
                    showCategorySheet = false
                    categorySearchQuery = ""
                    showManageCategoriesDialog = true
                },
                onManageCustomGroups = {
                    showCategorySheet = false
                    categorySearchQuery = ""
                    showCustomGroupsDialog = true
                }
            )
        }
    }

    // === Custom Groups management dialog ===
    if (showCustomGroupsDialog) {
        val groupChannels by customGroupsViewModel.groupChannels.collectAsState()
        var showCreateDialog by remember { mutableStateOf(false) }
        var showEditScreen by remember { mutableStateOf(false) }
        var showAddChannelsScreen by remember { mutableStateOf(false) }
        var selectedGroup by remember { mutableStateOf<CustomGroup?>(null) }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCustomGroupsDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            when {
                showAddChannelsScreen && selectedGroup != null -> {
                    AddChannelsScreen(
                        groupId = selectedGroup!!.id,
                        viewModel = addChannelsViewModel,
                        onNavigateBack = { showAddChannelsScreen = false },
                        onChannelsAdded = {
                            showAddChannelsScreen = false
                            customGroupsViewModel.loadGroupChannels(selectedGroup!!.id)
                            val currentFilter = uiState.currentCategoryFilter
                            val groupFilter = "${CHANNEL_FILTER_CUSTOM_GROUP_PREFIX}${selectedGroup!!.id}"
                            if (currentFilter == groupFilter) {
                                viewModel.invalidateChannelsPaging()
                            }
                        }
                    )
                }
                showEditScreen && selectedGroup != null -> {
                    LaunchedEffect(selectedGroup) {
                        customGroupsViewModel.loadGroupChannels(selectedGroup!!.id)
                    }
                    EditGroupScreen(
                        group = selectedGroup!!,
                        channels = groupChannels,
                        onNavigateBack = {
                            showEditScreen = false
                            selectedGroup = null
                        },
                        onSaveGroup = { updated -> customGroupsViewModel.updateCustomGroup(updated) },
                        onAddChannels = { showAddChannelsScreen = true },
                        onRemoveChannel = { channel ->
                            val cid = channel.id?.toLongOrNull()
                            if (cid != null) {
                                customGroupsViewModel.removeChannelFromGroup(selectedGroup!!.id, cid)
                                val currentFilter = uiState.currentCategoryFilter
                                val groupFilter = "${CHANNEL_FILTER_CUSTOM_GROUP_PREFIX}${selectedGroup!!.id}"
                                if (currentFilter == groupFilter) {
                                    viewModel.invalidateChannelsPaging()
                                }
                            }
                        },
                        onReorderChannels = { /* drag-reorder not implemented yet */ }
                    )
                }
                else -> {
                    CustomGroupsScreen(
                        customGroups = customGroups,
                        onCreateGroup = { showCreateDialog = true },
                        onEditGroup = { group ->
                            selectedGroup = group
                            showEditScreen = true
                        },
                        onDeleteGroup = { group -> customGroupsViewModel.deleteCustomGroup(group.id) },
                        onNavigateBack = { showCustomGroupsDialog = false }
                    )
                }
            }
        }

        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, icon ->
                    customGroupsViewModel.createCustomGroup(name, icon)
                    showCreateDialog = false
                }
            )
        }
    }

    // === Manage Categories dialog ===
    if (showManageCategoriesDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showManageCategoriesDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            ManageCategoriesScreen(
                viewModel = manageCategoriesViewModel,
                backgroundColor = themeColors.backgroundPrimary,
                primaryColor = themeColors.primaryColor,
                secondaryBackgroundColor = themeColors.backgroundSecondary,
                textColor = themeColors.textColor,
                textSecondaryColor = themeColors.textColor.copy(alpha = 0.7f),
                singleContentType = ContentType.CHANNELS,
                onNavigateBack = { showManageCategoriesDialog = false }
            )
        }
    }

    // === Parental Control PIN check ===
    val pid = pendingCategoryId
    val pfilter = pendingCategoryFilter
    if (pid != null && pfilter != null) {
        ParentalControlCheck(
            categoryId = pid,
            userId = userId,
            parentalControlManager = parentalControlManager,
            onAccessGranted = {
                onCategorySelected(pfilter)
                pendingCategoryId = null
                pendingCategoryFilter = null
            },
            onDismiss = {
                pendingCategoryId = null
                pendingCategoryFilter = null
            }
        )
    }
}
