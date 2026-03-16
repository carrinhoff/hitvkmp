package pt.hitv.core.sync

/**
 * Platform-specific sync scheduler interface.
 *
 * Android uses WorkManager, iOS uses BGTaskScheduler.
 */
interface SyncScheduler {
    fun schedulePeriodicSync(intervalHours: Int)
    fun cancelPeriodicSync()
}
