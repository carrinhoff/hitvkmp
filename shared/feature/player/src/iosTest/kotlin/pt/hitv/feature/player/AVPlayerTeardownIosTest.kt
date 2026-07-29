@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import kotlinx.cinterop.CValue
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Drives the AVPlayer teardown sequence the player hosts run in `DisposableEffect.onDispose`,
 * on a real iOS simulator.
 *
 * This is not a formality. `removeTimeObserver` throws `NSInvalidArgumentException` if handed a
 * token that was never added, was already removed, or belongs to a different player — and the
 * three hosts (`ChannelPlayerHost`, `MoviePlayerHost`, `SeriesPlayerHost`) each add an observer in
 * `DisposableEffect` and remove it on dispose, with a watchdog cancel and an item detach around
 * it. A mistake there is an uncaught ObjC exception that kills the app on *leaving* a player,
 * which is both easy to introduce and easy to miss, since it only fires on the way out.
 *
 * Type-checking cannot catch any of this; only executing it can.
 */
class AVPlayerTeardownIosTest {

    private val interval: CValue<CMTime>
        get() = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000)

    private fun playerWithItem(): AVPlayer {
        // A syntactically valid URL that will never load. The item's *load* outcome is irrelevant
        // here — teardown must be safe regardless, which is exactly the failing-stream case.
        val url = NSURL.URLWithString("http://127.0.0.1:1/never-resolves.m3u8")
        assertNotNull(url, "test URL should parse")
        return AVPlayer(playerItem = AVPlayerItem(uRL = url))
    }

    @Test
    fun `add then remove a periodic time observer`() {
        val player = playerWithItem()
        val token = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { _: CValue<CMTime> -> },
        )
        // Throws NSInvalidArgumentException if the token round-trip is wrong.
        player.removeTimeObserver(token)
    }

    @Test
    fun `full host teardown sequence in the order the hosts use`() {
        val player = playerWithItem()
        val token = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { _: CValue<CMTime> -> },
        )

        // Mirrors onDispose in the three hosts.
        player.removeTimeObserver(token)
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)

        assertNull(player.currentItem, "detaching the item should clear currentItem")
    }

    @Test
    fun `teardown is safe when the item never became ready`() {
        // The failing-stream path: the watchdog fires, the user backs out, and dispose runs while
        // the item is still stuck. Nothing here may throw.
        val player = playerWithItem()
        val token = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { _: CValue<CMTime> -> },
        )
        player.pause()
        player.removeTimeObserver(token)
        player.replaceCurrentItemWithPlayerItem(null)
    }

    @Test
    fun `replacing the current item mid-session is safe`() {
        // The retry path in every host: replace the item, keep the same observer token alive.
        val player = playerWithItem()
        val token = player.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { _: CValue<CMTime> -> },
        )

        val retryUrl = NSURL.URLWithString("http://127.0.0.1:1/retry.m3u8")
        assertNotNull(retryUrl)
        player.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = retryUrl))
        assertNotNull(player.currentItem, "retry should install a new item")

        // The token predates the replacement and must still be removable.
        player.removeTimeObserver(token)
        player.replaceCurrentItemWithPlayerItem(null)
    }

    @Test
    fun `several add-remove cycles on one player`() {
        // Channel switching adds and removes observers repeatedly on a single AVPlayer.
        val player = playerWithItem()
        repeat(5) {
            val token = player.addPeriodicTimeObserverForInterval(
                interval = interval,
                queue = null,
                usingBlock = { _: CValue<CMTime> -> },
            )
            player.removeTimeObserver(token)
        }
        player.replaceCurrentItemWithPlayerItem(null)
    }

    @Test
    fun `detaching an already-detached item is safe`() {
        // Compose can run dispose paths more than once in teardown races.
        val player = playerWithItem()
        player.replaceCurrentItemWithPlayerItem(null)
        player.replaceCurrentItemWithPlayerItem(null)
        assertNull(player.currentItem)
    }
}
