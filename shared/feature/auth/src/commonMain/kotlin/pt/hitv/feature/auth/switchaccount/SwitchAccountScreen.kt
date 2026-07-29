package pt.hitv.feature.auth.switchaccount

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ScreenName
import pt.hitv.core.common.helpers.DateFormatterUtil
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.UserCredentials

/**
 * Mobile Switch Account screen — KMP port of the original
 * `SwitchAccountScreen` + `MobileSwitchAccountLayout` from the Android-only
 * hitv project. TV layout is intentionally not ported (hitv-kmp is mobile-only).
 *
 * Behavior preserved:
 *  - Header with back button + title/subtitle.
 *  - "Add new account" button at top.
 *  - Lazy list of accounts with avatar, username, hostname, expiration row.
 *  - Active account marked, inactive accounts get edit/delete/chevron actions.
 *  - Delete confirmation dialog + loading dialog while deletion runs.
 *  - Edit account dialog (shared with TV in original; here always shown to mobile).
 *  - Auto-switch to next user when active user is deleted; navigate to login when
 *    last account is deleted.
 */
@Composable
fun SwitchAccountScreen(
    viewModel: SwitchAccountViewModel,
    analyticsHelper: AnalyticsHelper,
    onBackPressed: () -> Unit,
    onAddNewAccount: () -> Unit,
    onAccountSwitched: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val users by viewModel.users.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isDeletingUser by viewModel.isDeletingUser.collectAsState()
    val isEditingAccount by viewModel.isEditingAccount.collectAsState()

    LaunchedEffect(Unit) {
        analyticsHelper.logScreenView(ScreenName.SWITCH_ACCOUNT, "SwitchAccountScreen")
    }

    // Edit account dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<UserCredentials?>(null) }
    var editErrorMessage by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialog state
    val showDialog = remember { mutableStateOf(false) }
    val userToDelete = remember { mutableStateOf<UserCredentials?>(null) }

    // Handle delete user events (auto-switch to next user or navigate to login)
    LaunchedEffect(Unit) {
        viewModel.deleteUserEvent.collect { event ->
            when (event) {
                is DeleteUserEvent.SwitchedToUser -> onAccountSwitched()
                is DeleteUserEvent.LastAccountDeleted -> onNavigateToLogin()
            }
        }
    }

    // Handle edit account events
    LaunchedEffect(Unit) {
        viewModel.editAccountEvent.collect { event ->
            when (event) {
                is EditAccountEvent.Success -> {
                    val wasActiveUser = userToEdit?.userId == currentUserId
                    showEditDialog = false
                    userToEdit = null
                    editErrorMessage = null
                    if (wasActiveUser) {
                        onAccountSwitched()
                    }
                }
                is EditAccountEvent.Error -> {
                    editErrorMessage = event.message
                }
            }
        }
    }

    val onEditUser: (UserCredentials) -> Unit = { user ->
        userToEdit = user
        editErrorMessage = null
        showEditDialog = true
    }

    MobileSwitchAccountContent(
        users = users,
        currentUserId = currentUserId,
        isDeletingUser = isDeletingUser,
        onBackPressed = onBackPressed,
        onUserSelected = { user ->
            analyticsHelper.logSwitchAccount(user.userId.toString(), user.hostname)
            analyticsHelper.setUserId(null)
            viewModel.switchUser(user)
            analyticsHelper.setUserId(user.userId.toString())
            onAccountSwitched()
        },
        onAddNewAccount = {
            analyticsHelper.logAddAccountClicked()
            onAddNewAccount()
        },
        onUserDeleted = { user ->
            analyticsHelper.logDeleteAccountIntent(user.userId.toString(), user.hostname)
            userToDelete.value = user
            showDialog.value = true
        },
        onUserEdit = onEditUser,
        showDialog = showDialog.value,
        onDismissDialog = { showDialog.value = false },
        onConfirmDelete = {
            userToDelete.value?.let { user ->
                analyticsHelper.logDeleteAccountConfirmed(user.userId.toString())
                viewModel.deleteUser(user)
                showDialog.value = false
                userToDelete.value = null
            }
        },
        userToDelete = userToDelete.value
    )

    if (showEditDialog && userToEdit != null) {
        EditAccountDialog(
            user = userToEdit!!,
            isLoading = isEditingAccount,
            errorMessage = editErrorMessage,
            onSave = { username, password, hostname, epgUrl ->
                viewModel.editAccount(
                    userId = userToEdit!!.userId,
                    username = username,
                    password = password,
                    hostname = hostname,
                    epgUrl = epgUrl
                )
            },
            onDismiss = {
                showEditDialog = false
                userToEdit = null
                editErrorMessage = null
            }
        )
    }
}

