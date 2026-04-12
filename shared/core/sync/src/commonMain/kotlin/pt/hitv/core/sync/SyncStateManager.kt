package pt.hitv.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Observable state holder for sync progress.
 *
 * UI layers (e.g., AdaptiveScaffold) collect these flows to show/hide
 * the DataPercentageLoader overlay. SyncManagerImpl updates them via
 * the [performFullSync] progress callback.
 *
 * Registered as a singleton in Koin so the same instance is shared
 * between the sync layer and the UI layer.
 */
class SyncStateManager {

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _stageName = MutableStateFlow("")
    val stageName: StateFlow<String> = _stageName.asStateFlow()

    private val _stageMessage = MutableStateFlow("")
    val stageMessage: StateFlow<String> = _stageMessage.asStateFlow()

    /**
     * Transitions to [SyncState.SYNCING_DATA] and resets progress counters.
     * Call this before starting the sync chain.
     */
    fun startDataSync() {
        _progress.value = 0
        _stageName.value = ""
        _stageMessage.value = "Preparing..."
        _syncState.value = SyncState.SYNCING_DATA
    }

    /**
     * Updates the current progress values.
     * Called from [SyncManagerImpl.performFullSync]'s onProgress callback.
     */
    fun updateProgress(percent: Int, stage: String, message: String) {
        _progress.value = percent
        _stageName.value = stage
        _stageMessage.value = message
    }

    /**
     * Marks the data sync as complete and returns to [SyncState.IDLE].
     */
    fun onSyncComplete() {
        _progress.value = 100
        _syncState.value = SyncState.IDLE
    }

    /**
     * Marks the sync as failed and returns to [SyncState.IDLE].
     */
    fun onSyncFailed(errorMessage: String? = null) {
        _stageMessage.value = errorMessage ?: "Sync failed"
        _syncState.value = SyncState.IDLE
    }
}
