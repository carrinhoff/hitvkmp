package pt.hitv.core.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Status of a scheduled background sync task.
 *
 * Shared across platforms. Android's WorkManager states and iOS's BGTaskScheduler
 * completions both project onto this enum.
 */
enum class SyncTaskStatus {
    NotScheduled,
    Scheduled,
    Running,
    Succeeded,
    Failed
}

/**
 * Result of a background sync scheduling attempt.
 *
 * Named [BackgroundSyncResult] to avoid collision with the existing [SyncResult]
 * data class in [SyncManagerImpl] (which represents the outcome of a sync run,
 * not of a scheduling request).
 *
 * @param scheduled true if the OS accepted the scheduling request.
 *                  On iOS this means `BGTaskScheduler.submit` did not throw.
 * @param reason    optional human-readable diagnostic (failure cause, or a platform note
 *                  such as "iOS ignores wifiOnly at the API level").
 */
data class BackgroundSyncResult(val scheduled: Boolean, val reason: String? = null)

/**
 * Cross-platform background sync manager.
 *
 * - Android actual uses WorkManager PeriodicWorkRequest + unique work.
 * - iOS actual uses BGTaskScheduler + BGAppRefreshTaskRequest (best-effort OS cadence).
 *
 * Task identifiers are stable string IDs; use [TASK_EPG] and [TASK_CONTENT] for the
 * two built-in sync tasks.
 *
 * For iOS, the corresponding `BGTaskSchedulerPermittedIdentifiers` entries in
 * `Info.plist` MUST match the taskId values passed here, and the BGTask handlers
 * must be registered via `BGTaskScheduler.shared.register(forTaskWithIdentifier:)`
 * from `iOSApp.swift` at launch (before any `submit` call).
 */
expect class BackgroundSyncManager {

    /**
     * Schedule a periodic background task.
     *
     * @param taskId unique task identifier (see [TASK_EPG], [TASK_CONTENT]).
     * @param intervalMs desired cadence. OS may coalesce / extend (especially iOS).
     * @param wifiOnly Android: maps to NetworkType.UNMETERED. iOS: ignored (no API hook).
     * @param requiresCharging Android: setRequiresBatteryNotLow(true). iOS: ignored.
     */
    fun schedulePeriodic(
        taskId: String,
        intervalMs: Long,
        wifiOnly: Boolean,
        requiresCharging: Boolean = false
    ): BackgroundSyncResult

    /**
     * Cancel any scheduled task for [taskId].
     */
    fun cancel(taskId: String)

    /**
     * Trigger a one-shot run of the given task's work.
     *
     * - Android: enqueues a OneTimeWorkRequest for the matching Worker.
     * - iOS: there is no "run BGTask now" API; this calls the Kotlin sync
     *   function directly on a background dispatcher as a best-effort fallback.
     */
    fun runOnce(taskId: String): BackgroundSyncResult

    /**
     * Reactive status map (taskId -> SyncStatus). Updated by:
     * - Android: WorkManager work info observers.
     * - iOS: manually by the BGTask handler via status update helpers.
     */
    val statusFlow: StateFlow<Map<String, SyncTaskStatus>>
}

const val TASK_EPG: String = "pt.hitv.sync.epg"
const val TASK_CONTENT: String = "pt.hitv.sync.content"
