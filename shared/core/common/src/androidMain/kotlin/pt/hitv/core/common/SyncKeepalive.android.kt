package pt.hitv.core.common

/**
 * Android does not need a keepalive for foreground-initiated work — the process
 * is not suspended while the app is in the foreground, and background sync
 * routes through WorkManager (`DataSyncWorker` / `EpgSyncWorker`) which has its
 * own constraints + execution window.
 */
actual suspend fun <T> withSyncKeepalive(label: String, block: suspend () -> T): T = block()
