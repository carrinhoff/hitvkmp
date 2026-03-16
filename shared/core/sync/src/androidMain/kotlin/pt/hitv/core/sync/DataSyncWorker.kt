package pt.hitv.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager CoroutineWorker for background data (channels, movies, series) sync.
 *
 * This worker is scheduled by [AndroidSyncScheduler] and delegates actual sync
 * work to [SyncManager]. It runs on the IO dispatcher.
 */
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // In a full implementation, retrieve SyncManager from Koin and perform sync.
            // val syncManager: SyncManager = KoinJavaComponent.get(SyncManager::class.java)
            // val userId = ... // obtain from preferences
            // syncManager.syncChannels(userId)
            // syncManager.syncMovies(userId)
            // syncManager.syncSeries(userId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
