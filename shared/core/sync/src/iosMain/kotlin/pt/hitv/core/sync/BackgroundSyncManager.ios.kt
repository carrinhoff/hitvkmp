package pt.hitv.core.sync

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import platform.BackgroundTasks.BGProcessingTaskRequest
import platform.BackgroundTasks.BGTaskScheduler
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.withSyncKeepalive

/**
 * iOS implementation of [BackgroundSyncManager] backed by `BGTaskScheduler` +
 * `BGProcessingTaskRequest`.
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
 * ## Why `BGProcessingTaskRequest` and not `BGAppRefreshTaskRequest`
 *
 * The port submitted app-refresh requests, which was wrong on three counts:
 *
 *  - **Duration.** An app-refresh task gets roughly 30 seconds. Android runs these as
 *    `PeriodicWorkRequest`s, and a full content sync over tens of thousands of channels does not
 *    finish in 30s — the expiration handler would fire and mark it failed, every time. A
 *    processing task gets minutes.
 *  - **Constraints.** Android sets `NetworkType.CONNECTED` on both workers.
 *    `BGAppRefreshTaskRequest` cannot express that at all, so the port silently dropped the only
 *    constraint the original has. `BGProcessingTaskRequest` carries `requiresNetworkConnectivity`
 *    and `requiresExternalPower`, so both Android constraints now survive the port.
 *  - **Declared intent.** `Info.plist` already listed `processing` under `UIBackgroundModes` and
 *    permitted both identifiers; only the request type was never wired to match.
 *
 * The cadences suit it: EPG every 6h, content every 24h, matching Android's periodic workers.
 *
 * **Trade-off, stated plainly:** iOS schedules processing tasks more conservatively than
 * app-refresh ones, favouring idle and charging periods. For 6h/24h cadences that is appropriate —
 * and Doze does the same thing to WorkManager on Android — but it does mean a sync is more likely
 * to land overnight than exactly on the interval boundary. Reverting is a one-line change of the
 * request type here and in `iOSApp.swift`.
 *
 * ## API divergences from Android
 *
 * - `wifiOnly` still cannot be expressed: `requiresNetworkConnectivity` is a boolean and does not
 *   distinguish Wi-Fi from cellular. The [BackgroundSyncResult.reason] carries a hint when it is
 *   requested. `requiresCharging` now maps directly to `requiresExternalPower`.
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
        val request = BGProcessingTaskRequest(identifier = taskId)
        request.earliestBeginDate = NSDate.dateWithTimeIntervalSinceNow(
            intervalMs / 1000.0
        )
        // Android constrains both workers with NetworkType.CONNECTED; a sync with no network is a
        // guaranteed failure, so let the scheduler hold the task rather than burn the window.
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = requiresCharging

        return try {
            // K/N maps -[BGTaskScheduler submitTaskRequest:error:] as
            // `submitTaskRequest(taskRequest, error: ...)` with a nullable error-out pointer,
            // returning the ObjC BOOL rather than throwing. Passing `null` for the error means
            // we cannot read *why* it failed, but we must at least honour the return value —
            // ignoring it reported "scheduled" to the user even when BGTaskScheduler rejected
            // the request (unregistered identifier, over the pending-request limit, or
            // Background App Refresh disabled in Settings).
            val submitted = BGTaskScheduler.sharedScheduler.submitTaskRequest(request, null)
            if (!submitted) {
                updateStatus(taskId, SyncTaskStatus.Failed)
                return BackgroundSyncResult(
                    scheduled = false,
                    reason = "iOS refused the background task request. Check that Background " +
                        "App Refresh is enabled for HITV in Settings."
                )
            }
            updateStatus(taskId, SyncTaskStatus.Scheduled)
            // requiresCharging is now honoured via requiresExternalPower; only wifiOnly has no
            // iOS equivalent, since requiresNetworkConnectivity cannot distinguish Wi-Fi from
            // cellular.
            val note = if (wifiOnly) {
                "iOS: wifiOnly is not enforced — BGProcessingTaskRequest requires network " +
                    "connectivity but cannot restrict it to Wi-Fi."
            } else null
            BackgroundSyncResult(scheduled = true, reason = note)
        } catch (e: Throwable) {
            // NOTE: this does NOT cover the most likely failure. `submitTaskRequest:error:` raises
            // an ObjC NSInternalInconsistencyException when [taskId] is absent from
            // `BGTaskSchedulerPermittedIdentifiers`, and Kotlin/Native cannot catch ObjC
            // exceptions — that case terminates the process rather than landing here. The guard
            // against it is `scripts/check-bgtask-identifiers.sh`, which fails the build if the
            // Kotlin constants, the Swift registration and Info.plist ever drift apart.
            // This catch covers ordinary Kotlin-side failures only.
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
            // The "Run now" button is user-initiated foreground work — same
            // keepalive contract as the post-login sync.
            withSyncKeepalive("hitv.sync.runOnce.$taskId") {
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
