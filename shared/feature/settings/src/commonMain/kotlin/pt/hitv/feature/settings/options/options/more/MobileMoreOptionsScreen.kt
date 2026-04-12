package pt.hitv.feature.settings.options.options.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import pt.hitv.core.common.helpers.DateFormatterUtil
import pt.hitv.core.designsystem.theme.getThemeColors

@Composable
fun MobileMoreOptionsScreen(
    viewModel: MoreOptionsViewModel,
    onRefreshDataClick: () -> Unit,
    onEpgClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onThemeClick: () -> Unit,
    onParentalControlClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onSwitchAccountClick: () -> Unit,
    onDiscordClick: () -> Unit = {},
    onTipsAndFeaturesClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onPlayerEngineClick: () -> Unit = {},
    onBufferSizeClick: () -> Unit = {},
    hasAnnualOrLifetimePremium: Boolean = false,
    onPremiumClick: () -> Unit = {},
    backgroundSyncEnabled: Boolean = false,
    onBackgroundSyncToggle: (Boolean) -> Unit = {},
    epgIntervalHours: Long = 12,
    contentIntervalHours: Long = 24,
    wifiOnlySync: Boolean = true,
    onEpgIntervalChange: (Long) -> Unit = {},
    onContentIntervalChange: (Long) -> Unit = {},
    onWifiOnlyToggle: (Boolean) -> Unit = {},
    lastEpgSync: String? = null,
    lastContentSync: String? = null,
    scrollToTopSignal: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeColors = getThemeColors()
    val backgroundColor = themeColors.backgroundPrimary
    val primaryColor = themeColors.primaryColor
    val secondaryBackgroundColor = themeColors.backgroundSecondary
    val textColor = Color.White
    val textSecondaryColor = Color.White.copy(alpha = 0.7f)

    val listState = rememberLazyListState()

    // Scroll to top when signal changes
    var lastScrollToTopSignal by remember { mutableStateOf(0) }
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0 && scrollToTopSignal != lastScrollToTopSignal) {
            lastScrollToTopSignal = scrollToTopSignal
            listState.animateScrollToItem(index = 0, scrollOffset = 0)
        }
    }

    // Background sync expand state
    var backgroundSyncExpanded by remember { mutableStateOf(false) }

    // Language display
    val languageLabel = when (uiState.currentLanguage) {
        "en" -> "English"
        "pt" -> "Portuguese"
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "system", "" -> "System Default"
        else -> uiState.currentLanguage.ifEmpty { "System Default" }
    }

    // Build feature groups matching original Android structure
    val featureGroups = remember(
        uiState.channelPreviewEnabled, languageLabel,
        hasAnnualOrLifetimePremium, backgroundSyncExpanded
    ) {
        buildList {
            // General group
            add(
                FeatureGroup(
                    title = "General",
                    items = buildList {
                        // App Language
                        add(
                            FeatureItem(
                                id = "app_language",
                                title = "App Language",
                                description = languageLabel,
                                icon = Icons.Rounded.Language,
                                onClick = onLanguageClick
                            )
                        )

                        // Theme Studio
                        add(
                            FeatureItem(
                                id = "theme_studio",
                                title = "Theme Studio",
                                description = "Customize app appearance",
                                icon = Icons.Rounded.Palette,
                                onClick = onThemeClick
                            )
                        )

                        // Switch Account (IMPORTANT highlight)
                        add(
                            FeatureItem(
                                id = "switch_account",
                                title = "Switch Account",
                                description = "Choose your IPTV playlist",
                                icon = Icons.Rounded.SwitchAccount,
                                onClick = onSwitchAccountClick,
                                highlight = FeatureHighlight.IMPORTANT
                            )
                        )

                        // Manage Categories
                        add(
                            FeatureItem(
                                id = "manage_categories",
                                title = "Manage Categories",
                                description = "Pin or hide categories",
                                icon = Icons.Rounded.Category,
                                onClick = onManageCategoriesClick
                            )
                        )

                        // Parental Controls
                        add(
                            FeatureItem(
                                id = "parental_control",
                                title = "Parental Controls",
                                description = "Session settings",
                                icon = Icons.Rounded.Lock,
                                onClick = onParentalControlClick,
                                requiresPremium = true,
                                highlight = if (!hasAnnualOrLifetimePremium) FeatureHighlight.PREMIUM else FeatureHighlight.NONE
                            )
                        )

                        // Channel Preview Toggle
                        add(
                            FeatureItem(
                                id = "channel_preview",
                                title = "Channel Preview",
                                description = "Show channel preview on selection",
                                icon = Icons.Rounded.PlayCircle,
                                isToggle = true
                            )
                        )

                        // Player Engine
                        add(
                            FeatureItem(
                                id = "player_engine",
                                title = "Player Engine",
                                description = "ExoPlayer (Default)",
                                icon = Icons.Rounded.VideoSettings,
                                onClick = onPlayerEngineClick
                            )
                        )

                        // Live Buffer Size
                        add(
                            FeatureItem(
                                id = "buffer_size",
                                title = "Live Buffer Size",
                                description = "Medium (Default)",
                                icon = Icons.Rounded.Speed,
                                onClick = onBufferSizeClick
                            )
                        )
                    }
                )
            )

            // Data & Sync group
            add(
                FeatureGroup(
                    title = "Data & Sync",
                    items = buildList {
                        // Live EPG
                        add(
                            FeatureItem(
                                id = "epg",
                                title = "Live EPG",
                                description = "Notifications for future programs",
                                icon = Icons.Rounded.LiveTv,
                                onClick = onEpgClick
                            )
                        )

                        // Refresh Data
                        add(
                            FeatureItem(
                                id = "refresh_data",
                                title = "Refresh Data",
                                description = "Update channels, movies & series",
                                icon = Icons.Rounded.Refresh,
                                onClick = onRefreshDataClick
                            )
                        )

                        // Background Sync (toggle with expandable sub-settings)
                        add(
                            FeatureItem(
                                id = "background_sync",
                                title = "Background Sync",
                                description = "Background Sync",
                                icon = Icons.Rounded.Sync,
                                isToggle = true,
                                isExpanded = backgroundSyncExpanded
                            )
                        )
                    }
                )
            )

            // Feedback & Support group
            add(
                FeatureGroup(
                    title = "Feedback & Support",
                    items = buildList {
                        // Feedback & Support (IMPORTANT highlight)
                        add(
                            FeatureItem(
                                id = "feedback",
                                title = "Feedback & Support",
                                description = "Send Feedback",
                                icon = Icons.Rounded.RateReview,
                                onClick = onFeedbackClick,
                                highlight = FeatureHighlight.IMPORTANT
                            )
                        )

                        // Join Discord Community
                        add(
                            FeatureItem(
                                id = "discord",
                                title = "Join Discord Community",
                                description = "Join for support and community",
                                icon = Icons.Rounded.Forum,
                                onClick = onDiscordClick
                            )
                        )

                        // Tips & Features
                        add(
                            FeatureItem(
                                id = "tips_features",
                                title = "Tips & Features",
                                description = "Discover what you can do",
                                icon = Icons.Rounded.AutoAwesome,
                                onClick = onTipsAndFeaturesClick
                            )
                        )
                    }
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        secondaryBackgroundColor,
                        backgroundColor.copy(alpha = 0.9f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // Header: "More Options" title + subtitle
            MoreOptionsHeader(
                textColor = textColor,
                textSecondaryColor = textSecondaryColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Account Info Card
                item {
                    val accountInfo = uiState.currentAccountInfo
                    accountInfo?.let { (username, _, expirationDate) ->
                        AccountInfoCard(
                            username = username,
                            expirationDate = expirationDate,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                featureGroups.forEach { group ->
                    if (group.items.isNotEmpty()) {
                        // Section header
                        item {
                            Text(
                                text = group.title,
                                color = primaryColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(group.items) { feature ->
                            when (feature.id) {
                                "channel_preview" -> {
                                    ChannelPreviewToggleCard(
                                        feature = feature,
                                        isEnabled = uiState.channelPreviewEnabled,
                                        onToggle = { enabled ->
                                            viewModel.updateChannelPreviewEnabled(enabled)
                                        },
                                        primaryColor = primaryColor,
                                        textColor = textColor,
                                        textSecondaryColor = textSecondaryColor
                                    )
                                }
                                "background_sync" -> {
                                    BackgroundSyncCard(
                                        feature = feature,
                                        backgroundSyncEnabled = backgroundSyncEnabled,
                                        isExpanded = backgroundSyncExpanded,
                                        onExpandToggle = {
                                            backgroundSyncExpanded = !backgroundSyncExpanded
                                        },
                                        onBackgroundSyncToggle = onBackgroundSyncToggle,
                                        epgIntervalHours = epgIntervalHours,
                                        contentIntervalHours = contentIntervalHours,
                                        wifiOnlySync = wifiOnlySync,
                                        onEpgIntervalChange = onEpgIntervalChange,
                                        onContentIntervalChange = onContentIntervalChange,
                                        onWifiOnlyToggle = onWifiOnlyToggle,
                                        lastEpgSync = lastEpgSync,
                                        lastContentSync = lastContentSync,
                                        primaryColor = primaryColor,
                                        textColor = textColor,
                                        textSecondaryColor = textSecondaryColor
                                    )
                                }
                                else -> {
                                    when (feature.highlight) {
                                        FeatureHighlight.IMPORTANT -> {
                                            ImportantFeatureCard(
                                                feature = feature,
                                                primaryColor = primaryColor,
                                                textColor = textColor,
                                                textSecondaryColor = textSecondaryColor
                                            )
                                        }
                                        else -> {
                                            ModernFeatureCard(
                                                feature = feature,
                                                primaryColor = primaryColor,
                                                textColor = textColor,
                                                textSecondaryColor = textSecondaryColor,
                                                hasAnnualOrLifetimePremium = hasAnnualOrLifetimePremium,
                                                onPremiumClick = onPremiumClick
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Header
// ---------------------------------------------------------------------------

@Composable
private fun MoreOptionsHeader(
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "More Options",
            color = textColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = "Settings and additional tools",
            color = textSecondaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Account Info Card
// ---------------------------------------------------------------------------

@Composable
private fun AccountInfoCard(
    username: String,
    expirationDate: String?,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    // Parse expiration date from epoch seconds
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
            } catch (_: NumberFormatException) {
                null
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = primaryColor.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: "Current Account"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Username row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = textSecondaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expiration row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (expirationInfo == null) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No expiration",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = textSecondaryColor
                    )
                } else {
                    val (formattedDate, isExpired, isExpiringSoon) = expirationInfo
                    val themeColors = getThemeColors()

                    val displayColor = when {
                        isExpired -> Color.Red
                        isExpiringSoon -> themeColors.warning
                        else -> textSecondaryColor
                    }

                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = displayColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Expires: $formattedDate",
                        fontSize = 13.sp,
                        fontWeight = if (isExpired || isExpiringSoon) FontWeight.SemiBold else FontWeight.Normal,
                        color = displayColor
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Feature Card (standard item with icon + title + subtitle + chevron)
// Matches original: Box with border, CircleShape icon bg, 1dp border
// ---------------------------------------------------------------------------

@Composable
private fun ModernFeatureCard(
    feature: FeatureItem,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier,
    hasAnnualOrLifetimePremium: Boolean = false,
    onPremiumClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = {
                if (feature.requiresPremium && !hasAnnualOrLifetimePremium) {
                    onPremiumClick()
                } else {
                    feature.onClick?.invoke()
                }
            }),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        textColor.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = feature.title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    // Premium badge
                    if (feature.requiresPremium && !hasAnnualOrLifetimePremium) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = primaryColor,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PREMIUM",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (feature.description != feature.title) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = feature.description,
                        color = textSecondaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }

            // Chevron
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Important Feature Card (filled primary circle icon background)
// Matches original: primaryColor tinted bg, primaryColor border, CircleShape icon
// ---------------------------------------------------------------------------

@Composable
private fun ImportantFeatureCard(
    feature: FeatureItem,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = feature.onClick ?: {}),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in filled primary color circle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        primaryColor.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.description,
                    color = textSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Background Sync Card (expandable toggle with sub-settings)
// Matches original: expand/collapse with green dot status indicator
// ---------------------------------------------------------------------------

@Composable
private fun BackgroundSyncCard(
    feature: FeatureItem,
    backgroundSyncEnabled: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onBackgroundSyncToggle: (Boolean) -> Unit,
    epgIntervalHours: Long,
    contentIntervalHours: Long,
    wifiOnlySync: Boolean,
    onEpgIntervalChange: (Long) -> Unit,
    onContentIntervalChange: (Long) -> Unit,
    onWifiOnlyToggle: (Boolean) -> Unit,
    lastEpgSync: String?,
    lastContentSync: String?,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = textColor.copy(alpha = 0.1f),
        label = "cardBorder"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clickable(onClick = onExpandToggle),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            textColor.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = feature.title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = feature.description,
                            color = textSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1
                        )
                        // Green dot status indicator when sync is enabled
                        if (backgroundSyncEnabled) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Green, CircleShape)
                            )
                        }
                    }
                }

                // Expand/collapse indicator
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded content
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Column(
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            ) {
                HorizontalDivider(
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 1. Enable Background Sync Toggle
                SyncSettingRow(
                    icon = Icons.Rounded.Sync,
                    title = "Enable Background Sync",
                    subtitle = "Sync content automatically in the background",
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    trailing = {
                        SyncToggle(
                            checked = backgroundSyncEnabled,
                            primaryColor = primaryColor,
                            textColor = textColor
                        )
                    },
                    onClick = { onBackgroundSyncToggle(!backgroundSyncEnabled) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. EPG Refresh Interval
                SyncSettingRow(
                    icon = Icons.Rounded.LiveTv,
                    title = "EPG Refresh Interval",
                    subtitle = "How often the program guide updates",
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IntervalChip(
                        label = "6h",
                        selected = epgIntervalHours == 6L,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        onClick = { onEpgIntervalChange(6L) }
                    )
                    IntervalChip(
                        label = "12h",
                        selected = epgIntervalHours == 12L,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        onClick = { onEpgIntervalChange(12L) }
                    )
                    IntervalChip(
                        label = "24h",
                        selected = epgIntervalHours == 24L,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        onClick = { onEpgIntervalChange(24L) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Content Refresh Interval
                SyncSettingRow(
                    icon = Icons.Rounded.Cloud,
                    title = "Content Refresh Interval",
                    subtitle = "How often channels, movies and series update",
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IntervalChip(
                        label = "24h",
                        selected = contentIntervalHours == 24L,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        onClick = { onContentIntervalChange(24L) }
                    )
                    IntervalChip(
                        label = "48h",
                        selected = contentIntervalHours == 48L,
                        primaryColor = primaryColor,
                        textColor = textColor,
                        onClick = { onContentIntervalChange(48L) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. WiFi Only Toggle
                SyncSettingRow(
                    icon = Icons.Rounded.Wifi,
                    title = "WiFi Only",
                    subtitle = "Only sync on WiFi connections",
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor,
                    trailing = {
                        SyncToggle(
                            checked = wifiOnlySync,
                            primaryColor = primaryColor,
                            textColor = textColor
                        )
                    },
                    onClick = { onWifiOnlyToggle(!wifiOnlySync) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Sync Status Info Card
                SyncStatusCard(
                    lastEpgSync = lastEpgSync,
                    lastContentSync = lastContentSync,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sync sub-components
// ---------------------------------------------------------------------------

@Composable
private fun SyncSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = textSecondaryColor,
                fontSize = 12.sp
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
private fun SyncToggle(
    checked: Boolean,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(22.dp)
            .background(
                color = if (checked) primaryColor else textColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(11.dp)
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(18.dp)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun IntervalChip(
    label: String,
    selected: Boolean,
    primaryColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) primaryColor else textColor.copy(alpha = 0.08f)
    val contentColor = if (selected) Color.White else textColor.copy(alpha = 0.7f)
    val borderMod = if (selected) {
        Modifier
    } else {
        Modifier.border(1.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
    }

    Box(
        modifier = modifier
            .then(borderMod)
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun SyncStatusCard(
    lastEpgSync: String?,
    lastContentSync: String?,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sync Status",
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        SyncStatusRow(
            label = "Last EPG Sync",
            value = lastEpgSync ?: "Never",
            textColor = textColor,
            textSecondaryColor = textSecondaryColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        SyncStatusRow(
            label = "Last Content Sync",
            value = lastContentSync ?: "Never",
            textColor = textColor,
            textSecondaryColor = textSecondaryColor
        )
    }
}

@Composable
private fun SyncStatusRow(
    label: String,
    value: String,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = textSecondaryColor,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------------------------------------------------------------------------
// Channel Preview Toggle Card
// Matches original: same layout as ModernFeatureCard but with Switch instead of chevron
// ---------------------------------------------------------------------------

@Composable
private fun ChannelPreviewToggleCard(
    feature: FeatureItem,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = { onToggle(!isEnabled) }),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in circle background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        textColor.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = feature.description,
                    color = textSecondaryColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Custom toggle switch
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(22.dp)
                    .background(
                        color = if (isEnabled) primaryColor else textColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(18.dp)
                        .background(
                            color = Color.White,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
