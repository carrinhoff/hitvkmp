package pt.hitv.core.ui.customgroups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.theme.ThemeManager

/**
 * Screen for editing a custom channel group.
 *
 * @param group The group being edited
 * @param channels Channels in the group
 * @param onNavigateBack Callback for back navigation
 * @param onSaveGroup Callback to save group changes
 * @param onAddChannels Callback to add channels
 * @param onRemoveChannel Callback to remove a channel
 * @param onReorderChannels Callback to reorder channels
 * @param editGroupLabel Screen title
 * @param backLabel Back button content description
 * @param groupNameLabel Group name field label
 * @param addChannelsLabel Add channels button label
 * @param noChannelsLabel Empty state title
 * @param noChannelsDescLabel Empty state description
 * @param removeChannelLabel Remove channel action label
 * @param removeChannelTitle Remove channel dialog title
 * @param removeChannelMessage Remove channel dialog message
 * @param cancelLabel Cancel button label
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupScreen(
    group: CustomGroup,
    channels: List<Channel>,
    onNavigateBack: () -> Unit,
    onSaveGroup: (CustomGroup) -> Unit,
    onAddChannels: () -> Unit,
    onRemoveChannel: (Channel) -> Unit,
    onReorderChannels: (List<Channel>) -> Unit,
    editGroupLabel: String = "Edit Group",
    backLabel: String = "Back",
    groupNameLabel: String = "Group Name",
    addChannelsLabel: String = "Add Channels",
    noChannelsLabel: String = "No Channels",
    noChannelsDescLabel: String = "Add channels to this group",
    removeChannelLabel: String = "Remove",
    removeChannelTitle: String = "Remove channel?",
    removeChannelMessage: String = "Remove channel from group",
    cancelLabel: String = "Cancel"
) {
    var groupName by remember { mutableStateOf(group.name) }
    var showError by remember { mutableStateOf(false) }

    val themeColors = getThemeColors()

    DisposableEffect(Unit) {
        onDispose {
            if (groupName.isNotBlank() && groupName.trim() != group.name) {
                val updatedGroup = group.copy(
                    name = groupName.trim(),
                    updatedAt = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                )
                onSaveGroup(updatedGroup)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = editGroupLabel,
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddChannels,
                containerColor = themeColors.primaryColor,
                contentColor = themeColors.textColor
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(addChannelsLabel)
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        showError = false
                    },
                    label = { Text(groupNameLabel) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeColors.primaryColor,
                        unfocusedBorderColor = themeColors.textColor.copy(alpha = 0.2f),
                        focusedTextColor = themeColors.textColor,
                        unfocusedTextColor = themeColors.textColor
                    )
                )
            }

            if (channels.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = themeColors.textColor.copy(alpha = 0.3f)
                            )
                            Text(
                                text = noChannelsLabel,
                                color = themeColors.textColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = noChannelsDescLabel,
                                color = themeColors.textColor.copy(alpha = 0.6f),
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                items(channels, key = { it.id ?: it.streamUrl ?: it.name ?: "" }) { channel ->
                    EditGroupChannelItem(
                        channel = channel,
                        onRemove = { onRemoveChannel(channel) },
                        themeColors = themeColors,
                        removeChannelLabel = removeChannelLabel,
                        removeChannelTitle = removeChannelTitle,
                        removeChannelMessage = removeChannelMessage,
                        cancelLabel = cancelLabel
                    )
                }
            }
        }
    }
}

@Composable
private fun EditGroupChannelItem(
    channel: Channel,
    onRemove: () -> Unit,
    themeColors: ThemeManager.AppTheme,
    removeChannelLabel: String,
    removeChannelTitle: String,
    removeChannelMessage: String,
    cancelLabel: String
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = themeColors.textColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
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
            color = themeColors.textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(themeColors.textColor.copy(alpha = 0.08f), CircleShape)
                .clip(CircleShape)
                .clickable { showRemoveDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = removeChannelLabel,
                tint = themeColors.textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.RemoveCircle,
                    contentDescription = null,
                    tint = themeColors.primaryColor
                )
            },
            title = { Text(removeChannelTitle) },
            text = { Text("$removeChannelMessage: ${channel.name.orEmpty()}") },
            confirmButton = {
                Button(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primaryColor
                    )
                ) {
                    Text(removeChannelLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text(cancelLabel)
                }
            },
            containerColor = themeColors.backgroundSecondary,
            titleContentColor = themeColors.textColor,
            textContentColor = themeColors.textColor
        )
    }
}
