@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class
)

package pt.hitv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
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
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.feature.player.composables.MoviePlayerScreen
import pt.hitv.feature.player.movies.MoviePlayerViewModel
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * iOS host for the movie player. Mirrors `MoviePlayerActivity` on Android: thin
 * wrapper that owns the native player (AVPlayer) and supplies the surface to the
 * shared `MoviePlayerScreen` overlay. Position saving every 5 s + on dispose;
 * resume from [startPositionMs] once the item reaches `readyToPlay`.
 */
fun presentMoviePlayer(
    movieUrl: String,
    movieTitle: String,
    streamId: Int,
    startPositionMs: Long
) {
    configureAudioSession()
    val vcHolder = arrayOf<UIViewController?>(null)
    val composeVC: UIViewController = ComposeUIViewController {
        val dismiss: () -> Unit = remember { { vcHolder[0]?.dismissViewControllerAnimated(true, null) } }
        MoviePlayerHostContent(
            movieUrl = movieUrl,
            movieTitle = movieTitle,
            streamId = streamId,
            startPositionMs = startPositionMs,
            onClose = dismiss
        )
    }
    vcHolder[0] = composeVC
    presentFromTop(composeVC)
}

@Composable
private fun MoviePlayerHostContent(
    movieUrl: String,
    movieTitle: String,
    streamId: Int,
    startPositionMs: Long,
    onClose: () -> Unit
) {
    val viewModel: MoviePlayerViewModel = koinInject()
    val preferencesHelper: PreferencesHelper = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val sleepTimerManager = remember { SleepTimerManager(coroutineScope) { onClose() } }
    var aspectMode by remember { mutableStateOf(PlayerAspectMode.Fit) }

    val avPlayer = remember {
        val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        val normalized = MediaUrlNormalizer.normalize(movieUrl, outputFormat)
        val nsUrl = NSURL.URLWithString(normalized)
        if (nsUrl != null) AVPlayer(playerItem = AVPlayerItem(uRL = nsUrl)) else AVPlayer()
    }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(startPositionMs.coerceAtLeast(0L)) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var resumed by remember { mutableStateOf(startPositionMs <= 0L) }
    // One-shot latch for the finish-save. Mirrors Android's Player.STATE_ENDED
    // handler (MoviePlayerActivity:840) which sets the position to 0 so the
    // row doesn't keep showing "continue watching" after the movie finishes.
    var finishSaved by remember { mutableStateOf(false) }
    // Retry counter for AVPlayerItemStatusFailed — matches the ChannelPlayerHost
    // retry logic. Without this, a transient network error on load permanently
    // stalls the UI because iOS has no auto-retry.
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 3

    DisposableEffect(avPlayer) {
        val token: Any? = avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000),
            queue = null,
            usingBlock = { time: CValue<CMTime> ->
                val seconds = CMTimeGetSeconds(time)
                if (!seconds.isNaN() && seconds >= 0.0) {
                    currentPositionMs = (seconds * 1000).toLong()
                }
                val item = avPlayer.currentItem
                if (item != null) {
                    val durSec = CMTimeGetSeconds(item.duration)
                    if (!durSec.isNaN() && durSec > 0.0) durationMs = (durSec * 1000).toLong()
                    when (item.status) {
                        AVPlayerItemStatusReadyToPlay -> {
                            isBuffering = item.playbackBufferEmpty || !item.playbackLikelyToKeepUp
                            if (!resumed) {
                                resumed = true
                                avPlayer.seekToTime(
                                    CMTimeMakeWithSeconds(startPositionMs / 1000.0, preferredTimescale = 1000)
                                )
                            }
                            retryCount = 0
                        }
                        AVPlayerItemStatusFailed -> {
                            isBuffering = false
                            if (retryCount < maxRetries) {
                                retryCount++
                                coroutineScope.launch {
                                    delay(1000L * retryCount)
                                    val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
                                    val normalized = MediaUrlNormalizer.normalize(movieUrl, outputFormat)
                                    val nsUrl = NSURL.URLWithString(normalized)
                                    if (nsUrl != null) {
                                        avPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
                                        avPlayer.play()
                                    }
                                }
                            }
                        }
                        else -> isBuffering = true
                    }
                    // End-of-movie detection. Using 99% instead of exact end because
                    // AVPlayer's `currentTime` can plateau a few hundred ms short of
                    // `duration`. Clamped by `finishSaved` so we only save 0 once.
                    if (!finishSaved && durationMs > 0L && currentPositionMs >= (durationMs * 99) / 100) {
                        finishSaved = true
                        if (streamId > 0) viewModel.savePlaybackPosition(streamId, 0L)
                    }
                }
                isPlaying = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
            }
        )
        avPlayer.play()
        onDispose {
            token?.let { avPlayer.removeTimeObserver(it) }
            // Final position save (matches Android onPause/onDestroy).
            val finalPos = currentPositionMs
            if (finalPos > 0 && streamId > 0) viewModel.savePlaybackPosition(streamId, finalPos)
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
            try {
                AVAudioSession.sharedInstance().setActive(false, null)
            } catch (_: Throwable) {}
        }
    }

    // Periodic position save every 5 s (matches Android Handler-based save loop).
    LaunchedEffect(streamId) {
        while (streamId > 0) {
            delay(5_000L)
            val pos = currentPositionMs
            if (pos > 0) viewModel.savePlaybackPosition(streamId, pos)
        }
    }

    MoviePlayerScreen(
        movieTitle = movieTitle,
        isBuffering = isBuffering,
        isPlaying = isPlaying,
        currentPositionMs = currentPositionMs,
        durationMs = durationMs,
        sleepTimerManager = sleepTimerManager,
        playerViewFactory = { mod -> AVPlayerSurface(player = avPlayer, aspectMode = aspectMode, modifier = mod) },
        onBack = {
            // Final save before dismiss so the resume position is fresh even if
            // DisposableEffect.onDispose is delayed by Compose teardown.
            val pos = currentPositionMs
            if (pos > 0 && streamId > 0) viewModel.savePlaybackPosition(streamId, pos)
            onClose()
        },
        onPlayPause = { if (isPlaying) avPlayer.pause() else avPlayer.play() },
        onSeekTo = { posMs ->
            avPlayer.seekToTime(CMTimeMakeWithSeconds(posMs / 1000.0, preferredTimescale = 1000))
        },
        onAspectRatioToggle = { aspectMode = aspectMode.cycle() },
        onSleepTimerSelect = { sleepTimerManager.start(it) },
        onSleepTimerCancel = { sleepTimerManager.cancel() }
    )
}

internal fun presentFromTop(vc: UIViewController) {
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var topVC = rootVC
    while (topVC.presentedViewController != null) topVC = topVC.presentedViewController!!
    topVC.presentViewController(vc, animated = true, completion = null)
}

internal fun configureAudioSession() {
    try {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
    } catch (_: Throwable) {}
}
