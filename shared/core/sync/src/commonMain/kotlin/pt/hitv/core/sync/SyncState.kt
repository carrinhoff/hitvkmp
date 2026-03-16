package pt.hitv.core.sync

/**
 * Represents the current synchronization state.
 */
enum class SyncState {
    /** No sync operation in progress */
    IDLE,
    /** Data (channels, movies, series) sync in progress */
    SYNCING_DATA,
    /** EPG (Electronic Program Guide) sync in progress */
    SYNCING_EPG
}

/**
 * Data class containing EPG sync progress information.
 */
data class EpgProgress(
    val stage: String = "",
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val isComplete: Boolean = false
)

/**
 * Sync type identifier for tracking sync completion per content type.
 */
enum class SyncType {
    EPG, CONTENT
}

/**
 * Status snapshot of background sync configuration and last-known state.
 */
data class SyncStatus(
    val backgroundSyncEnabled: Boolean,
    val lastEpgSync: Long?,
    val lastContentSync: Long?,
    val lastSyncSuccess: Boolean,
    val epgIntervalHours: Long,
    val contentIntervalHours: Long,
    val wifiOnlySync: Boolean
)
