package pt.hitv.core.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.hitv.core.common.PreferencesHelper

sealed class EpgSyncResult {
    object Success : EpgSyncResult()
    data class Failure(val errorMessage: String?) : EpgSyncResult()
}

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

    // Separate channel for the EPG toaster subtitle so it doesn't fight with
    // the main data-sync stage message. Matches the original's epg_worker_*
    // messages funneled through SharedViewModel.updateEpgProgress.
    private val _epgProgressMessage = MutableStateFlow("")
    val epgProgressMessage: StateFlow<String> = _epgProgressMessage.asStateFlow()

    // One-shot result events so AdaptiveScaffold can fire the completion
    // snackbar matching the original's Toast.makeText on EPG success/failure.
    private val _epgSyncResult = MutableSharedFlow<EpgSyncResult>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val epgSyncResult: SharedFlow<EpgSyncResult> = _epgSyncResult.asSharedFlow()

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
        _epgProgressMessage.value = "Updating EPG…"
        _syncState.value = SyncState.SYNCING_EPG
    }

    fun updateEpgProgress(message: String) {
        _epgProgressMessage.value = message
    }

    fun onEpgSyncComplete() {
        _syncState.value = SyncState.IDLE
        _epgProgressMessage.value = ""
        _epgSyncResult.tryEmit(EpgSyncResult.Success)
    }

    fun onEpgSyncFailed(errorMessage: String? = null) {
        _syncState.value = SyncState.IDLE
        _epgProgressMessage.value = ""
        _epgSyncResult.tryEmit(EpgSyncResult.Failure(errorMessage))
    }

    // === Long-lived sync execution ===
    // The initial post-login sync runs in a scope owned by the singleton
    // manager, not in a Compose LaunchedEffect. That way, when the Activity
    // is destroyed + recreated (user backgrounds + foregrounds the app),
    // the in-flight coroutine keeps running and the UI just re-binds to the
    // existing state — no "Preparing..." restart.

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var runningJob: Job? = null

    fun isSyncRunning(): Boolean = runningJob?.isActive == true

    /**
     * Starts the full post-login sync (data + EPG) if not already running.
     * Re-entrant — calling from a re-composed LaunchedEffect is a no-op if a
     * previous job is still active.
     */
    fun startInitialSyncIfNeeded(
        userId: Int,
        syncManager: SyncManager,
        preferencesHelper: PreferencesHelper,
    ) {
        if (runningJob?.isActive == true) return
        runningJob = scope.launch {
            startDataSync()
            try {
                (syncManager as SyncManagerImpl).performFullSync(userId) { p, s, m ->
                    updateProgress(p, s, m)
                }
                preferencesHelper.setStoredBoolean("initial_sync_complete", true)
                onSyncComplete()

                startEpgSync()
                try {
                    val epgResult = syncManager.syncEpg(userId)
                    if (epgResult.isSuccess) onEpgSyncComplete()
                    else onEpgSyncFailed(epgResult.errorMessage)
                } catch (e: Exception) {
                    onEpgSyncFailed(e.message)
                }
            } catch (e: Exception) {
                onSyncFailed(e.message)
            }
        }
    }
}