@Composable
private fun MobileSwitchAccountContent(
    users: List<UserCredentials>,
    currentUserId: Int,
    isDeletingUser: Boolean,
    onBackPressed: () -> Unit,
    onUserSelected: (UserCredentials) -> Unit,
    onAddNewAccount: () -> Unit,
    onUserDeleted: (UserCredentials) -> Unit,
    onUserEdit: (UserCredentials) -> Unit,
    showDialog: Boolean,
    onDismissDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
    userToDelete: UserCredentials?
) {
    val themeColors = getThemeColors()
    val textSecondaryColor = themeColors.textColor.copy(alpha = 0.6f)

    var isVisible by remember { mutableStateOf(false) }
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, delayMillis = 100),
        label = "contentFade"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        themeColors.backgroundPrimary,
                        themeColors.backgroundSecondary,
                        themeColors.backgroundPrimary.copy(alpha = 0.9f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .alpha(contentAlpha)
        ) {
            MobileSwitchAccountHeader(
                onNavigateBack = onBackPressed,
                textColor = themeColors.textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            MobileAddAccountButton(
                onClick = onAddNewAccount,
                textColor = themeColors.textColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (users.isEmpty()) {
                EmptyAccountsState(
                    textColor = themeColors.textColor,
                    textSecondaryColor = textSecondaryColor
                )
            } else {
                MobileUserList(
                    users = users,
                    currentUserId = currentUserId,
                    onUserSelected = onUserSelected,
                    onUserDeleted = onUserDeleted,
                    onUserEdit = onUserEdit,
                    primaryColor = themeColors.primaryColor,
                    textColor = themeColors.textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }
        }
    }

    if (showDialog && userToDelete != null) {
        MobileConfirmationDialog(
            onDismissRequest = onDismissDialog,
            onConfirmation = onConfirmDelete,
            dialogTitle = "Delete Account",
            dialogText = "Are you sure you want to delete the account for ${userToDelete.hostname}?"
        )
    }

    if (isDeletingUser) {
        Dialog(
            onDismissRequest = { /* Prevent dismissal during deletion */ },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.backgroundSecondary
                ),
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = themeColors.primaryColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = "Deleting account…",
                            color = themeColors.textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Loading…",
                            color = themeColors.textColor.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSwitchAccountHeader(
    onNavigateBack: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = textColor.copy(alpha = 0.08f),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = textColor.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(20.dp))

        Column {
            Text(
                text = "Switch Account",
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Choose your IPTV playlist",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.sp
            )
        }
    }
}

@Composable
private fun MobileAddAccountButton(
    onClick: () -> Unit,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(textColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = textColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Add new account",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyAccountsState(
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.3f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No accounts found",
                color = textSecondaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add an account to get started",
                color = textSecondaryColor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MobileUserList(
    users: List<UserCredentials>,
    currentUserId: Int,
    onUserSelected: (UserCredentials) -> Unit,
    onUserDeleted: (UserCredentials) -> Unit,
    onUserEdit: (UserCredentials) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(users) { user ->
            MobileUserCard(
                user = user,
                isCurrentUser = currentUserId == user.userId,
                onUserSelected = onUserSelected,
                onDeleteUser = onUserDeleted,
                onEditUser = onUserEdit,
                primaryColor = primaryColor,
                textColor = textColor,
                textSecondaryColor = textSecondaryColor
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MobileUserCard(
    user: UserCredentials,
    isCurrentUser: Boolean,
    onUserSelected: (UserCredentials) -> Unit,
    onDeleteUser: (UserCredentials) -> Unit,
    onEditUser: (UserCredentials) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    val backgroundColor = when {
        isCurrentUser -> primaryColor.copy(alpha = 0.12f)
        else -> textColor.copy(alpha = 0.05f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isCurrentUser) { onUserSelected(user) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isCurrentUser) primaryColor else textColor.copy(alpha = 0.15f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.username,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.hostname,
                fontSize = 13.sp,
                color = textSecondaryColor,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))
            ExpirationDateText(
                expirationDate = user.expirationDate,
                textColor = textColor,
                primaryColor = primaryColor
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentUser) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Active",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
            }

            MobileActionButton(
                icon = Icons.Rounded.Edit,
                onClick = { onEditUser(user) },
                textColor = textColor,
                contentDescription = "Edit account"
            )

            if (!isCurrentUser) {
                MobileActionButton(
                    icon = Icons.Rounded.Delete,
                    onClick = { onDeleteUser(user) },
                    textColor = textColor,
                    contentDescription = "Delete account"
                )

                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MobileActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    textColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(textColor.copy(alpha = 0.08f), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = textColor.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MobileConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String
) {
    val themeColors = getThemeColors()
    val textSecondaryColor = themeColors.textColor.copy(alpha = 0.7f)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = dialogTitle,
                color = themeColors.textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = dialogText,
                color = textSecondaryColor,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmation,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.primaryColor
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = themeColors.textColor.copy(alpha = 0.7f)
                )
            ) {
                Text("Cancel")
            }
        },
        containerColor = themeColors.backgroundSecondary,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun ExpirationDateText(
    expirationDate: String?,
    textColor: Color,
    primaryColor: Color
) {
    val expirationInfo = remember(expirationDate) {
        if (expirationDate.isNullOrEmpty() || expirationDate == "0") {
            null
        } else {
            try {
                val timestampSeconds = expirationDate.toLong()
                val formattedDate = DateFormatterUtil.formatExpirationDate(timestampSeconds)
                if (formattedDate != null) {
                    Triple(
                        formattedDate,
                        DateFormatterUtil.isExpired(timestampSeconds),
                        DateFormatterUtil.isExpiringSoon(timestampSeconds, 7)
                    )
                } else {
                    null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }

    if (expirationInfo == null) {
        Text(
            text = "No expiration",
            fontSize = 12.sp,
            color = textColor.copy(alpha = 0.5f),
            fontWeight = FontWeight.Normal
        )
    } else {
        val (formattedDate, isExpired, isExpiringSoon) = expirationInfo

        val displayColor = when {
            isExpired -> Color.Red
            isExpiringSoon -> Color(0xFFFFA500)
            else -> primaryColor.copy(alpha = 0.7f)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.DateRange,
                contentDescription = null,
                tint = displayColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Expires on $formattedDate",
                fontSize = 12.sp,
                color = displayColor,
                fontWeight = if (isExpired || isExpiringSoon) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
