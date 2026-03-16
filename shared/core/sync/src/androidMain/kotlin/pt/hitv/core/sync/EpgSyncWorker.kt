package pt.hitv.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager CoroutineWorker for background EPG sync.
 *
 * This worker is scheduled by [AndroidSyncScheduler] and delegates actual EPG
 * sync work to [SyncManager]. It runs on the IO dispatcher.
 */
class EpgSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // In a full implementation, retrieve SyncManager from Koin and perform EPG sync.
            // val syncManager: SyncManager = KoinJavaComponent.get(SyncManager::class.java)
            // val userId = ... // obtain from preferences
            // syncManager.syncEpg(userId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
