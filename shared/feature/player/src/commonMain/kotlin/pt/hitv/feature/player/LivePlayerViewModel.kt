package pt.hitv.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.Channel
import pt.hitv.core.model.UserCredentials
import pt.hitv.core.common.PreferencesHelper

class LivePlayerViewModel(
    private val repository: StreamRepository,
    private val preferencesHelper: PreferencesHelper,
    private val accountManagerRepository: AccountManagerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): Int = preferencesHelper.getUserId()

    fun setSelectedPosition(position: String) { _uiState.update { it.copy(selectedPosition = position) } }

    fun fetchChannelsFromDB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllChannelsFlow().catch { _uiState.update { it.copy(cachedChannels = null) } }
                    .collect { channelList -> _uiState.update { it.copy(cachedChannels = channelList) } }
            } catch (_: Exception) { _uiState.update { it.copy(cachedChannels = null) } }
        }
    }

    fun fetchCurrentEpg(channel: Channel, currentTimeMillis: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentChannelEpg = null) }
            try {
                val epgData = repository.fetchCurrentEpg(channel, currentTimeMillis)
                _uiState.update { it.copy(currentChannelEpg = epgData) }
            } catch (_: Exception) { _uiState.update { it.copy(currentChannelEpg = null) } }
        }
    }

    suspend fun fetchCurrentEpgSuspend(channel: Channel, currentTimeMillis: Long): ChannelEpgInfo? {
        return try { repository.fetchCurrentEpg(channel, currentTimeMillis) } catch (_: Exception) { null }
    }

    fun getFavorites() {
        viewModelScope.launch {
            repository.getFavoritesChannel().catch { }.collect { channels -> _uiState.update { it.copy(favorites = channels) } }
        }
    }

    fun saveFavoriteChannel(channel: Channel) {
        viewModelScope.launch { try { repository.saveFavoriteChannel(channel); getFavorites() } catch (_: Exception) {} }
    }

    fun saveRecentlyViewedChannel(channel: Channel, currentTimeMillis: Long) {
        viewModelScope.launch { try { repository.saveRecentlyViewedChannel(channel.copy(lastViewedTimestamp = currentTimeMillis)) } catch (_: Exception) {} }
    }

    suspend fun getCredentialsByUserId(userId: Int): UserCredentials? {
        return try { accountManagerRepository.getCredentialsByUserId(userId) } catch (_: Exception) { null }
    }

    fun isFavorite(channel: Channel): Boolean {
        return _uiState.value.favorites.any { fav ->
            if (fav.id != null && channel.id != null) fav.id == channel.id else fav.name == channel.name && fav.categoryId == channel.categoryId
        }
    }

    fun calculateEpgTime(startTime: Long?, endTime: Long?, currentTime: Long): Int {
        if (startTime == null || endTime == null || startTime >= endTime || currentTime < startTime) return 0
        val totalDuration = (endTime - startTime).toDouble()
        val timePassed = (currentTime - startTime).toDouble()
        return ((timePassed / totalDuration) * 100).coerceIn(0.0, 100.0).toInt()
    }

    fun getChannel(name: String) {
        viewModelScope.launch { try { _uiState.update { it.copy(fetchedChannel = repository.getChannel(name)) } } catch (_: Exception) { _uiState.update { it.copy(fetchedChannel = null) } } }
    }

    fun fetchChannelCategories() {
        viewModelScope.launch {
            try {
                repository.getAllChannelCategories(getCurrentUserId()).catch { _uiState.update { it.copy(categories = emptyList()) } }
                    .collect { categories -> _uiState.update { it.copy(categories = categories) } }
            } catch (_: Exception) { _uiState.update { it.copy(categories = emptyList()) } }
        }
    }

    fun setPlaybackBuffering() { _uiState.update { it.copy(playbackState = PlaybackState.Buffering) } }
    fun setPlaybackReady() { _uiState.update { it.copy(playbackState = PlaybackState.Playing) } }
    fun setPlaybackError(message: String, retryCount: Int, maxRetries: Int) { _uiState.update { it.copy(playbackState = PlaybackState.Error(message = message, isRetrying = false, retryCount = retryCount, maxRetries = maxRetries), showErrorDialog = true, errorMessage = message) } }
    fun setAutoRetrying(retryCount: Int, maxRetries: Int) { _uiState.update { it.copy(playbackState = PlaybackState.Error(message = "Stream error - retrying ($retryCount/$maxRetries)...", isRetrying = true, retryCount = retryCount, maxRetries = maxRetries)) } }
    fun dismissErrorDialog() { _uiState.update { it.copy(showErrorDialog = false, errorMessage = "") } }

    fun onChannelSelected(channel: Channel) {
        _uiState.update {
            it.copy(currentChannelName = channel.name ?: "", currentChannelUrl = channel.streamUrl ?: "", currentChannelObject = channel, licenseKey = channel.licenseKey, currentChannelEpg = null, isChannelListVisible = false, isControlsVisible = false, playbackState = PlaybackState.Buffering, selectedCategoryId = channel.categoryId)
        }
        fetchCurrentEpg(channel, Clock.System.now().toEpochMilliseconds())
    }

    fun toggleControlsVisibility() { _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) } }
    fun setControlsVisible(visible: Boolean) { _uiState.update { it.copy(isControlsVisible = visible) } }
    fun toggleChannelList() { _uiState.update { it.copy(isChannelListVisible = !it.isChannelListVisible) } }
    fun setChannelListVisible(visible: Boolean) { _uiState.update { it.copy(isChannelListVisible = visible) } }
    fun setSelectedCategoryId(id: String?) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun toggleSleepTimerDialog() { _uiState.update { it.copy(showSleepTimerDialog = !it.showSleepTimerDialog) } }
    fun dismissSleepTimerDialog() { _uiState.update { it.copy(showSleepTimerDialog = false) } }

    fun cycleResizeMode() {
        _uiState.update {
            val nextMode = when (it.currentResizeMode) {
                LivePlayerUiState.RESIZE_MODE_FIT -> LivePlayerUiState.RESIZE_MODE_FILL
                LivePlayerUiState.RESIZE_MODE_FILL -> LivePlayerUiState.RESIZE_MODE_ZOOM
                else -> LivePlayerUiState.RESIZE_MODE_FIT
            }
            it.copy(currentResizeMode = nextMode)
        }
    }

    fun initFromArgs(url: String, name: String, position: String, categoryTitle: String?, categoryId: Int, licenseKey: String?, isPiPSupported: Boolean, isTvDevice: Boolean) {
        _uiState.update { it.copy(currentChannelUrl = url, currentChannelName = name, selectedPosition = position, currentCategoryTitle = categoryTitle, currentCategoryId = categoryId, licenseKey = licenseKey, isPiPSupported = isPiPSupported, isTvDevice = isTvDevice, selectedCategoryId = null) }
        if (name.isNotEmpty()) getChannel(name)
    }

    fun handleNewIntent(url: String?, name: String?, position: String?, categoryTitle: String?, categoryId: Int, licenseKey: String?, titleEpg: String?, descEpg: String?, imgEpg: String?) {
        _uiState.update {
            it.copy(
                currentChannelUrl = url?.trim() ?: it.currentChannelUrl, currentChannelName = if (!name.isNullOrBlank()) name else it.currentChannelName,
                licenseKey = licenseKey ?: it.licenseKey, currentCategoryTitle = categoryTitle ?: it.currentCategoryTitle,
                currentCategoryId = if (categoryId != -1) categoryId else it.currentCategoryId, selectedPosition = position ?: it.selectedPosition,
                isControlsVisible = false, isChannelListVisible = false, playbackState = PlaybackState.Buffering,
                currentChannelEpg = if (titleEpg != null || descEpg != null || imgEpg != null) ChannelEpgInfo(channelId = null, channelName = name, programmeTitle = titleEpg, programmeDescription = descEpg, startTime = null, endTime = null, logo = imgEpg) else null
            )
        }
        if (!name.isNullOrBlank()) getChannel(name)
    }
}
