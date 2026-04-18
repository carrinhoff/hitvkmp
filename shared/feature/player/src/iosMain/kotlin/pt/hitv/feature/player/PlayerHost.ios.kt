@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class
)

package pt.hitv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.CValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackBufferEmpty
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import pt.hitv.feature.player.media.MediaItem

/**
 * iOS actual for [PlayerHost].
 *
 * ## SPIKE decision — single [AVPlayer] + `replaceCurrentItem` over `AVQueuePlayer`
 *
 * `AVQueuePlayer` is forward-only: it exposes `advanceToNextItem()` but has no
 * `seekToPreviousItem`, and constructing it with N items then jumping to item K at
 * position P requires `removeAllItems()` + re-inserting from K onward — equivalent to
 * just building a fresh [AVPlayerItem] for K and calling [replaceCurrentItemWithPlayerItem].
 * Faking reverse navigation on top of `AVQueuePlayer` is strictly more code with no
 * upside. A plain [AVPlayer] + manual index tracking plays exactly the role that
 * ExoPlayer's internal playlist plays on Android, keeps prev/next symmetric, and makes
 * auto-advance a one-line notification-handler. See `switchToCurrentItem`.
 *
 * ## Threading
 *
 * All AVFoundation callbacks (`addPeriodicTimeObserverForInterval` with queue=null runs
 * on the main queue, and `addObserverForName` with `NSOperationQueue.mainQueue`) are
 * dispatched on the main thread. Writes to [MutableStateFlow.value] are thread-safe so
 * we don't marshal further via `withContext(Dispatchers.Main)`.
 *
 * ## Lifecycle
 *
 * A single [release] tears down:
 * - the time observer token,
 * - every notification observer collected in [notificationObservers],
 * - the current AVPlayerItem (nil-out via `replaceCurrentItemWithPlayerItem(null)`),
 * - the AVAudioSession (deactivates to free the audio route).
 *
 * ## Buffering / ready state
 *
 * Rather than subclass `NSObject` to implement `observeValueForKeyPath` (required for
 * Kotlin/Native KVO on foreign objects — ugly and error-prone), we piggy-back all
 * status derivation on the periodic time observer. At 0.5 s cadence the lag is
 * invisible to the overlay — matches ExoPlayer's coarse `onPlayWhenReadyChanged`
 * listener cadence.
 */
