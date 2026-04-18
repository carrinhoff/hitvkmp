package pt.hitv.core.sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGAppRefreshTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import pt.hitv.core.common.PreferencesHelper

/**
 * iOS implementation of [BackgroundSyncManager] backed by `BGTaskScheduler` +
 * `BGAppRefreshTaskRequest`.
 *
 * ## Lifecycle
 *
 * The BGTask identifiers in [TASK_EPG] / [TASK_CONTENT] MUST be:
 *   1. listed under `BGTaskSchedulerPermittedIdentifiers` in `Info.plist`, AND
 *   2. registered via `BGTaskScheduler.shared.register(forTaskWithIdentifier:using:launchHandler:)`
 *      from `iOSApp.swift.init()` BEFORE the first `submit` call.
 *
 * Failing either condition makes `submitTaskRequest` throw `NSInternalInconsistencyException`.
 *
 * ## API divergences from Android
 *
 * - `wifiOnly` / `requiresCharging` are ignored at the BGTaskScheduler API level
 *   (iOS does not expose network-type or charging constraints for BGAppRefreshTask).
 *   The [BackgroundSyncResult.reason] will carry a hint when these are requested.
 * - `runOnce` has no "run this BGTask now" API on iOS. We fall back to invoking the
 *   Kotlin sync function directly on a background dispatcher. This is best-effort —
 *   the process may be suspended mid-way if the app is backgrounded without an active
 *   BGTask extending the window. For reliable scheduled runs, rely on [schedulePeriodic].
 */
@OptIn(ExperimentalForeignApi::class)
actual class BackgroundSyncManager(
    private val syncManager: SyncManager,
    private val preferencesHelper: PreferencesHelper
) {

    private val statusMap = MutableStateFlow<Map<String, SyncTaskStatus>>(emptyMap())
    actual val statusFlow: StateFlow<Map<String, SyncTaskStatus>> = statusMap.asStateFlow()

    private val runOnceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual fun schedulePeriodic(
        taskId: String,
        intervalMs: Long,
        wifiOnly: Boolean,
        requiresCharging: Boolean
    ): BackgroundSyncResult {
        val request = BGAppRefreshTaskRequest(identifier = taskId)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
            intervalMs / 1000.0
        )

        return try {
            // K/N maps -[BGTaskScheduler submitTaskRequest:error:] as
            // `submitTaskRequest(taskRequest, error: ...)` with a nullable error-out pointer.
            BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            updateStatus(taskId, SyncTaskStatus.Scheduled)
            val note = when {
                wifiOnly && requiresCharging ->
                    "iOS: wifiOnly and requiresCharging are not enforced at the BGTask API level."
                wifiOnly ->
                    "iOS: wifiOnly is not enforced at the BGTask API level."
                requiresCharging ->
                    "iOS: requiresCharging is not enforced at the BGTask API level."
                else -> null
            }
            BackgroundSyncResult(scheduled = true, reason = note)
        } catch (e: Throwable) {
            updateStatus(taskId, SyncTaskStatus.Failed)
            BackgroundSyncResult(scheduled = false, reason = e.message)
        }
    }

    actual fun cancel(taskId: String) {
        BGTaskScheduler.sharedScheduler.cancelTaskRequestWithIdentifier(taskId)
        updateStatus(taskId, SyncTaskStatus.NotScheduled)
    }

    /**
     * Best-effort direct run. iOS has no "run-BGTask-now" API; we invoke the sync
     * function directly on a background dispatcher.
     */
    actual fun runOnce(taskId: String): BackgroundSyncResult {
        updateStatus(taskId, SyncTaskStatus.Running)
        runOnceScope.launch {
            val userId = preferencesHelper.getUserId()
            val result = when (taskId) {
                TASK_EPG -> syncManager.syncEpg(userId)
                TASK_CONTENT -> {
                    val impl = syncManager as? SyncManagerImpl
                    if (impl != null) {
                        impl.performFullSync(userId) { _, _, _ -> }
                    } else {
                        syncManager.syncChannels(userId)
                    }
                }
                else -> null
            }
            val status = when {
                result == null -> SyncTaskStatus.Failed
                result.isSuccess -> SyncTaskStatus.Succeeded
                else -> SyncTaskStatus.Failed
            }
            updateStatus(taskId, status)
        }
        return BackgroundSyncResult(scheduled = true)
    }

    /**
     * Called by the BGTask handler (via [SyncBridge] in the umbrella module) to
     * reflect task completion into [statusFlow].
     */
    fun reportStatus(taskId: String, status: SyncTaskStatus) {
        updateStatus(taskId, status)
    }

    private fun updateStatus(taskId: String, status: SyncTaskStatus) {
        statusMap.value = statusMap.value.toMutableMap().apply { put(taskId, status) }
    }

}
