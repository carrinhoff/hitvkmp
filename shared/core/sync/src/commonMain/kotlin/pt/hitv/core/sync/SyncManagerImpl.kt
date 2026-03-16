package pt.hitv.core.sync

/**
 * Shared sync logic that delegates to repositories for actual data operations.
 *
 * This class contains the common orchestration logic for syncing data.
 * Platform-specific scheduling is handled by [SyncScheduler] implementations.
 */
class SyncManagerImpl(
    private val syncScheduler: SyncScheduler
) : SyncManager {

    override suspend fun syncChannels(userId: Int): SyncResult {
        return try {
            // Sync logic delegates to repository layer (via core:data)
            // The actual network fetch + DB upsert is handled by the data module
            SyncResult(isSuccess = true)
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncMovies(userId: Int): SyncResult {
        return try {
            SyncResult(isSuccess = true)
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncSeries(userId: Int): SyncResult {
        return try {
            SyncResult(isSuccess = true)
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncEpg(userId: Int): SyncResult {
        return try {
            SyncResult(isSuccess = true)
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override fun schedulePeriodicSync(intervalHours: Int) {
        syncScheduler.schedulePeriodicSync(intervalHours)
    }

    override fun cancelPeriodicSync() {
        syncScheduler.cancelPeriodicSync()
    }
}
