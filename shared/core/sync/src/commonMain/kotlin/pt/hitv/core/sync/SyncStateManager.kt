package pt.hitv.core.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SyncStateManager {

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _syncVersion = MutableStateFlow(0)
    val syncVersion: StateFlow<Int> = _syncVersion.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _stageName = MutableStateFlow("")
    val stageName: StateFlow<String> = _stageName.asStateFlow()

    private val _stageMessage = MutableStateFlow("")
    val stageMessage: StateFlow<String> = _stageMessage.asStateFlow()

    fun startDataSync() {
        _progress.value = 0
        _stageName.value = ""
        _stageMessage.value = "Preparing..."
        _syncState.value = SyncState.SYNCING_DATA
    }

    fun updateProgress(percent: Int, stage: String, message: String) {
        _progress.value = percent
        _stageName.value = stage
        _stageMessage.value = message
    }

    fun onSyncComplete() {
        _progress.value = 100
        _syncState.value = SyncState.IDLE
        _syncVersion.value++
    }

    fun onSyncFailed(errorMessage: String? = null) {
        _stageMessage.value = errorMessage ?: "Sync failed"
        _syncState.value = SyncState.IDLE
    }

    fun startEpgSync() {
        _stageName.value = "EPG"
        _stageMessage.value = "Fetching program guide..."
        _syncState.value = SyncState.SYNCING_EPG
    }

    fun onEpgSyncComplete() {
        _syncState.value = SyncState.IDLE
    }
}
