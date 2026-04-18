package pt.hitv.core.navigation.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.adaptive.Orientation
import pt.hitv.core.designsystem.adaptive.rememberOrientation
import pt.hitv.core.designsystem.theme.AppThemeProvider
import pt.hitv.core.model.enums.MainTab
import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import pt.hitv.core.sync.EpgSyncResult
import pt.hitv.core.sync.SyncManager
import pt.hitv.core.sync.SyncManagerImpl
import pt.hitv.core.sync.SyncState
import pt.hitv.core.sync.SyncStateManager
import pt.hitv.core.ui.components.DataPercentageLoader
import pt.hitv.core.ui.components.EpgLoadingToaster
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

/**
 * Adaptive navigation scaffold shared across Android and iOS.
 *
 * Provides:
 * - Bottom navigation bar in portrait mode
 * - Side navigation rail in landscape mode
 * - Tab content with state preservation (all tabs stay composed)
 * - Scroll-to-top on active tab re-selection
 * - Premium tab conditional visibility
 * - Animated nav chrome show/hide
 */
@Composable
fun AdaptiveScaffold(
    isLoggedIn: Boolean,
    onLoginSuccess: () -> Unit = {},
    hasAnnualOrLifetime: Boolean = false,
    modifier: Modifier = Modifier
) {
    AppThemeProvider {
        // If not logged in, show login screen (no bottom nav)
        if (!isLoggedIn) {
            val loginScreen = remember(onLoginSuccess) {
                ScreenRegistry.create(HitvScreen.LOGIN, onLoginSuccess)
            }
            Navigator(loginScreen)
            return@AppThemeProvider
        }

        // === Sync state observation ===
        val syncStateManager: SyncStateManager = koinInject()
        val syncManager: SyncManager = koinInject()
        val preferencesHelper: PreferencesHelper = koinInject()

        val syncState by syncStateManager.syncState.collectAsState()
        val progress by syncStateManager.progress.collectAsState()
        val stageName by syncStateManager.stageName.collectAsState()
        val stageMessage by syncStateManager.stageMessage.collectAsState()
        val epgProgressMessage by syncStateManager.epgProgressMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Surface EPG sync outcomes as a snackbar — the original app shows
        // Toast.makeText on worker finish (worker_epg_synced_successfully /
        // worker_epg_sync_failed). A CMP snackbar matches that on both targets.
        LaunchedEffect(syncStateManager) {
            syncStateManager.epgSyncResult.collect { result ->
                val msg = when (result) {
                    is EpgSyncResult.Success -> "EPG updated successfully"
                    is EpgSyncResult.Failure -> "EPG update failed: ${result.errorMessage ?: "Unknown error"}"
                }
                snackbarHostState.showSnackbar(msg)
            }
        }

        // Track sync completion to force-recreate tab content
        var syncVersion by remember { mutableStateOf(0) }

        // Check if initial sync is needed — set state IMMEDIATELY to prevent blank flash
        val userId = remember { preferencesHelper.getUserId() }
        val initialSyncDone = remember { preferencesHelper.getStoredBoolean("initial_sync_complete") }
        val needsSync = userId != -1 && !initialSyncDone

        // Kick off the post-login sync via the singleton manager. The job
        // lives in a scope owned by SyncStateManager so it SURVIVES Activity
        // recreation (user backgrounding + foregrounding the app). Re-entrant:
        // if a sync is already active, the call is a no-op and the composition
        // just re-binds to the existing state.
        LaunchedEffect(needsSync) {
            if (!needsSync) return@LaunchedEffect
            syncStateManager.startInitialSyncIfNeeded(
                userId = userId,
                syncManager = syncManager,
                preferencesHelper = preferencesHelper,
            )
        }

        // Bump local syncVersion when the shared syncVersion increments —
        // guarantees TabContentHost recreates screens once data is written.
        LaunchedEffect(syncStateManager) {
            syncStateManager.syncVersion.collect { v ->
                if (v > 0) syncVersion = v
            }
        }

        val tabState = remember { TabState() }
        var activeTab by remember { mutableStateOf(MainTab.TV) }

        // Filter tabs based on premium status
        val visibleTabs = remember(hasAnnualOrLifetime) {
            if (hasAnnualOrLifetime) {
                MainTab.entries.filter { it != MainTab.PREMIUM }
            } else {
                MainTab.entries.toList()
            }
        }

        val handleTabSelected: (MainTab) -> Unit = { tab ->
            if (tab == activeTab) {
                // Re-selected active tab -> scroll to top
                tabState.requestScrollToTop(tab)
            } else {
                activeTab = tab
            }
        }

        // Use BoxWithConstraints for cross-platform orientation detection
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = modifier.fillMaxSize()
        ) {
            val orientation = rememberOrientation(
                maxWidth = constraints.maxWidth,
                maxHeight = constraints.maxHeight
            )

            // If syncing, show only the progress overlay (don't compose tabs yet)
            if (syncState == SyncState.SYNCING_DATA) {
                DataPercentageLoader(
                    isVisible = true,
                    percentage = "$progress%",
                    stageTitle = stageName,
                    statusText = stageMessage,
                    onCancelClick = { syncStateManager.onSyncFailed("Cancelled by user") }
                )
                return@BoxWithConstraints
            }

            // Hide nav chrome (bottom bar / side rail) whenever the ACTIVE tab's
            // navigator has pushed a secondary screen on top of the tab root.
            val showNavChrome = tabState.isTabOnRoot(activeTab)

            when (orientation) {
                Orientation.LANDSCAPE -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = showNavChrome,
                            enter = expandHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ),
                            exit = shrinkHorizontally(
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            )
                        ) {
                            MainSideNavigationRail(
                                currentTab = activeTab,
                                visibleTabs = visibleTabs,
                                onTabSelected = handleTabSelected
                            )
                        }

                        TabContentHost(
                            activeTab = activeTab,
                            tabState = tabState,
                            syncVersion = syncVersion,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

                Orientation.PORTRAIT -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TabContentHost(
                            activeTab = activeTab,
                            tabState = tabState,
                            syncVersion = syncVersion,
                            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()
                        )

                        AnimatedVisibility(
                            visible = showNavChrome,
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
                                visibleTabs = visibleTabs,
                                onTabSelected = handleTabSelected
                            )
                        }
                    }
                }
            }

            // EPG sync toaster — mirrors the original EpgLoadingToaster overlay
            // shown while SyncState.SYNCING_EPG is active.
            EpgLoadingToaster(
                isVisible = syncState == SyncState.SYNCING_EPG,
                message = epgProgressMessage,
                titleLabel = "EPG Update"
            )

            // Snackbar host for EPG success/failure feedback.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }
        }
    }
}

