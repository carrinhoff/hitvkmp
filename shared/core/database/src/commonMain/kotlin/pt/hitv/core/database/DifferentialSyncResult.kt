package pt.hitv.core.database

/**
 * Result of a differential sync operation.
 * Reports how many items were inserted, updated, and deleted.
 */
data class DifferentialSyncResult(
    val inserted: Int,
    val updated: Int,
    val deleted: Int
)
