package pt.hitv.feature.player.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Category
import pt.hitv.core.model.Channel
import pt.hitv.feature.player.LivePlayerViewModel
import pt.hitv.feature.player.PlaybackState
import pt.hitv.feature.player.helpers.ChannelNavigationHelper
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * Full-screen channel player composable — matches original project layout.
 * The video surface is provided by the platform via playerViewFactory lambda.
 * All overlay UI is shared Compose.
 */
@Composable
fun ChannelPlayerScreen(
    viewModel: LivePlayerViewModel,
    sleepTimerManager: SleepTimerManager,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigatePrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit,
    onForceRotation: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val sleepTimerRemaining by sleepTimerManager.remainingMs.collectAsState()
    val themeColors = getThemeColors()

    val channels = uiState.cachedChannels ?: emptyList()
    val categoryMap = remember(uiState.categories) {
        uiState.categories.associateBy { it.categoryId.toString() }
    }

    // Search state for channel list
    var searchQuery by remember { mutableStateOf("") }
    val searchWords = remember(searchQuery) { ChannelNavigationHelper.parseSearchWords(searchQuery) }
    val filteredCategories = remember(channels, categoryMap, searchWords) {
        ChannelNavigationHelper.filterCategories(channels, categoryMap, searchWords)
    }
    val filteredChannels = remember(channels, uiState.selectedCategoryId, searchWords) {
        ChannelNavigationHelper.filterChannels(channels, searchWords, uiState.selectedCategoryId, categoryMap)
    }

    // Auto-hide controls — 5s (matches original)
    var controlsInteractionCounter by remember { mutableStateOf(0) }
    LaunchedEffect(uiState.isControlsVisible, controlsInteractionCounter) {
        if (uiState.isControlsVisible && !uiState.isChannelListVisible) {
            delay(5000)
            viewModel.setControlsVisible(false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                if (uiState.isChannelListVisible) {
                    viewModel.setChannelListVisible(false)
                    viewModel.setControlsVisible(true)
                } else {
                    viewModel.toggleControlsVisibility()
                }
                controlsInteractionCounter++
            }
    ) {
        // Video surface
        playerViewFactory(Modifier.fillMaxSize())

        // Buffering indicator
        if (uiState.playbackState is PlaybackState.Buffering) {
            BufferingIndicator()
        }

        // Auto-retrying indicator
        val playbackError = uiState.playbackState as? PlaybackState.Error
        if (playbackError?.isRetrying == true) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Retrying (${playbackError.retryCount}/${playbackError.maxRetries})...",
                    color = Color.White, fontSize = 14.sp
                )
            }
        }

        // === Top bar (matches original CastPlayerTopBar layout) ===
        if (uiState.isControlsVisible) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            startY = 0f, endY = 120f
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: back + channel list + name
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onBack() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { viewModel.toggleChannelList(); controlsInteractionCounter++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = "Channels", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Text(
                            text = uiState.currentChannelName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    // Right side: aspect ratio + sleep timer
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sleepTimerRemaining > 0) {
                            SleepTimerIndicator(
                                remainingMs = sleepTimerRemaining,
                                onClick = { viewModel.toggleSleepTimerDialog(); controlsInteractionCounter++ }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        IconButton(
                            onClick = { viewModel.cycleResizeMode(); controlsInteractionCounter++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { onForceRotation(); controlsInteractionCounter++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ScreenRotation, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { viewModel.toggleSleepTimerDialog(); controlsInteractionCounter++ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = "Sleep Timer", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // === Dismiss overlay when sidebar visible ===
        if (uiState.isChannelListVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { viewModel.setChannelListVisible(false) }
            )
        }

        // === Channel list sidebar (from LEFT, matches original) ===
        AnimatedVisibility(
            visible = uiState.isChannelListVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(340.dp)
                    .background(themeColors.backgroundPrimary.copy(alpha = 0.95f))
            ) {
                ChannelListSidebar(
                    categories = filteredCategories,
                    selectedCategory = uiState.selectedCategoryId,
                    onCategorySelect = { viewModel.setSelectedCategoryId(it); controlsInteractionCounter++ },
                    channels = filteredChannels,
                    currentChannelUrl = uiState.currentChannelUrl,
                    onChannelClick = { channel ->
                        onChannelClick(channel)
                        viewModel.saveRecentlyViewedChannel(channel, Clock.System.now().toEpochMilliseconds())
                        controlsInteractionCounter++
                    },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it }
                )
            }
        }

        // === Bottom EPG overlay (matches original) ===
        if (uiState.isControlsVisible && !uiState.isChannelListVisible && uiState.currentChannelEpg != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                EpgInfoOverlay(epgInfo = uiState.currentChannelEpg!!)
            }
        }

        // === Sleep Timer Dialog ===
        if (uiState.showSleepTimerDialog) {
            SleepTimerDialog(
                isTimerActive = sleepTimerRemaining > 0,
                remainingMs = sleepTimerRemaining,
                onDurationSelected = { durationMs ->
                    onSleepTimerSelect(durationMs)
                    viewModel.dismissSleepTimerDialog()
                },
                onCancel = {
                    onSleepTimerCancel()
                    viewModel.dismissSleepTimerDialog()
                },
                onDismiss = { viewModel.dismissSleepTimerDialog() }
            )
        }

        // === Error dialog ===
        if (uiState.showErrorDialog) {
            PlaybackErrorDialog(
                errorMessage = uiState.errorMessage,
                onRetry = { viewModel.dismissErrorDialog(); onRetry() },
                onDismiss = { viewModel.dismissErrorDialog() }
            )
        }
    }
}

// === Channel list sidebar — matches original PlayerComposables.ChannelListSidebar ===

@Composable
private fun ChannelListSidebar(
    categories: List<Pair<String, String>>,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    channels: List<Channel>,
    currentChannelUrl: String,
    onChannelClick: (Channel) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val themeColors = getThemeColors()
    val isSearching = searchQuery.trim().isNotEmpty()

    val fullCategories = remember(categories) {
        listOf<Pair<String?, String>>(null to "All Categories") + categories
    }

    val categoryListState = rememberLazyListState()
    val channelListState = rememberLazyListState()

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(340.dp)
            .background(themeColors.backgroundSecondary)
            .padding(vertical = 8.dp)
    ) {
        // Search field (OutlinedTextField style)
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .background(themeColors.primaryColor.copy(alpha = 0.11f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                textStyle = TextStyle(color = themeColors.textColor, fontSize = 15.sp),
                singleLine = true,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text("Search channels...", color = Color.Gray, fontSize = 15.sp)
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = themeColors.textColor, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Search results count
        if (isSearching) {
            Text(
                text = "${channels.size} channel${if (channels.size != 1) "s" else ""} found",
                color = themeColors.textColor.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Vertical category list (when not searching)
        if (!isSearching && categories.isNotEmpty()) {
            LazyColumn(
                state = categoryListState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp)
            ) {
                items(fullCategories.size, key = { fullCategories[it].first ?: "all" }) { index ->
                    val cat = fullCategories[index]
                    val isSelected = (cat.first == selectedCategory) || (cat.first == null && selectedCategory == null)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategorySelect(cat.first) }
                            .background(if (isSelected) themeColors.primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = cat.second.ifBlank { "Uncategorized" },
                            color = if (isSelected) themeColors.primaryColor else themeColors.textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Divider
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp)
                .background(themeColors.textColor.copy(alpha = 0.2f))
        )

        // Channel list
        LazyColumn(
            state = channelListState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            items(channels.size, key = { channels[it].id ?: channels[it].hashCode() }) { idx ->
                val channel = channels[idx]
                val isPlaying = remember(channel.streamUrl, currentChannelUrl) {
                    ChannelNavigationHelper.isChannelPlaying(channel.streamUrl, currentChannelUrl)
                }
                ChannelListItem(
                    channel = channel,
                    isSelected = isPlaying,
                    onClick = { onChannelClick(channel) }
                )
            }
        }
    }

    // Auto-scroll to selected category
    LaunchedEffect(selectedCategory, fullCategories) {
        if (!isSearching && categories.isNotEmpty()) {
            val selectedIndex = fullCategories.indexOfFirst {
                (it.first == selectedCategory) || (it.first == null && selectedCategory == null)
            }
            if (selectedIndex >= 0) {
                delay(100)
                categoryListState.animateScrollToItem(selectedIndex)
            }
        }
    }

    // Auto-scroll to current playing channel
    LaunchedEffect(currentChannelUrl, channels) {
        if (channels.isNotEmpty() && currentChannelUrl.isNotEmpty()) {
            val currentChannelIndex = channels.indexOfFirst {
                ChannelNavigationHelper.isChannelPlaying(it.streamUrl, currentChannelUrl)
            }
            if (currentChannelIndex >= 0) {
                delay(100)
                channelListState.scrollToItem(currentChannelIndex)
            }
        }
    }
}

@Composable
private fun ChannelListItem(
    channel: Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val themeColors = getThemeColors()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) themeColors.primaryColor.copy(alpha = 0.4f) else Color.Transparent)
            .clickable { onClick() }
            .let { mod ->
                if (isSelected) mod.padding(start = 4.dp) else mod.padding(start = 8.dp)
            }
            .padding(end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left bar indicator for playing channel
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(themeColors.primaryColor)
            )
            Spacer(Modifier.width(4.dp))
        }

        // Channel logo
        Card(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = themeColors.primaryColor.copy(alpha = 0.12f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = (channel.name ?: "?").take(2).uppercase(),
                    color = themeColors.textColor.copy(alpha = 0.5f),
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = channel.name.orEmpty(),
            color = if (isSelected) themeColors.primaryColor else themeColors.textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Playing indicator icon
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Now playing",
                tint = themeColors.primaryColor,
                modifier = Modifier.size(20.dp).padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun SleepTimerDialog(
    isTimerActive: Boolean,
    remainingMs: Long,
    onDurationSelected: (Long) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val themeColors = getThemeColors()

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer", color = themeColors.textColor, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (isTimerActive) {
                    Text(
                        text = "Timer active: ${SleepTimerManager.formatRemainingTime(remainingMs)}",
                        color = themeColors.primaryColor, fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                SleepTimerManager.PRESET_DURATIONS.forEach { (durationMs, label) ->
                    Text(
                        text = label, color = themeColors.textColor, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onDurationSelected(durationMs) }.padding(vertical = 10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isTimerActive) {
                androidx.compose.material3.TextButton(onClick = onCancel) { Text("Cancel Timer", color = themeColors.danger) }
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close", color = themeColors.textColor.copy(alpha = 0.7f)) }
        },
        containerColor = themeColors.backgroundSecondary
    )
}
