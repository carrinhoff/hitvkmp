package pt.hitv.feature.player.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Behavioural tests for the watchdog that rescues "stream never starts" on iOS, where
 * `AVPlayerItemStatusFailed` can go undetected because the periodic time observer needs the
 * timeline to advance in order to run at all.
 *
 * Uses `runTest`'s virtual clock, so the 25 s deadline costs no wall-clock time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStartWatchdogTest {

    @Test
    fun `fires once after the timeout when never cancelled`() = runTest {
        var fired = 0
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 25_000) { fired++ }

        watchdog.arm()
        assertEquals(0, fired, "must not fire before the deadline")

        advanceTimeBy(24_999)
        assertEquals(0, fired, "fired early")

        advanceUntilIdle()
        assertEquals(1, fired, "expected exactly one firing")
    }

    @Test
    fun `cancel before the deadline prevents firing`() = runTest {
        var fired = false
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 25_000) { fired = true }

        watchdog.arm()
        advanceTimeBy(10_000)
        watchdog.cancel() // playback reported ready

        advanceUntilIdle()
        assertFalse(fired, "watchdog fired after playback was confirmed running")
    }

    @Test
    fun `cancel after the deadline does not retroactively suppress the firing`() = runTest {
        var fired = false
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 1_000) { fired = true }

        watchdog.arm()
        advanceUntilIdle()
        watchdog.cancel()

        assertTrue(fired, "the error was already surfaced; cancel must not rewrite history")
    }

    @Test
    fun `re-arming discards the previous deadline`() = runTest {
        var fired = 0
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 10_000) { fired++ }

        watchdog.arm()
        advanceTimeBy(9_000)
        watchdog.arm() // e.g. a retry replaced the player item

        // The first deadline would have elapsed here had it survived.
        advanceTimeBy(2_000)
        assertEquals(0, fired, "stale deadline from the previous arm() fired")

        advanceUntilIdle()
        assertEquals(1, fired, "the re-armed deadline should fire exactly once")
    }

    @Test
    fun `repeated arm-cancel cycles never double-fire`() = runTest {
        var fired = 0
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 5_000) { fired++ }

        // Mirrors the channel player switching channels several times, each start succeeding.
        repeat(3) {
            watchdog.arm()
            advanceTimeBy(1_000)
            watchdog.cancel()
        }
        advanceUntilIdle()
        assertEquals(0, fired, "successful starts must never surface an error")
    }

    @Test
    fun `zero timeout disables the watchdog entirely`() = runTest {
        var fired = false
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 0) { fired = true }

        watchdog.arm()
        advanceUntilIdle()
        assertFalse(fired, "timeoutMs = 0 is the documented off switch")
    }

    @Test
    fun `cancel is safe when nothing is armed and when called repeatedly`() = runTest {
        var fired = false
        val watchdog = PlaybackStartWatchdog(scope = this, timeoutMs = 5_000) { fired = true }

        // onDispose can run without arm() ever having been reached.
        watchdog.cancel()
        watchdog.cancel()
        advanceUntilIdle()
        assertFalse(fired)
    }

    @Test
    fun `default timeout matches the original project's 25 seconds`() {
        assertEquals(25_000L, PlaybackStartWatchdog.DEFAULT_TIMEOUT_MS)
    }
}
