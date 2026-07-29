import SwiftUI
import BackgroundTasks
import Foundation
import shared

/// Ensures `setTaskCompleted(success:)` is called exactly once for a `BGTask`.
///
/// Now that an `expirationHandler` is installed, two paths can race to finish the same task: the
/// sync callback completing normally, and iOS reclaiming the execution window. Calling
/// `setTaskCompleted` twice raises an exception and terminates the app — in the background, where
/// it is invisible until the crash reports arrive. Calling it zero times is what the
/// expirationHandler exists to prevent.
///
/// Deliberately a class, not a struct: the two closures must share one piece of mutable state.
private final class TaskCompletion {
    private let task: BGTask
    private let lock = NSLock()
    private var finished = false

    init(_ task: BGTask) {
        self.task = task
    }

    func complete(success: Bool) {
        lock.lock()
        defer { lock.unlock() }
        guard !finished else { return }
        finished = true
        task.setTaskCompleted(success: success)
    }
}

@main
struct iOSApp: App {

    // Task identifiers must match `BackgroundSyncManager.TASK_EPG` / `TASK_CONTENT`
    // in Kotlin and `BGTaskSchedulerPermittedIdentifiers` in Info.plist.
    private static let taskIdEpg = "pt.hitv.sync.epg"
    private static let taskIdContent = "pt.hitv.sync.content"

    // Default cadences (best-effort — iOS decides actual firing).
    private static let epgIntervalSeconds: TimeInterval = 6 * 60 * 60
    private static let contentIntervalSeconds: TimeInterval = 24 * 60 * 60

    init() {
        // BGTask handlers MUST be registered before the first `submit` call and
        // BEFORE Koin/init — the BGTaskScheduler scans handlers at process launch.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: iOSApp.taskIdEpg,
            using: nil
        ) { task in
            // `completion` guarantees setTaskCompleted is called exactly once — see its docs.
            let completion = TaskCompletion(task)

            // Without an expirationHandler, a sync that outlives the window iOS granted never
            // calls setTaskCompleted. The system then treats the task as having overrun and
            // progressively deprioritises future scheduling for this app — background sync
            // quietly degrades to never running. A full content sync over tens of thousands of
            // channels can easily exceed the ~30s a BGAppRefreshTask typically gets.
            task.expirationHandler = {
                completion.complete(success: false)
            }

            SyncBridgeKt.runEpgSync { success in
                completion.complete(success: success.boolValue)
            }
            iOSApp.scheduleRefresh(identifier: iOSApp.taskIdEpg, in: iOSApp.epgIntervalSeconds)
        }

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: iOSApp.taskIdContent,
            using: nil
        ) { task in
            let completion = TaskCompletion(task)
            task.expirationHandler = {
                completion.complete(success: false)
            }
            SyncBridgeKt.runContentSync { success in
                completion.complete(success: success.boolValue)
            }
            iOSApp.scheduleRefresh(identifier: iOSApp.taskIdContent, in: iOSApp.contentIntervalSeconds)
        }

        KoinIOSKt.doInitKoinIOS()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.all)
        }
    }

    /// Reschedules a BGProcessingTaskRequest for the given identifier. Called from
    /// the task handler to chain the next run.
    ///
    /// Must stay the same request type as `BackgroundSyncManager.ios.kt` submits. Chaining a
    /// different type here than the initial submission would silently change the duration and
    /// constraints the follow-up run gets — see that file for why processing is the right one.
    private static func scheduleRefresh(identifier: String, in seconds: TimeInterval) {
        let request = BGProcessingTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: seconds)
        // Mirrors Android's NetworkType.CONNECTED constraint on both periodic workers.
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Debug-build emission only; release users should not see this.
            print("BGTaskScheduler submit failed for \(identifier): \(error)")
        }
    }
}
