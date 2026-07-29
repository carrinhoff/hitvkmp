package pt.hitv.core.common

/**
 * Wraps a long-running, user-initiated task (post-login sync, manual "Run now") in
 * a platform-appropriate keepalive window so the OS does not interrupt it.
 *
 * - **Android:** passthrough. While the app is foreground the process is not
 *   killed, and WorkManager handles the truly-background case via [SyncScheduler].
 * - **iOS:** sets `UIApplication.idleTimerDisabled = true` so the screen does not
 *   auto-lock, and brackets the work with `beginBackgroundTask` /
 *   `endBackgroundTask` so the OS grants up to ~30 s of grace if the user does
 *   manually lock the device. Both are paired in `finally` under `NonCancellable`
 *   so the cleanup runs even if the calling coroutine is cancelled.
 *
 * The window covers the whole [block] — outer ContextSwitches inside [block] (e.g.
 * `withContext(Dispatchers.IO)`) do not break it.
 *
 * Re-entrancy: the iOS actual is safe to nest because each call gets its own
 * `UIBackgroundTaskIdentifier`. `idleTimerDisabled` is a single boolean so
 * concurrent overlapping calls would race, but the sync layer guards against
 * concurrent runs (`SyncStateManager.runningJob`).
 */
expect suspend fun <T> withSyncKeepalive(label: String, block: suspend () -> T): T