/**
 * Hosts all tab content simultaneously, showing only the active tab.
 * Inactive tabs remain composed (preserving state) but are hidden.
 */
@Composable
fun TabContentHost(
    activeTab: MainTab,
    tabState: TabState,
    syncVersion: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.statusBarsPadding()) {
        MainTab.entries.forEach { tab ->
            val isActive = tab == activeTab
            // Keep all tabs composed but hide inactive ones
            Box(
                modifier = if (isActive) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier.size(0.dp).alpha(0f)
                }
            ) {
                // syncVersion as key forces recreation when sync completes (fresh DB data)
                val screen = remember(tab, syncVersion) {
                    val hitvScreen = when (tab) {
                        MainTab.TV -> HitvScreen.CHANNELS
                        MainTab.MOVIES -> HitvScreen.MOVIES
                        MainTab.SERIES -> HitvScreen.SERIES
                        MainTab.PREMIUM -> HitvScreen.PREMIUM_SUBSCRIPTION
                        MainTab.MORE -> HitvScreen.MORE_OPTIONS
                    }
                    ScreenRegistry.create(hitvScreen)
                }

                Navigator(screen) { navigator ->
                    // Report stack size up to TabState so the scaffold can hide
                    // the bottom nav when this tab pushes a secondary screen.
                    LaunchedEffect(navigator) {
                        snapshotFlow { navigator.items.size }.collect { size ->
                            tabState.setNavStackSize(tab, size)
                        }
                    }
                    CurrentScreen()
                }
            }
        }
    }
}

// ===== Navigation Components =====

/**
 * Bottom navigation bar for mobile portrait mode.
 */
@Composable
fun MainBottomNavigation(
    currentTab: MainTab,
    visibleTabs: List<MainTab>,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        visibleTabs.forEach { tab ->
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
fun MainSideNavigationRail(
    currentTab: MainTab,
    visibleTabs: List<MainTab>,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight().navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        visibleTabs.forEach { tab ->
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
