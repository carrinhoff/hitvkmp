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
 * WorkManager CoroutineWorker for background EPG sync.
 *
 * This worker is scheduled by [AndroidSyncScheduler] and delegates actual EPG
 * sync work to [SyncManager]. It runs on the IO dispatcher and reports progress
 * via WorkManager's [setProgress].
 */
class EpgSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val syncManager = KoinJavaComponent.get<SyncManager>(SyncManager::class.java)
            val preferencesHelper = KoinJavaComponent.get<PreferencesHelper>(PreferencesHelper::class.java)
            val userId = preferencesHelper.getUserId()

            setProgress(
                workDataOf(
                    KEY_PROGRESS to 0,
                    KEY_STAGE to "EPG",
                    KEY_MESSAGE to "Fetching EPG data..."
                )
            )

            val result = syncManager.syncEpg(userId)

            if (result.isSuccess) {
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to 100,
                        KEY_STAGE to "EPG",
                        KEY_MESSAGE to "EPG sync complete"
                    )
                )
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_PROGRESS = "epg_progress"
        const val KEY_STAGE = "epg_stage"
        const val KEY_MESSAGE = "epg_message"
    }
}
