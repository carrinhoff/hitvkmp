package pt.hitv.feature.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the "Live Buffer Size" mapping to the original's numbers.
 *
 * The port stored this preference but no player read it, so the setting was inert — changing it
 * did nothing on either platform. It is now fed to `DefaultLoadControl` on Android and
 * `preferredForwardBufferDuration` on iOS, so the values have to stay in step with
 * `hitv/core/common/.../media/PlayerConfigFactory.kt:29-39` (`createLiveLoadControl`).
 */
class LiveBufferProfileTest {

    @Test
    fun `small matches the original`() {
        val b = PlayerConfigFactory.liveBufferFor("small")
        assertEquals(3_000, b.minMs)
        assertEquals(10_000, b.maxMs)
        assertEquals(500, b.playbackMs)
        assertEquals(1_500, b.rebufferMs)
    }

    @Test
    fun `medium matches the original`() {
        val b = PlayerConfigFactory.liveBufferFor("medium")
        assertEquals(5_000, b.minMs)
        assertEquals(20_000, b.maxMs)
        assertEquals(800, b.playbackMs)
        assertEquals(2_000, b.rebufferMs)
    }

    @Test
    fun `large matches the original`() {
        val b = PlayerConfigFactory.liveBufferFor("large")
        assertEquals(15_000, b.minMs)
        assertEquals(50_000, b.maxMs)
        assertEquals(2_000, b.playbackMs)
        assertEquals(5_000, b.rebufferMs)
    }

    @Test
    fun `very_large matches the original`() {
        val b = PlayerConfigFactory.liveBufferFor("very_large")
        assertEquals(30_000, b.minMs)
        assertEquals(120_000, b.maxMs)
        assertEquals(3_000, b.playbackMs)
        assertEquals(8_000, b.rebufferMs)
    }

    @Test
    fun `unknown null and empty all fall back to medium`() {
        // The original's `when` ends in `else -> medium`; a stored value from an older build, or
        // no value at all, must not produce a zero-length buffer.
        val medium = PlayerConfigFactory.liveBufferFor("medium")
        listOf(null, "", "gigantic", "MEDIUM").forEach { value ->
            assertEquals(medium, PlayerConfigFactory.liveBufferFor(value), "fallback failed for $value")
        }
    }

    @Test
    fun `buffers grow monotonically across the four sizes`() {
        val sizes = listOf("small", "medium", "large", "very_large")
            .map { PlayerConfigFactory.liveBufferFor(it) }
        for (i in 1 until sizes.size) {
            assertTrue(sizes[i].minMs > sizes[i - 1].minMs, "minMs not increasing at index $i")
            assertTrue(sizes[i].maxMs > sizes[i - 1].maxMs, "maxMs not increasing at index $i")
        }
    }

    @Test
    fun `every profile has a usable non-zero buffer`() {
        listOf("small", "medium", "large", "very_large", null).forEach { value ->
            val b = PlayerConfigFactory.liveBufferFor(value)
            assertTrue(b.minMs > 0 && b.maxMs > b.minMs, "degenerate buffer for $value: $b")
            assertTrue(b.playbackMs in 1 until b.minMs, "playback threshold out of range for $value")
        }
    }

    @Test
    fun `live buffers are shorter than VOD to keep latency down`() {
        // Live favours latency; VOD favours smoothness. Guards against someone "simplifying" the
        // two profiles into one.
        assertTrue(
            PlayerConfigFactory.liveBufferFor("medium").minMs < PlayerConfigFactory.VOD_BUFFER.minMs
        )
    }
}
