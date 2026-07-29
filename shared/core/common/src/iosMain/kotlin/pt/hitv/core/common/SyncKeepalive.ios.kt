@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.core.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid

/**
 * iOS keepalive: prevent screen auto-lock and request a background-task window
 * for the duration of [block].
 *
 * - `idleTimerDisabled = true` while running, restored to `false` afterwards.
 * - `beginBackgroundTask` returns an identifier; we end it in `finally`.
 * - Both UIApplication touchpoints dispatch to the main queue (`Dispatchers.Main`)
 *   because UIKit requires it — the existing call site in
 *   `EpgStreamingLoader.ios.kt` was lenient about this, but Apple is not always.
 * - `NonCancellable + Dispatchers.Main` on the cleanup ensures `endBackgroundTask`
 *   and the idleTimer reset still run if the parent coroutine is cancelled.
 *
 * Memory note (KMP iOS ObjC bindings): `idleTimerDisabled` is an ObjC `@property`,
 * so we use property syntax (`app.idleTimerDisabled = true`) — the
 * `setIdleTimerDisabled(...)` form is not generated.
 */
actual suspend fun <T> withSyncKeepalive(label: String, block: suspend () -> T): T {
    val app = UIApplication.sharedApplication

    val taskId = withContext(Dispatchers.Main) {
        app.idleTimerDisabled = true
        // [label] is intentionally unused here. UIApplication has a labeled
        // `beginBackgroundTaskWithName:expirationHandler:` variant that would
        // surface the label in `os_log`, but the unlabeled form is the one
        // already proven to work in `EpgStreamingLoader.ios.kt`'s K/N cinterop
        // bindings on this project — keep it consistent to avoid a CI-only
        // "Unresolved reference" round-trip.
        //
        // Null expiration handler => iOS silently ends the task when its window
        // runs out. We don't need a notification — the in-flight URLSession
        // will error with -999 (cancelled), the stage returns failure, and our
        // per-stage flags ensure the next foreground resumes from where we
        // left off rather than redoing completed stages.
        app.beginBackgroundTaskWithExpirationHandler(null)
    }

    try {
        return block()
    } finally {
        withContext(NonCancellable + Dispatchers.Main) {
            if (taskId != UIBackgroundTaskInvalid) {
                app.endBackgroundTask(taskId)
            }
            app.idleTimerDisabled = false
        }
    }
}
