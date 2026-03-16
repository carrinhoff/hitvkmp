package pt.hitv.android.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import pt.hitv.core.model.enums.MainTab
import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry

/**
 * Main screen composable providing adaptive navigation chrome.
 *
 * Handles:
 * - Bottom navigation bar for mobile portrait
 * - Side navigation rail for mobile landscape
 * - Voyager Navigator for screen transitions
 *
 * TV navigation drawer support can be added when the TV compose library
 * integration is ready.
 */
@Composable
fun HitvMainScreen(
    modifier: Modifier = Modifier
) {
    // Start with Channels screen (or Login if needed - will be handled by ScreenRegistry)
    val initialScreen = remember {
        ScreenRegistry.create(HitvScreen.CHANNELS)
    }

    Navigator(initialScreen) { navigator ->
        var activeTab by remember { mutableStateOf(MainTab.TV) }

        // Determine if navigation chrome should be visible
        val isNavVisible = remember(navigator.lastItem) {
            val screenKey = navigator.lastItem.key
            !screenKey.contains("Login", ignoreCase = true) &&
                !screenKey.contains("SwitchAccount", ignoreCase = true) &&
                !screenKey.contains("Detail", ignoreCase = true) &&
                !screenKey.contains("Player", ignoreCase = true) &&
                !screenKey.contains("ThemeSettings", ignoreCase = true) &&
                !screenKey.contains("ParentalControl", ignoreCase = true) &&
                !screenKey.contains("ManageCategories", ignoreCase = true) &&
                !screenKey.contains("Feedback", ignoreCase = true) &&
                !screenKey.contains("LiveEpg", ignoreCase = true)
        }

        val handleTabSelected: (MainTab) -> Unit = { tab ->
            if (tab != activeTab) {
                activeTab = tab
                val screen = when (tab) {
                    MainTab.TV -> HitvScreen.CHANNELS
                    MainTab.MOVIES -> HitvScreen.MOVIES
                    MainTab.SERIES -> HitvScreen.SERIES
                    MainTab.PREMIUM -> HitvScreen.PREMIUM_SUBSCRIPTION
                    MainTab.MORE -> HitvScreen.MORE_OPTIONS
                }
                navigator.replaceAll(ScreenRegistry.create(screen))
            }
        }

        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

        if (isLandscape) {
            // Landscape: Row with rail on left, content on right
            Row(modifier = modifier.fillMaxSize()) {
                if (isNavVisible) {
                    MainSideNavigationRail(
                        currentTab = activeTab,
                        onTabSelected = handleTabSelected
                    )
                }

                CurrentScreen()
            }
        } else {
            // Portrait: Column with content on top, bottom bar below
            Column(modifier = modifier.fillMaxSize()) {
                // Content takes remaining space
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    CurrentScreen()
                }

                AnimatedVisibility(
                    visible = isNavVisible,
                    enter = expandVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        expandFrom = Alignment.Bottom
                    ),
                    exit = shrinkVertically(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        shrinkTowards = Alignment.Bottom
                    )
                ) {
                    MainBottomNavigation(
                        currentTab = activeTab,
                        onTabSelected = handleTabSelected
                    )
                }
            }
        }
    }
}

/**
 * Bottom navigation bar for mobile portrait mode.
 * Shows: Channels, Movies, Series, Premium, More
 */
@Composable
private fun MainBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.toIcon(), contentDescription = tab.titleKey) },
                label = { Text(tab.toLabel()) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * Side navigation rail for mobile landscape mode.
 */
@Composable
private fun MainSideNavigationRail(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        MainTab.entries.forEach { tab ->
            NavigationRailItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.toIcon(), contentDescription = tab.titleKey) },
                label = { Text(tab.toLabel()) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}

// ===== Tab -> UI Mapping =====

private fun MainTab.toIcon(): ImageVector = when (this) {
    MainTab.TV -> Icons.Default.LiveTv
    MainTab.MOVIES -> Icons.Default.Movie
    MainTab.SERIES -> Icons.Default.VideoLibrary
    MainTab.PREMIUM -> Icons.Default.Star
    MainTab.MORE -> Icons.Default.MoreHoriz
}

private fun MainTab.toLabel(): String = when (this) {
    MainTab.TV -> "Channels"
    MainTab.MOVIES -> "Movies"
    MainTab.SERIES -> "Series"
    MainTab.PREMIUM -> "Premium"
    MainTab.MORE -> "More"
}
