package pt.hitv.core.sync

import pt.hitv.core.common.PreferencesHelper

// Preference keys for the background-sync settings. They live here, next to the scheduler, rather
// than in the settings feature, so the boot path can read them without depending on the UI module.
const val PREF_BG_SYNC_ENABLED: String = "bg_sync_enabled"
const val PREF_BG_SYNC_EPG_INTERVAL_HOURS: String = "bg_sync_epg_interval_hours"
const val PREF_BG_SYNC_CONTENT_INTERVAL_DAYS: String = "bg_sync_content_interval_days"
const val PREF_BG_SYNC_WIFI_ONLY: String = "bg_sync_wifi_only"

const val DEFAULT_BG_SYNC_EPG_INTERVAL_HOURS: Long = 12L
const val DEFAULT_BG_SYNC_CONTENT_INTERVAL_DAYS: Long = 1L

private const val HOUR_MS: Long = 60L * 60L * 1000L
private const val DAY_MS: Long = 24L * HOUR_MS

/**
 * Re-arms the OS-level periodic sync tasks at app start, when the user has background sync on.
 *
 * ## Why this exists
 *
 * The re-arm used to live in `BackgroundSyncSettingsViewModel.init`, and that ViewModel is only
 * constructed when the user opens **More Options**. So the sequence "enable background sync →
 * quit the app → reopen it and go straight to watching TV" left the tasks un-submitted, on both
 * platforms, indefinitely. The user had turned the feature on, the settings screen would still
 * show it as on, and nothing was scheduled.
 *
 * It matters more on iOS: a `BGAppRefreshTaskRequest` is one-shot. The Swift launch handler chains
 * the next one after each firing, but that chain only exists once something submits the first
 * request — so a missed re-arm means background sync silently stops forever rather than merely
 * being delayed. Android's WorkManager keeps unique periodic work across restarts, so there it is
 * closer to belt-and-braces.
 *
 * Deliberately does **not** change the default: background sync stays off until the user enables
 * it. This only ensures that an already-expressed preference is actually honoured.
 */
class BackgroundSyncBootstrapper(
    private val preferencesHelper: PreferencesHelper,
    private val backgroundSyncManager: BackgroundSyncManager,
) {

    /**
     * Submits both periodic tasks if the user has sync enabled; a no-op otherwise.
     *
     * Safe to call on every launch — scheduling is idempotent on both platforms (WorkManager
     * replaces unique work; BGTaskScheduler replaces a pending request with the same identifier).
     */
    fun reArmIfEnabled(): Boolean {
        val requests = computeSyncSchedule(
            enabled = preferencesHelper.getStoredBoolean(PREF_BG_SYNC_ENABLED, false),
            storedEpgIntervalHours = preferencesHelper.getStoredLongTag(PREF_BG_SYNC_EPG_INTERVAL_HOURS),
            storedContentIntervalDays = preferencesHelper.getStoredLongTag(PREF_BG_SYNC_CONTENT_INTERVAL_DAYS),
            wifiOnly = preferencesHelper.getStoredBoolean(PREF_BG_SYNC_WIFI_ONLY, true),
        )
        requests.forEach {
            backgroundSyncManager.schedulePeriodic(
                taskId = it.taskId,
                intervalMs = it.intervalMs,
                wifiOnly = it.wifiOnly,
                requiresCharging = false,
            )
        }
        return requests.isNotEmpty()
    }
}

/** One periodic task to submit. */
data class SyncScheduleRequest(
    val taskId: String,
    val intervalMs: Long,
    val wifiOnly: Boolean,
)

/**
 * Decides what to schedule from the stored preferences — pure, so the decision can be tested
 * without an OS scheduler. `BackgroundSyncManager` is an `expect class` and `PreferencesHelper` is
 * concrete, so neither can be substituted in `commonTest`; keeping the logic here rather than
 * inline in [BackgroundSyncBootstrapper.reArmIfEnabled] is what makes it reachable at all.
 *
 * Interval values of 0 or less mean "never written", and fall back to the defaults.
 *
 * @return the requests to submit, or empty when the user has background sync switched off.
 */
internal fun computeSyncSchedule(
    enabled: Boolean,
    storedEpgIntervalHours: Long,
    storedContentIntervalDays: Long,
    wifiOnly: Boolean,
): List<SyncScheduleRequest> {
    if (!enabled) return emptyList()

    val epgHours = storedEpgIntervalHours.takeIf { it > 0 } ?: DEFAULT_BG_SYNC_EPG_INTERVAL_HOURS
    val contentDays =
        storedContentIntervalDays.takeIf { it > 0 } ?: DEFAULT_BG_SYNC_CONTENT_INTERVAL_DAYS

    return listOf(
        SyncScheduleRequest(TASK_EPG, epgHours * HOUR_MS, wifiOnly),
        SyncScheduleRequest(TASK_CONTENT, contentDays * DAY_MS, wifiOnly),
    )
}
