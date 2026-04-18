package pt.hitv.core.sync

import android.content.Context
import pt.hitv.core.common.AndroidContextHolder
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Android implementation of [BackgroundSyncManager] backed by WorkManager.
 *
 * Each taskId maps to a specific Worker class:
 *  - [BackgroundSyncManager.TASK_EPG]     -> [EpgSyncWorker]
 *  - [BackgroundSyncManager.TASK_CONTENT] -> [DataSyncWorker]
 *
 * Tasks are enqueued as unique periodic work so re-schedules replace prior requests
 * (ExistingPeriodicWorkPolicy.UPDATE). One-shot runs use ExistingWorkPolicy.KEEP
 * to avoid duplicate in-flight runs.
 */
actual class BackgroundSyncManager {

    // Matches the `PreferencesHelper.android.kt` pattern: use the app-wide
    // `AndroidContextHolder.applicationContext` rather than injecting Context
    // through Koin. This keeps the expect class constructor-argument-free on
    // Android while still giving WorkManager a reachable Context.
    private val context: Context get() = AndroidContextHolder.applicationContext

    private val statusMap = MutableStateFlow<Map<String, SyncTaskStatus>>(emptyMap())
    actual val statusFlow: StateFlow<Map<String, SyncTaskStatus>> = statusMap.asStateFlow()

    private val observerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    actual fun schedulePeriodic(
        taskId: String,
        intervalMs: Long,
        wifiOnly: Boolean,
        requiresCharging: Boolean
    ): BackgroundSyncResult {
        return try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(requiresCharging)
                .build()

            val request = when (taskId) {
                TASK_EPG -> PeriodicWorkRequestBuilder<EpgSyncWorker>(
                    intervalMs, TimeUnit.MILLISECONDS
                )
                    .setConstraints(constraints)
                    .addTag(taskId)
                    .build()

                TASK_CONTENT -> PeriodicWorkRequestBuilder<DataSyncWorker>(
                    intervalMs, TimeUnit.MILLISECONDS
                )
                    .setConstraints(constraints)
                    .addTag(taskId)
                    .build()

                else -> return BackgroundSyncResult(
                    scheduled = false,
                    reason = "Unknown taskId: $taskId"
                )
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                taskId,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
            observe(taskId)
            updateStatus(taskId, SyncTaskStatus.Scheduled)
            BackgroundSyncResult(scheduled = true)
        } catch (e: Exception) {
            BackgroundSyncResult(scheduled = false, reason = e.message)
        }
    }

    actual fun cancel(taskId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(taskId)
        updateStatus(taskId, SyncTaskStatus.NotScheduled)
    }

    actual fun runOnce(taskId: String): BackgroundSyncResult {
        return try {
            val request = when (taskId) {
                TASK_EPG -> OneTimeWorkRequestBuilder<EpgSyncWorker>()
                    .addTag(taskId)
                    .build()

                TASK_CONTENT -> OneTimeWorkRequestBuilder<DataSyncWorker>()
                    .addTag(taskId)
                    .build()

                else -> return BackgroundSyncResult(
                    scheduled = false,
                    reason = "Unknown taskId: $taskId"
                )
            }

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${taskId}_oneshot",
                ExistingWorkPolicy.KEEP,
                request
            )
            observe(taskId)
            updateStatus(taskId, SyncTaskStatus.Running)
            BackgroundSyncResult(scheduled = true)
        } catch (e: Exception) {
            BackgroundSyncResult(scheduled = false, reason = e.message)
        }
    }

    /**
     * Subscribe to WorkManager WorkInfo flow for [taskId] and project WorkInfo.State
     * onto [SyncTaskStatus]. Idempotent — calling again simply re-launches a collector;
     * the underlying StateFlow deduplicates identical emissions.
     */
    private fun observe(taskId: String) {
        observerScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosByTagFlow(taskId)
                .collect { infos ->
                    val latest = infos.maxByOrNull { it.runAttemptCount } ?: return@collect
                    val projected = when (latest.state) {
                        WorkInfo.State.ENQUEUED -> SyncTaskStatus.Scheduled
                        WorkInfo.State.RUNNING -> SyncTaskStatus.Running
                        WorkInfo.State.SUCCEEDED -> SyncTaskStatus.Succeeded
                        WorkInfo.State.FAILED -> SyncTaskStatus.Failed
                        WorkInfo.State.CANCELLED -> SyncTaskStatus.NotScheduled
                        WorkInfo.State.BLOCKED -> SyncTaskStatus.Scheduled
                    }
                    updateStatus(taskId, projected)
                }
        }
    }

    private fun updateStatus(taskId: String, status: SyncTaskStatus) {
        statusMap.value = statusMap.value.toMutableMap().apply { put(taskId, status) }
    }

}
