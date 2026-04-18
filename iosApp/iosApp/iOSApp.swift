import SwiftUI
import BackgroundTasks
import shared

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
            SyncBridgeKt.runEpgSync { success in
                task.setTaskCompleted(success: success)
            }
            iOSApp.scheduleRefresh(identifier: iOSApp.taskIdEpg, in: iOSApp.epgIntervalSeconds)
        }

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: iOSApp.taskIdContent,
            using: nil
        ) { task in
            SyncBridgeKt.runContentSync { success in
                task.setTaskCompleted(success: success)
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

    /// Reschedules a BGAppRefreshTaskRequest for the given identifier. Called from
    /// the task handler to chain the next run.
    private static func scheduleRefresh(identifier: String, in seconds: TimeInterval) {
        let request = BGAppRefreshTaskRequest(identifier: identifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: seconds)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Debug-build emission only; release users should not see this.
            print("BGTaskScheduler submit failed for \(identifier): \(error)")
        }
    }
}
