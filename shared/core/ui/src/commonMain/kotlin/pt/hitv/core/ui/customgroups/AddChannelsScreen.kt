package pt.hitv.core.ui.customgroups

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.model.Channel
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.theme.ThemeManager

/**
 * Screen for adding channels to a custom group.
 * Simplified version without Paging 3 (uses simple list for KMP compatibility).
 *
 * @param groupId ID of the group to add channels to
 * @param viewModel ViewModel managing channel selection
 * @param onNavigateBack Callback for back navigation
 * @param onChannelsAdded Callback when channels are added
 * @param addChannelsLabel Screen title
 * @param backLabel Back button content description
 * @param searchPlaceholder Search placeholder text
 * @param clearSearchLabel Clear search content description
 * @param addToGroupLabel Add to group button label
 * @param inGroupLabel Label for channels already in group
 * @param noChannelsLabel Empty state label
 * @param errorLabel Error state button label
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChannelsScreen(
    groupId: Long,
    viewModel: AddChannelsViewModel,
    onNavigateBack: () -> Unit,
    onChannelsAdded: () -> Unit,
    addChannelsLabel: String = "Add Channels",
    backLabel: String = "Back",
    searchPlaceholder: String = "Search channels...",
    clearSearchLabel: String = "Clear",
    addToGroupLabel: String = "Add to Group",
    inGroupLabel: String = "In group",
    noChannelsLabel: String = "No channels found",
    errorLabel: String = "Error loading channels"
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedChannels by viewModel.selectedChannels.collectAsState()
    val existingChannelIds by viewModel.existingChannelIds.collectAsState()
    val allChannels by viewModel.allChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val themeColors = getThemeColors()

    LaunchedEffect(groupId) {
        viewModel.loadExistingChannels(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = addChannelsLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = themeColors.textColor.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = backLabel,
                            tint = themeColors.textColor.copy(alpha = 0.9f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    titleContentColor = themeColors.textColor,
                    navigationIconContentColor = themeColors.textColor
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(48.dp)
                    .background(
                        color = themeColors.textColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = themeColors.textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = LocalTextStyle.current.copy(
                            color = themeColors.textColor,
                            fontSize = 16.sp
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    searchPlaceholder,
                                    color = themeColors.textColor.copy(alpha = 0.5f),
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setSearchQuery("") },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = clearSearchLabel,
                                tint = themeColors.textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Selected channels action bar
            if (selectedChannels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            color = themeColors.primaryColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = themeColors.primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${selectedChannels.size} selected",
                            color = themeColors.textColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.addSelectedChannels(groupId)
                            onChannelsAdded()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryColor,
                            contentColor = themeColors.textColor
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = addToGroupLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Channel list
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = themeColors.primaryColor)
                }
            } else if (allChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOff,
                            contentDescription = null,
                            tint = themeColors.textColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = noChannelsLabel,
                            color = themeColors.textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = allChannels.size,
                        key = { index -> allChannels[index].id ?: index }
                    ) { index ->
                        val channel = allChannels[index]
                        val channelId = channel.id?.toLongOrNull() ?: 0L
                        val isInGroup = existingChannelIds.contains(channelId)
                        val isSelected = selectedChannels.any { it.id == channel.id }

                        AddChannelItem(
                            channel = channel,
                            isInGroup = isInGroup,
                            isSelected = isSelected,
                            onToggleSelection = { viewModel.toggleChannelSelection(channel) },
                            themeColors = themeColors,
                            inGroupLabel = inGroupLabel
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddChannelItem(
    channel: Channel,
    isInGroup: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    themeColors: ThemeManager.AppTheme,
    inGroupLabel: String
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> themeColors.primaryColor.copy(alpha = 0.15f)
            else -> themeColors.textColor.copy(alpha = 0.05f)
        },
        label = "itemBackground"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isInGroup) { onToggleSelection() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = channel.streamIcon,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
        }

        Text(
            text = channel.name.orEmpty(),
            color = themeColors.textColor.copy(alpha = if (isInGroup) 0.5f else 1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isInGroup) {
            Text(
                text = inGroupLabel,
                color = themeColors.textColor.copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        } else {
            val checkboxColor by animateColorAsState(
                targetValue = if (isSelected) themeColors.primaryColor else themeColors.textColor.copy(alpha = 0.3f),
                label = "checkboxColor"
            )

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = checkboxColor,
                    uncheckedColor = checkboxColor
                )
            )
        }
    }
}
