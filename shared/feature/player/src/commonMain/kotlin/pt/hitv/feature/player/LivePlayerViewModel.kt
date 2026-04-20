package pt.hitv.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.media.CatchUpUrlBuilder
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.Channel
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.core.model.UserCredentials

class LivePlayerViewModel(
    private val repository: StreamRepository,
    private val preferencesHelper: PreferencesHelper,
    private val accountManagerRepository: AccountManagerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LivePlayerUiState())
    val uiState: StateFlow<LivePlayerUiState> = _uiState.asStateFlow()

    // One-shot channel for catch-up seek requests. The platform host
    // (ExoPlayer on Android, AVPlayer on iOS) collects this and calls the
    // native seek — a plain StateFlow value can't do that because the same
    // target position must fire the seek every time (e.g. if the user seeks
    // to the same spot twice after playback drifted).
    private val _catchUpSeekRequests = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    val catchUpSeekRequests: SharedFlow<Long> = _catchUpSeekRequests.asSharedFlow()

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

    fun setPlaybackBuffering() { _uiState.update { it.copy(playbackState = LivePlaybackState.Buffering) } }
    fun setPlaybackReady() { _uiState.update { it.copy(playbackState = LivePlaybackState.Playing) } }
    fun setPlaybackError(message: String, retryCount: Int, maxRetries: Int) { _uiState.update { it.copy(playbackState = LivePlaybackState.Error(message = message, isRetrying = false, retryCount = retryCount, maxRetries = maxRetries), showErrorDialog = true, errorMessage = message) } }
    fun setAutoRetrying(retryCount: Int, maxRetries: Int) { _uiState.update { it.copy(playbackState = LivePlaybackState.Error(message = "Stream error - retrying ($retryCount/$maxRetries)...", isRetrying = true, retryCount = retryCount, maxRetries = maxRetries)) } }
    fun dismissErrorDialog() { _uiState.update { it.copy(showErrorDialog = false, errorMessage = "") } }

    fun onChannelSelected(channel: Channel) {
        val hasCatchUp = channel.tvArchive > 0
        _uiState.update {
            it.copy(
                currentChannelName = channel.name ?: "",
                currentChannelUrl = channel.streamUrl ?: "",
                currentChannelObject = channel,
                licenseKey = channel.licenseKey,
                currentChannelEpg = null,
                isChannelListVisible = false,
                isControlsVisible = false,
                playbackState = LivePlaybackState.Buffering,
                selectedCategoryId = channel.categoryId,
                // Leaving catch-up mode for the new channel — reset state, but
                // remember whether the new channel supports catch-up so the
                // EPG overlay can render the extra buttons.
                catchUpState = CatchUpState(channelHasCatchUp = hasCatchUp),
            )
        }
        fetchCurrentEpg(channel, Clock.System.now().toEpochMilliseconds())
        if (hasCatchUp) loadPastPrograms(channel)
    }

    fun toggleControlsVisibility() { _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) } }
    fun setControlsVisible(visible: Boolean) { _uiState.update { it.copy(isControlsVisible = visible) } }
    fun toggleChannelList() { _uiState.update { it.copy(isChannelListVisible = !it.isChannelListVisible) } }
    fun setChannelListVisible(visible: Boolean) { _uiState.update { it.copy(isChannelListVisible = visible) } }
    fun setSelectedCategoryId(id: String?) { _uiState.update { it.copy(selectedCategoryId = id) } }
    fun toggleSleepTimerDialog() { _uiState.update { it.copy(showSleepTimerDialog = !it.showSleepTimerDialog) } }
    fun dismissSleepTimerDialog() { _uiState.update { it.copy(showSleepTimerDialog = false) } }

    fun cycleResizeMode() {
        _uiState.update { it.copy(currentAspectMode = it.currentAspectMode.cycle()) }
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
                isControlsVisible = false, isChannelListVisible = false, playbackState = LivePlaybackState.Buffering,
                currentChannelEpg = if (titleEpg != null || descEpg != null || imgEpg != null) ChannelEpgInfo(channelId = null, channelName = name, programmeTitle = titleEpg, programmeDescription = descEpg, startTime = null, endTime = null, logo = imgEpg) else null
            )
        }
        if (!name.isNullOrBlank()) getChannel(name)
    }

    // ======================= Catch-up (time-shift) =======================
    // Ported from the original hitv LivePlayerViewModel. Analytics calls from
    // the original (`analyticsHelper.logCatchUp*`) are intentionally omitted —
    // Firebase helper isn't wired into the KMP port yet. Add them back when it is.

    /** Loads archived programmes for the given channel into `catchUpState.pastPrograms`. */
    private fun loadPastPrograms(channel: Channel) {
        val epgId = channel.epgChannelId?.trim()?.lowercase()
        if (epgId.isNullOrEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val past = repository.getPastProgramsForChannel(epgId, Int.MAX_VALUE)
                _uiState.update { it.copy(catchUpState = it.catchUpState.copy(pastPrograms = past)) }
            } catch (_: Exception) { /* keep existing list */ }
        }
    }

    /**
     * Switch playback to the start of the currently-airing programme.
     * Only meaningful when the live EPG has a valid start/end — no-op otherwise.
     */
    fun rewindToStart() {
        val state = _uiState.value
        val epg = state.currentChannelEpg ?: return
        val start = epg.startTime ?: return
        val end = epg.endTime ?: return
        switchToCatchUp(start, end, epg.programmeTitle ?: "", epg.programmeDescription ?: "")
    }

    /**
     * Switch playback to the given archived programme. Builds the time-shift
     * URL via [CatchUpUrlBuilder], then updates `currentChannelUrl` so the
     * platform player (ExoPlayer on Android, AVPlayer on iOS) reloads.
     */
    fun switchToCatchUp(
        programStart: Long,
        programEnd: Long,
        programTitle: String,
        programDescription: String = "",
    ) {
        val channel = _uiState.value.currentChannelObject ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val serverTz = repository.fetchAndCacheServerTimezone()
                val catchUpUrl = CatchUpUrlBuilder.buildCatchUpUrl(
                    channel = channel,
                    programStart = programStart,
                    programEnd = programEnd,
                    username = preferencesHelper.getUsername().ifEmpty { null },
                    password = preferencesHelper.getPassword().ifEmpty { null },
                    serverTimezone = serverTz,
                ) ?: return@launch

                _uiState.update {
                    it.copy(
                        currentChannelUrl = catchUpUrl,
                        playbackState = LivePlaybackState.Buffering,
                        catchUpState = it.catchUpState.copy(
                            isActive = true,
                            programStart = programStart,
                            programEnd = programEnd,
                            programTitle = programTitle,
                            programDescription = programDescription,
                            catchUpUrl = catchUpUrl,
                            playbackPositionMs = 0L,
                            playbackDurationMs = 0L,
                        )
                    )
                }
            } catch (_: Exception) { /* leave current state untouched */ }
        }
    }

    /** Exits catch-up playback and returns to the live stream for the current channel. */
    fun backToLive() {
        val channel = _uiState.value.currentChannelObject ?: return
        _uiState.update {
            it.copy(
                currentChannelUrl = channel.streamUrl ?: "",
                playbackState = LivePlaybackState.Buffering,
                catchUpState = it.catchUpState.copy(
                    isActive = false,
                    catchUpUrl = null,
                    playbackSpeed = 1.0f,
                    playbackPositionMs = 0L,
                    playbackDurationMs = 0L,
                    programStart = 0L,
                    programEnd = 0L,
                    programTitle = "",
                    programDescription = "",
                )
            )
        }
        // Re-fetch live EPG now that we're back on the live stream.
        fetchCurrentEpg(channel, Clock.System.now().toEpochMilliseconds())
    }

    /**
     * Jump to the programme after the one currently playing in catch-up.
     * If the next programme has already caught up to live, fall back to `backToLive()`.
     */
    fun navigateNextProgram() {
        val state = _uiState.value
        val current = state.catchUpState
        if (!current.isActive) return
        val past = current.pastPrograms
        // pastPrograms is newest-first, so "next programme" is the one just *before*
        // the current one in the list (later start time).
        val idx = past.indexOfFirst { (it.startTime ?: 0L) == current.programStart }
        val next = if (idx > 0) past[idx - 1] else null
        if (next?.startTime != null && next.endTime != null) {
            if (next.endTime!! >= Clock.System.now().toEpochMilliseconds()) {
                backToLive()
            } else {
                switchToCatchUp(
                    programStart = next.startTime!!,
                    programEnd = next.endTime!!,
                    programTitle = next.programmeTitle ?: "",
                    programDescription = next.programmeDescription ?: "",
                )
            }
        } else {
            backToLive()
        }
    }

    /** Jump to the programme before the one currently playing in catch-up. */
    fun navigatePreviousProgram() {
        val state = _uiState.value
        val current = state.catchUpState
        if (!current.isActive) return
        val past = current.pastPrograms
        val idx = past.indexOfFirst { (it.startTime ?: 0L) == current.programStart }
        val prev = if (idx >= 0 && idx < past.size - 1) past[idx + 1] else null
        if (prev?.startTime != null && prev.endTime != null) {
            switchToCatchUp(
                programStart = prev.startTime!!,
                programEnd = prev.endTime!!,
                programTitle = prev.programmeTitle ?: "",
                programDescription = prev.programmeDescription ?: "",
            )
        }
    }

    fun setCatchUpPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(catchUpState = it.catchUpState.copy(playbackSpeed = speed)) }
    }

    /** Called from the platform player (ExoPlayer/AVPlayer) on each tick while in catch-up. */
    fun updateCatchUpPosition(positionMs: Long, durationMs: Long) {
        _uiState.update {
            it.copy(catchUpState = it.catchUpState.copy(
                playbackPositionMs = positionMs,
                playbackDurationMs = durationMs,
            ))
        }
    }

    /**
     * Triggered by the overlay's seek slider. Fires a seek request for the
     * platform host to pick up, and optimistically updates the stored
     * position so the slider thumb snaps to the release point before the
     * next periodic-time tick arrives.
     */
    fun seekCatchUpTo(positionMs: Long) {
        _uiState.update { it.copy(catchUpState = it.catchUpState.copy(playbackPositionMs = positionMs)) }
        _catchUpSeekRequests.tryEmit(positionMs)
    }
}
