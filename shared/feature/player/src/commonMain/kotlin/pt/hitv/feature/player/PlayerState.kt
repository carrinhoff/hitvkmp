package pt.hitv.feature.player

import pt.hitv.core.model.Category
import pt.hitv.core.model.Channel
import pt.hitv.core.model.ChannelEpgInfo

/**
 * Sealed interface representing the playback state of the LIVE channel player.
 * (Separate from [PlaybackState] enum used by the generic movie/series player.)
 */
sealed interface LivePlaybackState {
    data object Idle : LivePlaybackState
    data object Buffering : LivePlaybackState
    data object Playing : LivePlaybackState
    data class Error(
        val message: String,
        val isRetrying: Boolean = false,
        val retryCount: Int = 0,
        val maxRetries: Int = 3
    ) : LivePlaybackState
}

/**
 * UI state for the live channel player.
 */
data class LivePlayerUiState(
    val cachedChannels: List<Channel>? = null,
    val currentChannelEpg: ChannelEpgInfo? = null,
    val favorites: List<Channel> = emptyList(),
    val categories: List<Category> = emptyList(),
    val fetchedChannel: Channel? = null,
    val selectedPosition: String = "",
    val playbackState: LivePlaybackState = LivePlaybackState.Idle,
    val currentChannelUrl: String = "",
    val currentChannelName: String = "",
    val currentChannelObject: Channel? = null,
    val licenseKey: String? = null,
    val isControlsVisible: Boolean = false,
    val isChannelListVisible: Boolean = false,
    val selectedCategoryId: String? = null,
    val showErrorDialog: Boolean = false,
    val errorMessage: String = "",
    val showSleepTimerDialog: Boolean = false,
    val currentAspectMode: PlayerAspectMode = PlayerAspectMode.Fit,
    val isPiPSupported: Boolean = false,
    val isPiPModeEnabled: Boolean = true,
    val isTvDevice: Boolean = false,
    val currentCategoryTitle: String? = null,
    val currentCategoryId: Int = -1
)
