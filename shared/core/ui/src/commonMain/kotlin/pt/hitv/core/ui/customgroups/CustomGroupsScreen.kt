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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.designsystem.theme.ThemeManager.AppTheme

/**
 * Maps icon name string to Material Icon
 */
internal fun getIconForGroup(iconName: String?): ImageVector {
    return when (iconName) {
        "folder" -> Icons.Rounded.Folder
        "star" -> Icons.Rounded.Star
        "favorite" -> Icons.Rounded.Favorite
        "sports" -> Icons.Rounded.SportsBasketball
        "movie" -> Icons.Rounded.Movie
        "tv" -> Icons.Rounded.Tv
        "music" -> Icons.Rounded.MusicNote
        "news" -> Icons.Rounded.Newspaper
        "play" -> Icons.Rounded.PlayCircle
        else -> Icons.Rounded.Folder
    }
}

/**
 * Screen displaying custom channel groups with create, edit, and delete actions.
 *
 * @param customGroups List of custom groups to display
 * @param onCreateGroup Callback for creating a new group
 * @param onEditGroup Callback when editing a group
 * @param onDeleteGroup Callback when deleting a group
 * @param onNavigateBack Callback for back navigation
 * @param titleLabel Screen title
 * @param backLabel Back button content description
 * @param createGroupLabel Create group button label
 * @param noGroupsLabel Empty state title
 * @param noGroupsDescLabel Empty state description
 * @param deleteGroupTitle Delete dialog title
 * @param deleteGroupMessage Delete dialog message format (receives group name)
 * @param deleteLabel Delete button label
 * @param cancelLabel Cancel button label
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGroupsScreen(
    customGroups: List<CustomGroup>,
    onCreateGroup: () -> Unit,
    onEditGroup: (CustomGroup) -> Unit,
    onDeleteGroup: (CustomGroup) -> Unit,
    onNavigateBack: () -> Unit,
    titleLabel: String = "Custom Groups",
    backLabel: String = "Back",
    createGroupLabel: String = "Create Group",
    noGroupsLabel: String = "No Custom Groups",
    noGroupsDescLabel: String = "Create a group to organize your channels",
    deleteGroupTitle: String = "Delete group?",
    deleteGroupMessage: String = "Are you sure you want to delete",
    deleteLabel: String = "Delete",
    cancelLabel: String = "Cancel"
) {
    val themeColors = getThemeColors()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = titleLabel,
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
                    containerColor = themeColors.backgroundPrimary,
                    titleContentColor = themeColors.textColor,
                    navigationIconContentColor = themeColors.textColor
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateGroup,
                containerColor = themeColors.primaryColor,
                contentColor = themeColors.textColor
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(createGroupLabel)
            }
        },
        containerColor = themeColors.backgroundPrimary
    ) { paddingValues ->
        if (customGroups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = themeColors.textColor.copy(alpha = 0.3f)
                    )
                    Text(
                        text = noGroupsLabel,
                        color = themeColors.textColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = noGroupsDescLabel,
                        color = themeColors.textColor.copy(alpha = 0.6f),
                        fontSize = 15.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customGroups, key = { it.id }) { group ->
                    CustomGroupItem(
                        group = group,
                        onEdit = { onEditGroup(group) },
                        onDelete = { onDeleteGroup(group) },
                        themeColors = themeColors,
                        deleteGroupTitle = deleteGroupTitle,
                        deleteGroupMessage = deleteGroupMessage,
                        deleteLabel = deleteLabel,
                        cancelLabel = cancelLabel
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomGroupItem(
    group: CustomGroup,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    themeColors: AppTheme,
    deleteGroupTitle: String,
    deleteGroupMessage: String,
    deleteLabel: String,
    cancelLabel: String
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                color = themeColors.textColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onEdit)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getIconForGroup(group.icon),
            contentDescription = null,
            tint = themeColors.primaryColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = group.name,
            color = themeColors.textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "${group.channelCount}",
            color = themeColors.textColor.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.widthIn(min = 32.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        ModernIconButton(
            icon = Icons.Rounded.Edit,
            onClick = onEdit,
            themeColors = themeColors
        )

        Spacer(modifier = Modifier.width(4.dp))

        ModernIconButton(
            icon = Icons.Rounded.Delete,
            onClick = { showDeleteDialog = true },
            themeColors = themeColors
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = themeColors.primaryColor
                )
            },
            title = { Text(deleteGroupTitle) },
            text = { Text("$deleteGroupMessage ${group.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primaryColor
                    )
                ) {
                    Text(deleteLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(cancelLabel)
                }
            },
            containerColor = themeColors.backgroundSecondary,
            titleContentColor = themeColors.textColor,
            textContentColor = themeColors.textColor
        )
    }
}

@Composable
private fun ModernIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    themeColors: AppTheme
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(themeColors.textColor.copy(alpha = 0.08f), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = themeColors.textColor.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
