package pt.hitv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.SyncManager
import pt.hitv.core.sync.SyncManagerImpl
import pt.hitv.core.sync.SyncStatus

/**
 * Top-level Kotlin entry points callable from Swift.
 *
 * Invoked from the `BGTaskScheduler.register(forTaskWithIdentifier:)` launch
 * handlers in `iOSApp.swift`. Each function:
 *   1. resolves the appropriate sync function from Koin,
 *   2. runs it on a background dispatcher, and
 *   3. fires [onComplete] when the sync finishes (success/failure).
 *
 * The Swift handler MUST call `task.setTaskCompleted(success:)` inside [onComplete]
 * so the OS can accurately account for the task window.
 */

private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * Runs an EPG sync on a background dispatcher. Invokes [onComplete] with `true`
 * if the sync succeeded, `false` otherwise.
 */
fun runEpgSync(onComplete: (Boolean) -> Unit) {
    bridgeScope.launch {
        val success = try {
            val syncManager = GlobalContext.get().get<SyncManager>()
            val preferencesHelper = GlobalContext.get().get<PreferencesHelper>()
            val userId = preferencesHelper.getUserId()
            val result = syncManager.syncEpg(userId)
            reportStatus(BackgroundSyncManager.TASK_EPG, result.isSuccess)
            result.isSuccess
        } catch (t: Throwable) {
            reportStatus(BackgroundSyncManager.TASK_EPG, false)
            false
        }
        onComplete(success)
    }
}

/**
 * Runs a full content sync (channels + movies + series) on a background dispatcher.
 * Invokes [onComplete] with `true` if the sync succeeded, `false` otherwise.
 */
fun runContentSync(onComplete: (Boolean) -> Unit) {
    bridgeScope.launch {
        val success = try {
            val syncManager = GlobalContext.get().get<SyncManager>()
            val preferencesHelper = GlobalContext.get().get<PreferencesHelper>()
            val userId = preferencesHelper.getUserId()
            val result = if (syncManager is SyncManagerImpl) {
                syncManager.performFullSync(userId) { _, _, _ -> }
            } else {
                syncManager.syncChannels(userId)
            }
            reportStatus(BackgroundSyncManager.TASK_CONTENT, result.isSuccess)
            result.isSuccess
        } catch (t: Throwable) {
            reportStatus(BackgroundSyncManager.TASK_CONTENT, false)
            false
        }
        onComplete(success)
    }
}

private fun reportStatus(taskId: String, success: Boolean) {
    try {
        val manager = GlobalContext.get().get<BackgroundSyncManager>()
        manager.reportStatus(
            taskId,
            if (success) SyncStatus.Succeeded else SyncStatus.Failed
        )
    } catch (_: Throwable) {
        // Koin may not be initialised yet during very early BGTask firings;
        // silently ignore — the next sync attempt will update status.
    }
}
