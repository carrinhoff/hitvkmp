package pt.hitv.core.ui.customgroups

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Dialog for creating a new custom channel group.
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onCreate Callback with group name and optional icon name
 * @param titleLabel Dialog title
 * @param closeLabel Close button content description
 * @param groupNameLabel Label for group name field
 * @param groupNamePlaceholder Placeholder for group name field
 * @param groupNameError Error when name is empty
 * @param selectIconLabel Label for icon selection section
 * @param cancelLabel Cancel button label
 * @param createLabel Create button label
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String?) -> Unit,
    titleLabel: String = "Create Custom Group",
    closeLabel: String = "Close",
    groupNameLabel: String = "Group Name",
    groupNamePlaceholder: String = "e.g., Sports, News, Kids",
    groupNameError: String = "Please enter a group name",
    selectIconLabel: String = "Select Icon (Optional)",
    cancelLabel: String = "Cancel",
    createLabel: String = "Create"
) {
    var groupName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }

    val themeColors = getThemeColors()

    val availableIcons = listOf(
        "folder" to Icons.Rounded.Folder,
        "star" to Icons.Rounded.Star,
        "favorite" to Icons.Rounded.Favorite,
        "sports" to Icons.Rounded.SportsBasketball,
        "movie" to Icons.Rounded.Movie,
        "tv" to Icons.Rounded.Tv,
        "music" to Icons.Rounded.MusicNote,
        "news" to Icons.Rounded.Newspaper,
        "play" to Icons.Rounded.PlayCircle
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = themeColors.backgroundSecondary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = titleLabel,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textColor,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = themeColors.textColor.copy(alpha = 0.08f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = closeLabel,
                            tint = themeColors.textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = groupName,
                    onValueChange = {
                        groupName = it
                        showError = false
                    },
                    label = { Text(groupNameLabel) },
                    placeholder = { Text(groupNamePlaceholder) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(groupNameError) }
                    } else null,
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

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = selectIconLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = themeColors.textColor.copy(alpha = 0.6f)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(availableIcons) { (iconName, icon) ->
                            val isSelected = selectedIcon == iconName

                            val backgroundColor by animateColorAsState(
                                targetValue = when {
                                    isSelected -> themeColors.primaryColor.copy(alpha = 0.2f)
                                    else -> themeColors.textColor.copy(alpha = 0.08f)
                                },
                                label = "iconBackground"
                            )

                            val iconTint by animateColorAsState(
                                targetValue = when {
                                    isSelected -> themeColors.primaryColor
                                    else -> themeColors.textColor.copy(alpha = 0.6f)
                                },
                                label = "iconTint"
                            )

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        color = backgroundColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedIcon = if (selectedIcon == iconName) null else iconName
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = iconName,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(cancelLabel, color = themeColors.textColor.copy(alpha = 0.7f))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (groupName.isBlank()) {
                                showError = true
                            } else {
                                onCreate(groupName.trim(), selectedIcon)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryColor,
                            contentColor = themeColors.textColor
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(createLabel)
                    }
                }
            }
        }
    }
}
