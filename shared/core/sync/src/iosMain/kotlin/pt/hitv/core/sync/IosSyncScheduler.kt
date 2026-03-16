package pt.hitv.core.sync

/**
 * iOS implementation of [SyncScheduler].
 *
 * This is a stub for BGTaskScheduler integration. The actual BGTaskScheduler
 * registration must be done from the Swift side (in AppDelegate or @main App struct):
 *
 * ```swift
 * BGTaskScheduler.shared.register(
 *     forTaskWithIdentifier: "pt.hitv.sync",
 *     using: nil
 * ) { task in
 *     // Call KMP sync logic
 * }
 * ```
 *
 * This Kotlin class provides the interface for the KMP layer to request
 * scheduling, but the actual scheduling call happens in Swift.
 */
class IosSyncScheduler : SyncScheduler {

    override fun schedulePeriodicSync(intervalHours: Int) {
        // BGTaskScheduler.shared.submit(BGAppRefreshTaskRequest)
        // This will be called from Swift; KMP just signals intent.
        // The Swift layer observes this and registers the BGTask accordingly.
    }

    override fun cancelPeriodicSync() {
        // BGTaskScheduler.shared.cancel(taskRequestWithIdentifier:)
        // Same pattern: Swift layer handles the actual cancellation.
    }
}