actual class PlayerHost actual constructor(
    config: PlayerConfig,
    items: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long
) {

    private val items: List<MediaItem> = items
    private var currentIdx: Int = startIndex.coerceIn(0, maxOf(0, items.size - 1))

    // StateFlows exposed via the shared PlayerController contract.
    private val isPlayingFlow = MutableStateFlow(false)
    private val positionFlow = MutableStateFlow(0L)
    private val durationFlow = MutableStateFlow(0L)
    private val stateFlow = MutableStateFlow(PlaybackState.Idle)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val currentIndexFlowInternal = MutableStateFlow(currentIdx)

    actual val currentIndexFlow: StateFlow<Int> = currentIndexFlowInternal.asStateFlow()

    private val avPlayer: AVPlayer
    private var avPlayerViewController: AVPlayerViewController? = null
    private var timeObserverToken: Any? = null
    private val notificationObservers = mutableListOf<Any>()
    private var released = false

    private var currentAspectMode: PlayerAspectMode = config.initialAspectMode

    /**
     * iOS-only callback hook invoked when the app transitions to background. The
     * launcher wires this to the view-model's save-position call; keeping the callback
     * owner outside the PlayerHost avoids coupling shared code to VM types.
     *
     * Not part of the expect contract (Android uses Activity.onPause for the same
     * purpose).
     */
    var onBackgroundSavePosition: (() -> Unit)? = null

    actual val controller: PlayerController = object : PlayerController {
        override val isPlaying: StateFlow<Boolean> = isPlayingFlow.asStateFlow()
        override val currentPositionMs: StateFlow<Long> = positionFlow.asStateFlow()
        override val durationMs: StateFlow<Long> = durationFlow.asStateFlow()
        override val playbackState: StateFlow<PlaybackState> = stateFlow.asStateFlow()
        override val error: StateFlow<String?> = errorFlow.asStateFlow()

        override fun play() { avPlayer.play() }
        override fun pause() { avPlayer.pause() }
        override fun seekTo(positionMs: Long) {
            val cm = CMTimeMakeWithSeconds(positionMs / 1000.0, preferredTimescale = 1000)
            avPlayer.seekToTime(cm)
        }
        override fun release() = this@PlayerHost.release()
    }

    init {
        configureAudioSession()

        val firstItem = buildPlayerItem(this.items.getOrNull(currentIdx))
        avPlayer = AVPlayer(playerItem = firstItem)
        avPlayer.applyVodBufferProfile()

        // AVPlayer accepts seeks before the item reaches `readyToPlay` — it queues the
        // seek until the item's timeline is loaded. Matches how ExoPlayer handles
        // resume-position at startup.
        if (startPositionMs > 0 && firstItem != null) {
            avPlayer.seekToTime(
                CMTimeMakeWithSeconds(startPositionMs / 1000.0, preferredTimescale = 1000)
            )
        }

        installPeriodicTimeObserver()
        installPlayToEndObserver()
        installInterruptionObserver()
        installBackgroundObserver()

        avPlayer.play()
    }

    actual fun currentIndex(): Int = currentIdx

    actual fun seekToPrevious() {
        if (currentIdx <= 0) return
        currentIdx -= 1
        switchToCurrentItem()
    }

    actual fun seekToNext() {
        if (currentIdx >= items.size - 1) return
        currentIdx += 1
        switchToCurrentItem()
    }

    actual fun setAspectMode(mode: PlayerAspectMode) {
        currentAspectMode = mode
        // When called before the Surface is composed (e.g. synchronously from the
        // launcher), buffer the value in `currentAspectMode`; the Surface factory
        // applies it on creation.
        avPlayerViewController?.videoGravity = mode.toVideoGravity()
    }

    @Composable
    actual fun Surface(modifier: Modifier) {
        UIKitViewController(
            modifier = modifier,
            factory = {
                val vc = AVPlayerViewController()
                vc.player = avPlayer
                vc.showsPlaybackControls = true
                vc.videoGravity = currentAspectMode.toVideoGravity()
                avPlayerViewController = vc
                vc
            }
        )
    }

    actual fun release() {
        // Idempotent — the launcher wires both a sleep-timer callback and a
        // DisposableEffect.onDispose that may race to call release(), and Android's
        // actual is explicitly idempotent.
        if (released) return
        released = true

        timeObserverToken?.let { token ->
            avPlayer.removeTimeObserver(token)
            timeObserverToken = null
        }

        val center = NSNotificationCenter.defaultCenter
        notificationObservers.forEach { token -> center.removeObserver(token) }
        notificationObservers.clear()

        avPlayer.pause()
        avPlayer.replaceCurrentItemWithPlayerItem(null)
        avPlayerViewController?.player = null
        avPlayerViewController = null

        try {
            AVAudioSession.sharedInstance().setActive(false, null)
        } catch (_: Throwable) {
            // Best-effort teardown; if the session refuses to deactivate we don't
            // propagate — the next player's configureAudioSession() will re-activate.
        }
    }

    // ---- private helpers ----

    private fun configureAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (_: Throwable) {
            // Ignored — audio-session failure leaves playback silent but isn't fatal.
        }
    }

    /**
     * Build an AVPlayerItem for the given shared [MediaItem]. URLs are assumed to be
     * pre-normalized by the launcher via [MediaUrlNormalizer]. Returns null if the URL
     * is malformed — AVPlayer handles a nil currentItem gracefully (no playback).
     */
    private fun buildPlayerItem(item: MediaItem?): AVPlayerItem? {
        val url = item?.url?.let { NSURL.URLWithString(it) } ?: return null
        return AVPlayerItem(uRL = url).apply { applyVodBufferProfile() }
    }

    /**
     * Replace the current AVPlayerItem with the one at [currentIdx], reset transient
     * position/duration state, and kick playback. Called from [seekToPrevious],
     * [seekToNext], and the play-to-end auto-advance handler.
     */
    private fun switchToCurrentItem() {
        val newItem = buildPlayerItem(items.getOrNull(currentIdx))
        avPlayer.replaceCurrentItemWithPlayerItem(newItem)
        positionFlow.value = 0L
        durationFlow.value = 0L
        stateFlow.value = PlaybackState.Buffering
        currentIndexFlowInternal.value = currentIdx
        avPlayer.play()
    }

    /**
     * Single periodic observer that drives almost every StateFlow.
     *
     * Running every 0.5 s (the same cadence the overlay reads at) keeps load low and
     * matches ExoPlayer's listener cadence. We derive:
     * - position from the [CMTime] block parameter;
     * - duration from `currentItem.duration` once it's non-NaN (it's indefinite before
     *   `readyToPlay`);
     * - playback/ready/buffering from `status` + `playbackBufferEmpty` +
     *   `playbackLikelyToKeepUp`;
     * - isPlaying from `timeControlStatus`.
     */
    private fun installPeriodicTimeObserver() {
        val interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000)
        timeObserverToken = avPlayer.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { time: CValue<CMTime> ->
                updateFlowsFromPlayer(time)
            }
        )
    }

    private fun updateFlowsFromPlayer(time: CValue<CMTime>) {
        val seconds = CMTimeGetSeconds(time)
        if (!seconds.isNaN() && seconds >= 0.0) {
            positionFlow.value = (seconds * 1000).toLong()
        }

        val item = avPlayer.currentItem
        if (item != null) {
            val durSeconds = CMTimeGetSeconds(item.duration)
            if (!durSeconds.isNaN() && durSeconds > 0.0) {
                val durMs = (durSeconds * 1000).toLong()
                if (durationFlow.value != durMs) durationFlow.value = durMs
            }

            when (item.status) {
                AVPlayerItemStatusReadyToPlay -> {
                    if (item.playbackBufferEmpty) {
                        stateFlow.value = PlaybackState.Buffering
                    } else if (item.playbackLikelyToKeepUp) {
                        stateFlow.value = PlaybackState.Ready
                    }
                }
                AVPlayerItemStatusFailed -> {
                    stateFlow.value = PlaybackState.Error
                    errorFlow.value = item.error?.localizedDescription
                }
                else -> {
                    // AVPlayerItemStatusUnknown — still preparing.
                    stateFlow.value = PlaybackState.Buffering
                }
            }
        }

        isPlayingFlow.value = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
    }

    private fun installPlayToEndObserver() {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue(),
            usingBlock = { _: NSNotification? ->
                // Auto-advance if there's a next item; else surface "Ended" so the
                // overlay / launcher can react.
                if (currentIdx < items.size - 1) {
                    currentIdx += 1
                    switchToCurrentItem()
                } else {
                    stateFlow.value = PlaybackState.Ended
                }
            }
        )
        notificationObservers.add(token)
    }

    private fun installInterruptionObserver() {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVAudioSessionInterruptionNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue(),
            usingBlock = { notification: NSNotification? ->
                val userInfo = notification?.userInfo ?: return@addObserverForName
                val type = (userInfo[AVAudioSessionInterruptionTypeKey] as? NSNumber)
                    ?.unsignedLongValue() ?: return@addObserverForName
                when (type) {
                    AVAudioSessionInterruptionTypeBegan -> {
                        avPlayer.pause()
                        isPlayingFlow.value = false
                    }
                    AVAudioSessionInterruptionTypeEnded -> {
                        val opts = (userInfo[AVAudioSessionInterruptionOptionKey] as? NSNumber)
                            ?.unsignedLongValue() ?: 0uL
                        if ((opts and AVAudioSessionInterruptionOptionShouldResume) != 0uL) {
                            avPlayer.play()
                        }
                    }
                }
            }
        )
        notificationObservers.add(token)
    }

    private fun installBackgroundObserver() {
        val token = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue(),
            usingBlock = { _: NSNotification? ->
                onBackgroundSavePosition?.invoke()
            }
        )
        notificationObservers.add(token)
    }
}
