package pt.hitv.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.java.KoinJavaComponent
import pt.hitv.core.common.PreferencesHelper

/**
 * WorkManager CoroutineWorker for background data (channels, movies, series) sync.
 *
 * This worker is scheduled by [AndroidSyncScheduler] and delegates actual sync
 * work to [SyncManagerImpl.performFullSync]. It runs on the IO dispatcher and
 * reports progress via WorkManager's [setProgress].
 */
class DataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncManager = KoinJavaComponent.get<SyncManager>(SyncManager::class.java) as SyncManagerImpl
            val preferencesHelper = KoinJavaComponent.get<PreferencesHelper>(PreferencesHelper::class.java)
            val userId = preferencesHelper.getUserId()

            val result = syncManager.performFullSync(userId) { percent, stage, message ->
                setProgress(
                    workDataOf(
                        KEY_TOTAL_PROGRESS to percent,
                        KEY_STAGE_NAME to stage,
                        KEY_STAGE_MESSAGE to message
                    )
                )
            }

            if (result.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_TOTAL_PROGRESS = "total_progress"
        const val KEY_STAGE_NAME = "stage_name"
        const val KEY_STAGE_MESSAGE = "stage_message"
    }
}
