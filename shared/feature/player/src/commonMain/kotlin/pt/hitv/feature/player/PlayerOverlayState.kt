package pt.hitv.feature.player

/**
 * Shared overlay UI state for player screens.
 */
data class PlayerOverlayState(
    val isVisible: Boolean = false,
    val showControls: Boolean = false,
    val showEpg: Boolean = false,
    val showChannelList: Boolean = false,
    val showSleepTimer: Boolean = false
)
