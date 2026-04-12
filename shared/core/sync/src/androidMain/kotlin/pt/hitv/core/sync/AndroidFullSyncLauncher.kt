package pt.hitv.core.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

/**
 * Android-specific launcher that chains [DataSyncWorker] followed by [EpgSyncWorker]
 * using WorkManager's unique work chain. Equivalent to the original WorkerHelper.startFullSyncChain().
 *
 * The chain ensures EPG sync only starts after data sync completes successfully.
 * Uses [ExistingWorkPolicy.KEEP] so duplicate launches are ignored if a sync is already running.
 */
class AndroidFullSyncLauncher(private val context: Context) {

    companion object {
        const val UNIQUE_WORK_NAME = "full_sync_chain"
    }

    /**
     * Enqueues a full sync chain (data then EPG) and returns the data worker's UUID
     * for progress observation.
     */
    fun startFullSyncChain(): UUID {
        val dataFetchRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .addTag("data_sync")
            .build()

        val epgFetchRequest = OneTimeWorkRequestBuilder<EpgSyncWorker>()
            .addTag("epg_sync")
            .build()

        WorkManager.getInstance(context)
            .beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, dataFetchRequest)
            .then(epgFetchRequest)
            .enqueue()

        return dataFetchRequest.id
    }

    /**
     * Cancels any running or pending full sync chain.
     */
    fun cancelFullSyncChain() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
