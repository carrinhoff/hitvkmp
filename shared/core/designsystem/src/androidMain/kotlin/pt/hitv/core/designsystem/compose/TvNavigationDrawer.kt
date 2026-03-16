package pt.hitv.core.designsystem.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * TV Navigation Drawer with collapsible states (Android-only).
 *
 * - Collapsed: Shows only icons (~80dp width)
 * - Expanded: Shows icons + labels (~200dp width)
 * - Automatically expands on focus
 * - D-pad controlled
 *
 * @param currentScreen Currently selected screen identifier
 * @param onNavigateToChannels Navigate to channels
 * @param onNavigateToMovies Navigate to movies
 * @param onNavigateToSeries Navigate to series
 * @param onNavigateToPremium Navigate to premium
 * @param onNavigateToSettings Navigate to settings
 * @param hasAnnualOrLifetime Whether user has premium subscription
 * @param liveTvLabel Label for Live TV
 * @param moviesLabel Label for Movies
 * @param tvShowsLabel Label for TV Shows
 * @param premiumLabel Label for Premium
 * @param settingsLabel Label for Settings
 * @param content Main content composable
 */
@Composable
fun HitvTvNavigationDrawer(
    currentScreen: String,
    onNavigateToChannels: () -> Unit,
    onNavigateToMovies: () -> Unit,
    onNavigateToSeries: () -> Unit,
    onNavigateToPremium: () -> Unit,
    onNavigateToSettings: () -> Unit,
    hasAnnualOrLifetime: Boolean = false,
    liveTvLabel: String = "Live TV",
    moviesLabel: String = "Movies",
    tvShowsLabel: String = "TV Shows",
    premiumLabel: String = "Premium",
    settingsLabel: String = "More Options",
    content: @Composable () -> Unit
) {
    val themeColors = getThemeColors()

    var focusedItems by remember { mutableStateOf(setOf<String>()) }
    val isDrawerFocused = focusedItems.isNotEmpty()

    val drawerWidth by animateDpAsState(
        targetValue = if (isDrawerFocused) 200.dp else 80.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "drawerWidth"
    )

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .background(themeColors.backgroundPrimary)
                .padding(vertical = 24.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TvDrawerItem(
                itemId = "channels", selected = currentScreen == "channels",
                onClick = onNavigateToChannels, icon = Icons.Default.Tv,
                label = liveTvLabel, themeColors = themeColors,
                isExpanded = isDrawerFocused,
                onFocusChanged = { focused ->
                    focusedItems = if (focused) focusedItems + "channels" else focusedItems - "channels"
                }
            )

            TvDrawerItem(
                itemId = "movies", selected = currentScreen == "movies",
                onClick = onNavigateToMovies, icon = Icons.Default.Movie,
                label = moviesLabel, themeColors = themeColors,
                isExpanded = isDrawerFocused,
                onFocusChanged = { focused ->
                    focusedItems = if (focused) focusedItems + "movies" else focusedItems - "movies"
                }
            )

            TvDrawerItem(
                itemId = "series", selected = currentScreen == "series",
                onClick = onNavigateToSeries, icon = Icons.Default.VideoLibrary,
                label = tvShowsLabel, themeColors = themeColors,
                isExpanded = isDrawerFocused,
                onFocusChanged = { focused ->
                    focusedItems = if (focused) focusedItems + "series" else focusedItems - "series"
                }
            )

            if (!hasAnnualOrLifetime) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = themeColors.textColor.copy(alpha = 0.2f)
                )

                TvDrawerItem(
                    itemId = "premium", selected = currentScreen == "premium",
                    onClick = onNavigateToPremium, icon = Icons.Default.Star,
                    label = premiumLabel, themeColors = themeColors,
                    isExpanded = isDrawerFocused, isPremium = true,
                    onFocusChanged = { focused ->
                        focusedItems = if (focused) focusedItems + "premium" else focusedItems - "premium"
                    }
                )
            }

            TvDrawerItem(
                itemId = "settings", selected = currentScreen == "settings",
                onClick = onNavigateToSettings, icon = Icons.Default.Settings,
                label = settingsLabel, themeColors = themeColors,
                isExpanded = isDrawerFocused,
                onFocusChanged = { focused ->
                    focusedItems = if (focused) focusedItems + "settings" else focusedItems - "settings"
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun TvDrawerItem(
    itemId: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    themeColors: ThemeManager.AppTheme,
    isExpanded: Boolean,
    isPremium: Boolean = false,
    onFocusChanged: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = tween(180),
        label = "itemScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                color = when {
                    isPremium && isFocused -> themeColors.primaryColor.copy(alpha = 0.4f)
                    isPremium -> themeColors.primaryColor.copy(alpha = 0.2f)
                    isFocused -> themeColors.primaryColor.copy(alpha = 0.3f)
                    selected -> themeColors.primaryColor.copy(alpha = 0.15f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) themeColors.primaryColor else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                        android.view.KeyEvent.KEYCODE_ENTER -> { onClick(); true }
                        else -> false
                    }
                } else false
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPremium) themeColors.primaryColor
                else if (selected || isFocused) themeColors.primaryColor
                else themeColors.textColor,
                modifier = Modifier.size(24.dp)
            )

            if (isExpanded) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isPremium || selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPremium) themeColors.primaryColor
                    else if (selected || isFocused) themeColors.primaryColor
                    else themeColors.textColor
                )
            }
        }
    }
}
