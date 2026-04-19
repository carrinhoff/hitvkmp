package pt.hitv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.SyncManager
import pt.hitv.core.sync.SyncManagerImpl
import pt.hitv.core.sync.SyncTaskStatus
import pt.hitv.core.sync.TASK_CONTENT
import pt.hitv.core.sync.TASK_EPG

/**
 * Top-level Kotlin entry points callable from Swift.
 *
 * Invoked from the `BGTaskScheduler.register(forTaskWithIdentifier:)` launch
 * handlers in `iOSApp.swift`. Each function:
 *   1. resolves the appropriate sync function from Koin,
 *   2. runs it on a background dispatcher, and
 *   3. fires [onComplete] when the sync finishes (success/failure).
 *
 * Lives in iosMain because [BackgroundSyncManager.reportStatus] is iOS-only
 * (defined on the iOS actual class, not the expect class).
 */

private val bridgeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun runEpgSync(onComplete: (Boolean) -> Unit) {
    bridgeScope.launch {
        val success = try {
            val syncManager = KoinPlatform.getKoin().get<SyncManager>()
            val preferencesHelper = KoinPlatform.getKoin().get<PreferencesHelper>()
            val userId = preferencesHelper.getUserId()
            val result = syncManager.syncEpg(userId)
            reportStatus(TASK_EPG, result.isSuccess)
            result.isSuccess
        } catch (t: Throwable) {
            reportStatus(TASK_EPG, false)
            false
        }
        onComplete(success)
    }
}

fun runContentSync(onComplete: (Boolean) -> Unit) {
    bridgeScope.launch {
        val success = try {
            val syncManager = KoinPlatform.getKoin().get<SyncManager>()
            val preferencesHelper = KoinPlatform.getKoin().get<PreferencesHelper>()
            val userId = preferencesHelper.getUserId()
            val result = if (syncManager is SyncManagerImpl) {
                syncManager.performFullSync(userId) { _, _, _ -> }
            } else {
                syncManager.syncChannels(userId)
            }
            reportStatus(TASK_CONTENT, result.isSuccess)
            result.isSuccess
        } catch (t: Throwable) {
            reportStatus(TASK_CONTENT, false)
            false
        }
        onComplete(success)
    }
}

private fun reportStatus(taskId: String, success: Boolean) {
    try {
        val manager = KoinPlatform.getKoin().get<BackgroundSyncManager>()
        manager.reportStatus(
            taskId,
            if (success) SyncTaskStatus.Succeeded else SyncTaskStatus.Failed
        )
    } catch (_: Throwable) {
        // Koin may not be initialised yet during very early BGTask firings;
        // silently ignore — the next sync attempt will update status.
    }
}
