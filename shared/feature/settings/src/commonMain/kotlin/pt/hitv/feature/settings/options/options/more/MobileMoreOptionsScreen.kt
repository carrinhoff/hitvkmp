package pt.hitv.feature.settings.options.options.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwitchAccount
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.hitv.core.common.locale.SupportedLanguage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.settings.options.options.sync.BackgroundSyncSettingsViewModel
import pt.hitv.feature.settings.options.options.sync.CONTENT_INTERVAL_OPTIONS_DAYS
import pt.hitv.feature.settings.options.options.sync.EPG_INTERVAL_OPTIONS_HOURS

@Composable
fun MobileMoreOptionsScreen(
    viewModel: MoreOptionsViewModel,
    syncViewModel: BackgroundSyncSettingsViewModel,
    onSwitchAccountClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onParentalControlClick: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onPlayerEngineClick: () -> Unit,
    onBufferSizeClick: () -> Unit,
    onRefreshDataClick: () -> Unit,
    onTipsAndFeaturesClick: () -> Unit,
    onEpgClick: () -> Unit,
    onFeedbackClick: () -> Unit,
    onDiscordClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = getThemeColors()
    val primaryColor = theme.primaryColor
    val textColor = Color.White
    val textSecondaryColor = Color.White.copy(alpha = 0.7f)

    val uiState by viewModel.uiState.collectAsState()
    val channelPreviewEnabled = uiState.channelPreviewEnabled
    val currentLanguage = uiState.currentLanguage
    val currentAccount = uiState.currentAccountInfo
    val playerEngine by viewModel.playerEngine.collectAsState()
    val liveBufferSize by viewModel.liveBufferSize.collectAsState()
    val syncState by syncViewModel.uiState.collectAsState()

    var backgroundSyncExpanded by remember { mutableStateOf(false) }

    val languageLabel = remember(currentLanguage) {
        if (currentLanguage.isEmpty() || currentLanguage == "system") "System Default"
        else SupportedLanguage.fromCode(currentLanguage)?.nativeName ?: "System Default"
    }
    val playerEngineLabel = if (playerEngine == "vlc") "VLC" else "ExoPlayer"
    val bufferSizeLabel = when (liveBufferSize) {
        "small" -> "Small"
        "large" -> "Large"
        "very_large" -> "Very Large"
        else -> "Medium"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        theme.backgroundPrimary,
                        theme.backgroundSecondary,
                        theme.backgroundPrimary.copy(alpha = 0.9f)
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {
            // Header — root destination, no back button
            Column {
                Text(
                    text = "More Options",
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Additional settings and tools",
                    color = textSecondaryColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Current account card
                item {
                    if (currentAccount != null) {
                        AccountInfoCard(
                            username = currentAccount.first,
                            expirationDate = currentAccount.third,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // ——— General
                sectionLabel("General", primaryColor)

                rows(
                    FeatureItem("app_language", "App Language", languageLabel, Icons.Rounded.Language, onClick = onLanguageClick),
                    FeatureItem("theme_studio", "Theme Studio", "Customize your app's look and feel", Icons.Rounded.Palette, onClick = onThemeClick),
                    FeatureItem("switch_account", "Switch Account", "Choose your IPTV playlist", Icons.Rounded.SwitchAccount, onClick = onSwitchAccountClick, highlight = FeatureHighlight.IMPORTANT),
                    FeatureItem("manage_categories", "Manage Categories", "Pin or hide categories", Icons.Rounded.Category, onClick = onManageCategoriesClick),
                    FeatureItem("parental_control", "Parental Control", "Session Settings", Icons.Rounded.Lock, onClick = onParentalControlClick)
                ) { feature ->
                    FeatureCard(feature, primaryColor, textColor, textSecondaryColor)
                }

                item {
                    ChannelPreviewToggleCard(
                        title = "Preview Mode",
                        description = "Show video preview when viewing channel list",
                        icon = Icons.Rounded.PlayCircle,
                        isEnabled = channelPreviewEnabled,
                        onToggle = { viewModel.updateChannelPreviewEnabled(it) },
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )
                }

                rows(
                    FeatureItem("player_engine", "Player Engine", playerEngineLabel, Icons.Rounded.VideoSettings, onClick = onPlayerEngineClick),
                    FeatureItem("buffer_size", "Live Buffer Size", bufferSizeLabel, Icons.Rounded.Speed, onClick = onBufferSizeClick)
                ) { feature ->
                    FeatureCard(feature, primaryColor, textColor, textSecondaryColor)
                }

                // ——— Data & Sync
                sectionLabel("Data & Sync", primaryColor)

                rows(
                    FeatureItem("epg", "Live with EPG", "Notifications for upcoming programs", Icons.Rounded.LiveTv, onClick = onEpgClick),
                    FeatureItem("refresh_data", "Refresh Data", "Update channels, movies and series", Icons.Rounded.Refresh, onClick = onRefreshDataClick)
                ) { feature ->
                    FeatureCard(feature, primaryColor, textColor, textSecondaryColor)
                }

                // Inline expandable Background Sync card
                item {
                    BackgroundSyncCard(
                        title = "Background Sync",
                        isEnabled = syncState.enabled,
                        epgIntervalHours = syncState.epgIntervalHours,
                        contentIntervalDays = syncState.contentIntervalDays,
                        wifiOnly = syncState.wifiOnly,
                        isExpanded = backgroundSyncExpanded,
                        onExpandToggle = { backgroundSyncExpanded = !backgroundSyncExpanded },
                        onEnabledChange = { syncViewModel.setEnabled(it) },
                        onEpgIntervalChange = { syncViewModel.setEpgIntervalHours(it) },
                        onContentIntervalChange = { syncViewModel.setContentIntervalDays(it) },
                        onWifiOnlyChange = { syncViewModel.setWifiOnly(it) },
                        primaryColor = primaryColor,
                        textColor = textColor,
                        textSecondaryColor = textSecondaryColor
                    )
                }

                // ——— Feedback & Support
                sectionLabel("Feedback & Support", primaryColor)

                rows(
                    FeatureItem("feedback", "Feedback & Support", "Send Feedback", Icons.Rounded.RateReview, onClick = onFeedbackClick),
                    FeatureItem("discord", "Join Discord Community", "Join our Discord for support and updates", Icons.Rounded.Forum, onClick = onDiscordClick),
                    FeatureItem("tips_features", "Tips & Features", "Discover what you can do with HITV", Icons.Rounded.AutoAwesome, onClick = onTipsAndFeaturesClick)
                ) { feature ->
                    FeatureCard(feature, primaryColor, textColor, textSecondaryColor)
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ——— scoped helpers

private fun androidx.compose.foundation.lazy.LazyListScope.sectionLabel(
    title: String,
    primaryColor: Color
) {
    item {
        Text(
            text = title,
            color = primaryColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp)
        )
    }
}

private fun <T> androidx.compose.foundation.lazy.LazyListScope.rows(
    vararg items: T,
    itemContent: @Composable (T) -> Unit
) {
    items.forEach { value ->
        item { itemContent(value) }
    }
}

// ——— Feature card wrapper

@Composable
private fun FeatureCard(
    feature: FeatureItem,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    when (feature.highlight) {
        FeatureHighlight.IMPORTANT -> ImportantFeatureCard(feature, primaryColor, textColor, textSecondaryColor)
        else -> ModernFeatureCard(feature, primaryColor, textColor, textSecondaryColor)
    }
}

@Composable
private fun ModernFeatureCard(
    feature: FeatureItem,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = { feature.onClick?.invoke() }),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(textColor.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
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
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = textColor.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ImportantFeatureCard(
    feature: FeatureItem,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .background(primaryColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = { feature.onClick?.invoke() }),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(feature.icon, null, tint = primaryColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
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
            Icon(
                Icons.Rounded.ChevronRight, null,
                tint = textColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ——— Channel preview toggle

@Composable
private fun ChannelPreviewToggleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Box(
        modifier = Modifier
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(textColor.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, color = textSecondaryColor, fontSize = 12.sp, maxLines = 2)
            }
            Spacer(modifier = Modifier.width(8.dp))
            ToggleSwitch(isEnabled = isEnabled, primaryColor = primaryColor, textColor = textColor)
        }
    }
}

@Composable
private fun ToggleSwitch(
    isEnabled: Boolean,
    primaryColor: Color,
    textColor: Color,
    compact: Boolean = false
) {
    val width = if (compact) 36.dp else 40.dp
    val trackHeight = if (compact) 20.dp else 22.dp
    val thumbSize = if (compact) 16.dp else 18.dp
    Box(
        modifier = Modifier
            .width(width)
            .height(trackHeight)
            .background(
                color = if (isEnabled) primaryColor else textColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(trackHeight / 2)
            ),
        contentAlignment = if (isEnabled) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(thumbSize)
                .background(Color.White, CircleShape)
        )
    }
}

// ——— Inline expandable Background Sync card (mirrors the original)

@Composable
private fun BackgroundSyncCard(
    title: String,
    isEnabled: Boolean,
    epgIntervalHours: Long,
    contentIntervalDays: Long,
    wifiOnly: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onEpgIntervalChange: (Long) -> Unit,
    onContentIntervalChange: (Long) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(textColor.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .border(1.5.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Header row (tap to expand)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(textColor.copy(alpha = 0.08f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Sync, null, tint = textColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = textSecondaryColor,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isEnabled) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF4CAF50), CircleShape)
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = textColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded body
        AnimatedVisibility(visible = isExpanded, enter = fadeIn(), exit = fadeOut()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                HorizontalDivider(
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Master toggle
                SyncToggleRow(
                    title = "Enable Background Sync",
                    description = "Automatically update content while the app is closed",
                    icon = Icons.Rounded.Sync,
                    isChecked = isEnabled,
                    onToggle = onEnabledChange,
                    primaryColor = primaryColor,
                    textColor = textColor,
                    textSecondaryColor = textSecondaryColor
                )

                AnimatedVisibility(visible = isEnabled, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IntervalSelectionRow(
                            title = "EPG Refresh",
                            description = "How often to refresh the program guide",
                            icon = Icons.Rounded.Schedule,
                            currentValue = epgIntervalHours,
                            options = EPG_INTERVAL_OPTIONS_HOURS,
                            labelFormatter = { "${it}h" },
                            onSelect = onEpgIntervalChange,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )

                        IntervalSelectionRow(
                            title = "Content Refresh",
                            description = "How often to refresh channels, movies and series",
                            icon = Icons.Rounded.CloudSync,
                            currentValue = contentIntervalDays,
                            options = CONTENT_INTERVAL_OPTIONS_DAYS,
                            labelFormatter = { if (it == 1L) "1d" else "${it}d" },
                            onSelect = onContentIntervalChange,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor
                        )

                        SyncToggleRow(
                            title = "Wi-Fi Only",
                            description = "Only sync when on Wi-Fi",
                            icon = Icons.Rounded.Wifi,
                            isChecked = wifiOnly,
                            onToggle = onWifiOnlyChange,
                            primaryColor = primaryColor,
                            textColor = textColor,
                            textSecondaryColor = textSecondaryColor,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncToggleRow(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    compact: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle(!isChecked) }
            .padding(if (compact) 12.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = textColor,
                    fontSize = if (compact) 14.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    color = textSecondaryColor,
                    fontSize = if (compact) 12.sp else 14.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            ToggleSwitch(isEnabled = isChecked, primaryColor = primaryColor, textColor = textColor, compact = compact)
        }
    }
}

@Composable
private fun IntervalSelectionRow(
    title: String,
    description: String,
    icon: ImageVector,
    currentValue: Long,
    options: List<Long>,
    labelFormatter: (Long) -> String,
    onSelect: (Long) -> Unit,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = textColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = textSecondaryColor, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val selected = option == currentValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .background(
                            if (selected) primaryColor.copy(alpha = 0.8f) else textColor.copy(alpha = 0.1f),
                            RoundedCornerShape(18.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) primaryColor else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labelFormatter(option),
                        color = if (selected) Color.White else textColor.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ——— Account info card

@Composable
private fun AccountInfoCard(
    username: String,
    expirationDate: String?,
    primaryColor: Color,
    textColor: Color,
    textSecondaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.AccountCircle, null, tint = primaryColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Current Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Person, null, tint = textSecondaryColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = username,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.CalendarToday, null, tint = textSecondaryColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatExpirationDate(expirationDate),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondaryColor
                )
            }
        }
    }
}

private fun formatExpirationDate(raw: String?): String {
    if (raw.isNullOrEmpty() || raw == "0") return "No expiration"
    val seconds = raw.toLongOrNull() ?: return "No expiration"
    val instant = Instant.fromEpochMilliseconds(seconds * 1000L)
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val y = ldt.year.toString()
    val m = ldt.monthNumber.toString().padStart(2, '0')
    val d = ldt.dayOfMonth.toString().padStart(2, '0')
    return "Expires: $y-$m-$d"
}
