package pt.hitv.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Android implementation of [SyncScheduler] using WorkManager.
 *
 * Schedules periodic background sync for both content and EPG data
 * with network connectivity constraints.
 */
class AndroidSyncScheduler(
    private val context: Context
) : SyncScheduler {

    companion object {
        private const val CONTENT_PERIODIC_WORK = "content_periodic_sync"
        private const val EPG_PERIODIC_WORK = "epg_periodic_sync"
    }

    override fun schedulePeriodicSync(intervalHours: Int) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val contentRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("background_content_sync")
            .build()

        val epgRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(
            intervalHours.toLong(), TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("background_epg_sync")
            .build()

        val workManager = WorkManager.getInstance(context)

        workManager.enqueueUniquePeriodicWork(
            CONTENT_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            contentRequest
        )

        workManager.enqueueUniquePeriodicWork(
            EPG_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            epgRequest
        )
    }

    override fun cancelPeriodicSync() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(CONTENT_PERIODIC_WORK)
        workManager.cancelUniqueWork(EPG_PERIODIC_WORK)
    }
}
