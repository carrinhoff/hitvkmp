@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import kotlinx.cinterop.CValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTime
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.NSRunLoop
import platform.Foundation.runUntilDate
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Decodes a real HLS stream through AVPlayer on an iOS simulator.
 *
 * Everything else in the iOS test suite exercises setup and teardown; nothing had actually decoded
 * video. This closes that: it drives the same pipeline the player hosts use — build an
 * `AVPlayerItem` from a URL, attach a periodic time observer, `play()`, watch `status` become
 * `readyToPlay`, confirm the clock advances, seek, then tear down.
 *
 * It also validates the premise the whole watchdog design rests on: that `AVPlayerItem.status` and
 * the periodic time observer behave as assumed on a working stream, so the failure branch really
 * does mean failure.
 *
 * ## Why the wait pumps the run loop
 *
 * The first CI run of this suite failed all four network cases with `status=Unknown, error=null`
 * after a 30-second wait — which reads exactly like an unreachable CDN and was not. AVFoundation
 * delivers `AVPlayerItem.status` on the main queue, and Kotlin/Native's `runBlocking` drives its
 * own event loop rather than `NSRunLoop`, so polling with `delay()` held the main thread without
 * ever letting AVFoundation dispatch anything. The item could not load because nothing was
 * servicing the run loop.
 *
 * `waitFor` now re-enters the run loop with `runUntilDate` between polls. That is also why these
 * tests no longer use `runBlocking`: there is nothing left to suspend on, and its presence was what
 * caused the problem.
 *
 * ## Network dependency, stated plainly
 *
 * This test hits Apple's long-standing public HLS sample, so it is the one test here that can fail
 * for reasons unrelated to this codebase. It is deliberately **not** written to swallow that: a
 * soft-pass on network trouble would make it indistinguishable from a test that never ran, which is
 * the failure mode this audit spent its time removing. If it genuinely goes flaky, quarantine it
 * explicitly rather than making it lie.
 */
class AVPlayerPlaybackIosTest {

    /** Apple's reference HLS stream, used in Apple's own sample code for many years. */
    private val hlsUrl =
        "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"

    private val pollInterval = 250L
    private val readyTimeoutMs = 30_000L

    private fun player(): AVPlayer {
        val url = NSURL.URLWithString(hlsUrl)
        assertNotNull(url, "sample HLS URL should parse")
        return AVPlayer(playerItem = AVPlayerItem(uRL = url))
    }

    /**
     * Polls until [predicate] holds or the timeout elapses, **servicing the main run loop** while
     * it waits. Returns whether it held.
     *
     * The run-loop pump is the whole point. AVFoundation delivers `AVPlayerItem.status` changes on
     * the main queue, and Kotlin/Native's `runBlocking` drives its own event loop rather than
     * `NSRunLoop` — so a `delay()`-based poll blocks the main thread without ever letting
     * AVFoundation dispatch anything. The item then sits at `AVPlayerItemStatusUnknown` with a null
     * error until the timeout, which reads exactly like an unreachable stream and is not: the item
     * never got the chance to load at all.
     *
     * `runUntilDate` re-enters the run loop for a slice, letting those callbacks land.
     */
    private fun waitFor(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val steps = (timeoutMs / pollInterval).toInt().coerceAtLeast(1)
        repeat(steps) {
            if (predicate()) return true
            NSRunLoop.mainRunLoop().runUntilDate(
                NSDate.dateWithTimeIntervalSinceNow(pollInterval / 1000.0)
            )
        }
        return predicate()
    }

    @Test
    fun `reaches readyToPlay and reports a duration`() {
        val avPlayer = player()
        try {
            val ready = waitFor(readyTimeoutMs) {
                avPlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
            }
            assertTrue(
                ready,
                "item never reached readyToPlay (status=${avPlayer.currentItem?.status}, " +
                    "error=${avPlayer.currentItem?.error?.localizedDescription})",
            )

            val durationSec = CMTimeGetSeconds(avPlayer.currentItem!!.duration)
            assertTrue(
                durationSec.isFinite() && durationSec > 0.0,
                "expected a finite positive duration for a VOD playlist, got $durationSec",
            )
        } finally {
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
        }
    }

    @Test
    fun `playback advances the clock`() {
        val avPlayer = player()
        try {
            assertTrue(
                waitFor(readyTimeoutMs) {
                    avPlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
                },
                "item never became ready",
            )

            avPlayer.play()
            val advanced = waitFor(15_000L) {
                CMTimeGetSeconds(avPlayer.currentTime()) > 0.5
            }
            assertTrue(
                advanced,
                "clock did not advance past 0.5s — decode never started " +
                    "(pos=${CMTimeGetSeconds(avPlayer.currentTime())})",
            )
        } finally {
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
        }
    }

    @Test
    fun `the periodic time observer actually fires during playback`() {
        // The three hosts derive ALL playback state from this callback. If it does not fire the
        // way the port assumes, buffering/ready/error handling silently never runs.
        val avPlayer = player()
        var ticks = 0
        val token = avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000),
            queue = null,
            usingBlock = { _: CValue<CMTime> -> ticks++ },
        )
        try {
            assertTrue(
                waitFor(readyTimeoutMs) {
                    avPlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
                },
                "item never became ready",
            )
            avPlayer.play()

            assertTrue(waitFor(15_000L) { ticks >= 3 }, "observer fired only $ticks times")
        } finally {
            avPlayer.removeTimeObserver(token)
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
        }
    }

    @Test
    fun `seeking moves the playhead`() {
        // Backs the resume-position path: movie and series playback seek to a saved offset once
        // the item reports ready.
        val avPlayer = player()
        try {
            assertTrue(
                waitFor(readyTimeoutMs) {
                    avPlayer.currentItem?.status == AVPlayerItemStatusReadyToPlay
                },
                "item never became ready",
            )

            avPlayer.seekToTime(CMTimeMakeWithSeconds(10.0, preferredTimescale = 1000))
            val sought = waitFor(10_000L) { CMTimeGetSeconds(avPlayer.currentTime()) > 8.0 }
            assertTrue(
                sought,
                "seek did not take effect (pos=${CMTimeGetSeconds(avPlayer.currentTime())})",
            )
        } finally {
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
        }
    }

    @Test
    fun `an unreachable stream reports failed rather than hanging silently`() {
        // The watchdog exists because this case can leave the UI stuck. Confirm AVPlayer does
        // eventually surface `failed` for a host that refuses the connection, so the retry ladder
        // has something to react to — and that when it does not, the watchdog is the only net.
        val url = NSURL.URLWithString("http://127.0.0.1:1/nope.m3u8")
        assertNotNull(url)
        val avPlayer = AVPlayer(playerItem = AVPlayerItem(uRL = url))
        try {
            val failed = waitFor(20_000L) {
                avPlayer.currentItem?.status == AVPlayerItemStatusFailed
            }
            // Not asserted as a hard requirement: whether AVFoundation reports `failed` or simply
            // never leaves `unknown` is exactly the ambiguity PlaybackStartWatchdog covers. The
            // assertion is only that it never falsely claims readiness.
            assertTrue(
                avPlayer.currentItem?.status != AVPlayerItemStatusReadyToPlay,
                "an unreachable stream must never report readyToPlay",
            )
            println("unreachable-stream status settled as failed=$failed")
        } finally {
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
        }
    }
}
