package pt.hitv.core.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The re-arm previously lived in `BackgroundSyncSettingsViewModel.init`, which only runs when the
 * user opens More Options. So "enable background sync → quit → reopen and go straight to watching"
 * left the OS tasks unscheduled indefinitely, while the settings screen still showed the feature
 * as on.
 *
 * On iOS that is permanent rather than merely delayed: a `BGAppRefreshTaskRequest` is one-shot and
 * the Swift handler only chains the next one after a firing, so if nothing submits the first
 * request the chain never starts.
 *
 * These pin the decision logic. Whether iOS then honours the request is a scheduling matter no
 * unit test can assert — that part is in the audit's device pass.
 */
class BackgroundSyncBootstrapperTest {

    private val hourMs = 60L * 60 * 1000
    private val dayMs = 24 * hourMs

    @Test
    fun `schedules nothing when the user has not enabled background sync`() {
        val requests = computeSyncSchedule(
            enabled = false,
            storedEpgIntervalHours = 6L,
            storedContentIntervalDays = 3L,
            wifiOnly = true,
        )
        assertTrue(requests.isEmpty(), "must not schedule anything when the preference is off")
    }

    @Test
    fun `schedules both tasks when enabled`() {
        val requests = computeSyncSchedule(
            enabled = true,
            storedEpgIntervalHours = 0L,
            storedContentIntervalDays = 0L,
            wifiOnly = true,
        )
        assertEquals(
            listOf(TASK_EPG, TASK_CONTENT),
            requests.map { it.taskId },
            "both the EPG and content tasks must be re-armed",
        )
    }

    @Test
    fun `uses the stored intervals`() {
        val byTask = computeSyncSchedule(
            enabled = true,
            storedEpgIntervalHours = 6L,
            storedContentIntervalDays = 3L,
            wifiOnly = true,
        ).associate { it.taskId to it.intervalMs }

        assertEquals(6 * hourMs, byTask.getValue(TASK_EPG))
        assertEquals(3 * dayMs, byTask.getValue(TASK_CONTENT))
    }

    @Test
    fun `falls back to defaults when intervals were never written`() {
        // getStoredLongTag returns 0 for an absent key; treating that as a literal interval would
        // schedule a zero-delay task and hammer the provider.
        val byTask = computeSyncSchedule(
            enabled = true,
            storedEpgIntervalHours = 0L,
            storedContentIntervalDays = 0L,
            wifiOnly = true,
        ).associate { it.taskId to it.intervalMs }

        assertEquals(DEFAULT_BG_SYNC_EPG_INTERVAL_HOURS * hourMs, byTask.getValue(TASK_EPG))
        assertEquals(DEFAULT_BG_SYNC_CONTENT_INTERVAL_DAYS * dayMs, byTask.getValue(TASK_CONTENT))
        assertTrue(byTask.values.all { it > 0 }, "no task may be scheduled with a zero interval")
    }

    @Test
    fun `negative stored intervals also fall back rather than scheduling in the past`() {
        val byTask = computeSyncSchedule(
            enabled = true,
            storedEpgIntervalHours = -5L,
            storedContentIntervalDays = -1L,
            wifiOnly = true,
        ).associate { it.taskId to it.intervalMs }

        assertEquals(DEFAULT_BG_SYNC_EPG_INTERVAL_HOURS * hourMs, byTask.getValue(TASK_EPG))
        assertEquals(DEFAULT_BG_SYNC_CONTENT_INTERVAL_DAYS * dayMs, byTask.getValue(TASK_CONTENT))
    }

    @Test
    fun `carries the wifi-only preference through`() {
        val on = computeSyncSchedule(true, 0L, 0L, wifiOnly = true)
        assertTrue(on.all { it.wifiOnly })

        val off = computeSyncSchedule(true, 0L, 0L, wifiOnly = false)
        assertTrue(off.none { it.wifiOnly })
    }

    @Test
    fun `task ids match the ones registered in Info dot plist`() {
        // Drift here is an uncatchable ObjC exception at launch — the reason
        // scripts/check-bgtask-identifiers.sh exists. Pinned in a test too so a rename is caught
        // even by someone running only the unit suite.
        assertEquals("pt.hitv.sync.epg", TASK_EPG)
        assertEquals("pt.hitv.sync.content", TASK_CONTENT)
    }
}
