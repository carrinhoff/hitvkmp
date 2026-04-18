package pt.hitv.feature.settings.options.options.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.BackgroundSyncResult
import pt.hitv.core.sync.TASK_CONTENT
import pt.hitv.core.sync.TASK_EPG

/**
 * PreferencesHelper keys matching the original hitv project. Kept as constants so
 * migrations/read-throughs elsewhere (e.g. the inline BackgroundSyncCard in
 * [MobileMoreOptionsScreen]) can reuse the same keys.
 */
const val PREF_BG_SYNC_ENABLED: String = "bg_sync_enabled"
const val PREF_BG_SYNC_EPG_INTERVAL_HOURS: String = "bg_sync_epg_interval_hours"
const val PREF_BG_SYNC_CONTENT_INTERVAL_DAYS: String = "bg_sync_content_interval_days"
const val PREF_BG_SYNC_WIFI_ONLY: String = "bg_sync_wifi_only"

private const val DEFAULT_EPG_INTERVAL_HOURS: Long = 12L
private const val DEFAULT_CONTENT_INTERVAL_DAYS: Long = 1L
private const val HOUR_MS: Long = 60L * 60L * 1000L
private const val DAY_MS: Long = 24L * HOUR_MS

/**
 * Allowed EPG refresh intervals (hours), matching the original Android UI.
 */
val EPG_INTERVAL_OPTIONS_HOURS: List<Long> = listOf(6L, 12L, 24L)

/**
 * Allowed content refresh intervals (days), matching the original Android UI.
 */
val CONTENT_INTERVAL_OPTIONS_DAYS: List<Long> = listOf(1L, 3L, 7L)

data class BackgroundSyncSettingsUiState(
    val enabled: Boolean = false,
    val epgIntervalHours: Long = DEFAULT_EPG_INTERVAL_HOURS,
    val contentIntervalDays: Long = DEFAULT_CONTENT_INTERVAL_DAYS,
    val wifiOnly: Boolean = true
)

/**
 * ViewModel for the Background Sync settings screen.
 *
 * Every UI change writes through [PreferencesHelper] and then either reschedules
 * both tasks via [BackgroundSyncManager.schedulePeriodic] or cancels them when
 * the master toggle is off. The viewmodel never queues work directly — it
 * delegates entirely to Team α's [BackgroundSyncManager] so Android (WorkManager)
 * and iOS (BGTaskScheduler) stay in sync with the same logic.
 */
class BackgroundSyncSettingsViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val backgroundSyncManager: BackgroundSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<BackgroundSyncSettingsUiState> = _uiState.asStateFlow()

    /**
     * Exposes Team α's sync status map (taskId -> status) unchanged so the UI
     * can render per-task indicators.
     */
    val syncStatusFlow = backgroundSyncManager.statusFlow

    init {
        // If the user already enabled sync in a prior session, make sure the
        // OS tasks are queued after process restart. Android WorkManager keeps
        // unique periodic work across restarts, but the iOS actual has to
        // re-submit BGTasks after launch.
        if (_uiState.value.enabled) {
            applySchedule(_uiState.value)
        }
    }

    private fun loadInitialState(): BackgroundSyncSettingsUiState {
        val enabled = preferencesHelper.getStoredBoolean(PREF_BG_SYNC_ENABLED, false)
        val epg = preferencesHelper.getStoredLongTag(PREF_BG_SYNC_EPG_INTERVAL_HOURS)
            .takeIf { it > 0 } ?: DEFAULT_EPG_INTERVAL_HOURS
        val content = preferencesHelper.getStoredLongTag(PREF_BG_SYNC_CONTENT_INTERVAL_DAYS)
            .takeIf { it > 0 } ?: DEFAULT_CONTENT_INTERVAL_DAYS
        val wifiOnly = preferencesHelper.getStoredBoolean(PREF_BG_SYNC_WIFI_ONLY, true)
        return BackgroundSyncSettingsUiState(
            enabled = enabled,
            epgIntervalHours = epg,
            contentIntervalDays = content,
            wifiOnly = wifiOnly
        )
    }

    fun setEnabled(enabled: Boolean) {
        preferencesHelper.setStoredBoolean(PREF_BG_SYNC_ENABLED, enabled)
        _uiState.update { it.copy(enabled = enabled) }
        if (enabled) {
            applySchedule(_uiState.value)
        } else {
            backgroundSyncManager.cancel(TASK_EPG)
            backgroundSyncManager.cancel(TASK_CONTENT)
        }
    }

    fun setEpgIntervalHours(hours: Long) {
        if (hours !in EPG_INTERVAL_OPTIONS_HOURS) return
        preferencesHelper.setStoredLongTag(PREF_BG_SYNC_EPG_INTERVAL_HOURS, hours)
        _uiState.update { it.copy(epgIntervalHours = hours) }
        if (_uiState.value.enabled) {
            scheduleEpg(_uiState.value)
        }
    }

    fun setContentIntervalDays(days: Long) {
        if (days !in CONTENT_INTERVAL_OPTIONS_DAYS) return
        preferencesHelper.setStoredLongTag(PREF_BG_SYNC_CONTENT_INTERVAL_DAYS, days)
        _uiState.update { it.copy(contentIntervalDays = days) }
        if (_uiState.value.enabled) {
            scheduleContent(_uiState.value)
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        preferencesHelper.setStoredBoolean(PREF_BG_SYNC_WIFI_ONLY, wifiOnly)
        _uiState.update { it.copy(wifiOnly = wifiOnly) }
        if (_uiState.value.enabled) {
            applySchedule(_uiState.value)
        }
    }

    /**
     * Fires both syncs once. Returns the pair of results so the UI can surface
     * errors (e.g. scheduling rejected).
     */
    fun runNow(onResult: (Pair<BackgroundSyncResult, BackgroundSyncResult>) -> Unit = {}) {
        viewModelScope.launch {
            val epg = backgroundSyncManager.runOnce(TASK_EPG)
            val content = backgroundSyncManager.runOnce(TASK_CONTENT)
            onResult(epg to content)
        }
    }

    private fun applySchedule(state: BackgroundSyncSettingsUiState) {
        scheduleEpg(state)
        scheduleContent(state)
    }

    private fun scheduleEpg(state: BackgroundSyncSettingsUiState) {
        backgroundSyncManager.schedulePeriodic(
            taskId = TASK_EPG,
            intervalMs = state.epgIntervalHours * HOUR_MS,
            wifiOnly = state.wifiOnly,
            requiresCharging = false
        )
    }

    private fun scheduleContent(state: BackgroundSyncSettingsUiState) {
        backgroundSyncManager.schedulePeriodic(
            taskId = TASK_CONTENT,
            intervalMs = state.contentIntervalDays * DAY_MS,
            wifiOnly = state.wifiOnly,
            requiresCharging = false
        )
    }
}
