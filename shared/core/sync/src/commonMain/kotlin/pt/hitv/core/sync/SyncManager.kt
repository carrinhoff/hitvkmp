package pt.hitv.core.sync

interface SyncManager {
    suspend fun syncChannels(userId: Int): SyncResult
    suspend fun syncMovies(userId: Int): SyncResult
    suspend fun syncSeries(userId: Int): SyncResult
    suspend fun syncEpg(userId: Int): SyncResult
    fun schedulePeriodicSync(intervalHours: Int)
    fun cancelPeriodicSync()
}

data class SyncResult(
    val isSuccess: Boolean,
    val inserted: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
    val errorMessage: String? = null
)
