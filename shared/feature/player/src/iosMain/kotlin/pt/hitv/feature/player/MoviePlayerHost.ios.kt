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
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.feature.player.composables.MoviePlayerScreen
import pt.hitv.feature.player.movies.MoviePlayerViewModel
import pt.hitv.feature.player.AvFoundationSupport
import pt.hitv.feature.player.util.PlaybackStartWatchdog
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

    // Derive the stream id from the URL when the caller didn't supply one. The only call site
    // (MovieDetailVoyagerScreen) uses launchMoviePlayer's defaults, so this arrives as 0 —
    // which silently disabled BOTH position saving and resume on iOS, leaving Continue Watching
    // permanently empty. Android's MoviePlayerActivity:154-156 does the same derivation, so this
    // is parity rather than invention.
    val effectiveStreamId = remember(streamId, movieUrl) {
        if (streamId > 0) streamId else PlayerStreamIdExtractor.extract(movieUrl)
    }

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

    // Resume bookkeeping. `startPositionMs` arrives as 0 from the only call site, so the saved
    // position has to be read here — Android does the equivalent load in MoviePlayerActivity
    // before initialising the player. `resumeResolved` gates the seek so we never commit to
    // "start from 0" before the DB lookup has answered.
    var resumeTargetMs by remember { mutableLongStateOf(startPositionMs.coerceAtLeast(0L)) }
    var resumeResolved by remember { mutableStateOf(startPositionMs > 0L) }
    var resumed by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveStreamId) {
        if (!resumeResolved) {
            resumeTargetMs = if (effectiveStreamId > 0) {
                viewModel.getPlaybackPosition(effectiveStreamId) ?: 0L
            } else 0L
            resumeResolved = true
        }
    }
    // One-shot latch for the finish-save. Mirrors Android's Player.STATE_ENDED
    // handler (MoviePlayerActivity:840) which sets the position to 0 so the
    // row doesn't keep showing "continue watching" after the movie finishes.
    var finishSaved by remember { mutableStateOf(false) }
    // Retry counter for AVPlayerItemStatusFailed — matches the ChannelPlayerHost
    // retry logic. Without this, a transient network error on load permanently
    // stalls the UI because iOS has no auto-retry.
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 3
    // User-visible failure state. Previously a failed movie just stopped the spinner and
    // showed black — the original sets errorMessage and Toasts (MoviePlayerActivity.kt:944-950).
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Providers serve a movie in whatever container they stored it in, and the URL carries it:
    // `…/movie/user/pass/12345.mkv`. ExoPlayer has a Matroska extractor so the original never had
    // to care; AVFoundation does not, and simply fails. Detecting it up front turns a black screen
    // plus a 25-second watchdog timeout into an immediate, accurate message. See
    // AvFoundationSupport for why this is a deny-list.
    val unsupportedContainer = remember(movieUrl) {
        AvFoundationSupport.unsupportedContainerMessage(movieUrl)
    }
    LaunchedEffect(unsupportedContainer) {
        if (unsupportedContainer != null) errorMessage = unsupportedContainer
    }

    // See PlaybackStartWatchdog's KDoc: the periodic time observer below cannot detect an item
    // that fails during load, because it needs the timeline to advance to run at all.
    val startWatchdog = remember {
        PlaybackStartWatchdog(scope = coroutineScope) {
            isBuffering = false
            errorMessage = "The movie did not start playing. The stream may be unavailable."
        }
    }

    // Keep the screen awake for the duration of playback. Without this iOS dims and locks
    // mid-movie, since AVPlayerViewController embedded in a Compose surface does not get the
    // automatic idle-timer handling a full-screen AVPlayerViewController would.
    KeepScreenOnAndFullscreen()

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
                            // Seek once, and only after the saved-position lookup has resolved.
                            if (!resumed && resumeResolved) {
                                resumed = true
                                if (resumeTargetMs > 0L) {
                                    avPlayer.seekToTime(
                                        CMTimeMakeWithSeconds(
                                            resumeTargetMs / 1000.0,
                                            preferredTimescale = 1000
                                        )
                                    )
                                }
                            }
                            retryCount = 0
                            errorMessage = null
                            startWatchdog.cancel()
                        }
                        AVPlayerItemStatusFailed -> {
                            isBuffering = false
                            if (unsupportedContainer != null) {
                                // Retrying cannot help: the file is in a format AVFoundation has
                                // no decoder for. Say so instead of burning three attempts.
                                errorMessage = unsupportedContainer
                            } else if (retryCount < maxRetries) {
                                retryCount++
                                coroutineScope.launch {
                                    delay(1000L * retryCount)
                                    val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
                                    val normalized = MediaUrlNormalizer.normalize(movieUrl, outputFormat)
                                    val nsUrl = NSURL.URLWithString(normalized)
                                    if (nsUrl != null) {
                                        avPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
                                        avPlayer.play()
                                        startWatchdog.arm()
                                    }
                                }
                            } else {
                                // Retries exhausted — tell the user instead of showing black.
                                startWatchdog.cancel()
                                errorMessage = item.error?.localizedDescription
                                    ?: "Playback failed. Please try again."
                            }
                        }
                        else -> isBuffering = true
                    }
                    // End-of-movie detection. Using 99% instead of exact end because
                    // AVPlayer's `currentTime` can plateau a few hundred ms short of
                    // `duration`. Clamped by `finishSaved` so we only save 0 once.
                    if (!finishSaved && durationMs > 0L && currentPositionMs >= (durationMs * 99) / 100) {
                        finishSaved = true
                        if (effectiveStreamId > 0) viewModel.savePlaybackPosition(effectiveStreamId, 0L)
                    }
                }
                isPlaying = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
            }
        )
        avPlayer.play()
        startWatchdog.arm()
        onDispose {
            startWatchdog.cancel()
            token?.let { avPlayer.removeTimeObserver(it) }
            // Final position save (matches Android onPause/onDestroy).
            val finalPos = currentPositionMs
            if (finalPos > 0 && effectiveStreamId > 0) viewModel.savePlaybackPosition(effectiveStreamId, finalPos)
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
            try {
                AVAudioSession.sharedInstance().setActive(false, null)
            } catch (_: Throwable) {}
        }
    }

    // Periodic position save every 5 s (matches Android Handler-based save loop).
    LaunchedEffect(effectiveStreamId) {
        while (effectiveStreamId > 0) {
            delay(5_000L)
            val pos = currentPositionMs
            if (pos > 0) viewModel.savePlaybackPosition(effectiveStreamId, pos)
        }
    }

    MoviePlayerScreen(
        movieTitle = movieTitle,
        sleepTimerManager = sleepTimerManager,
        // `showsPlaybackControls = true` tells AVPlayerViewController to render
        // its native AVKit controller (scrubber, AirPlay, PiP, subtitles menu,
        // 15 s skip). That replaces the custom Compose seek bar / play-pause
        // / 10 s skip we used to draw, matching the original Android project
        // which uses PlayerView.useController = true.
        playerViewFactory = { mod ->
            AVPlayerSurface(
                player = avPlayer,
                aspectMode = aspectMode,
                modifier = mod,
                showsPlaybackControls = true,
            )
        },
        onBack = {
            // Final save before dismiss so the resume position is fresh even if
            // DisposableEffect.onDispose is delayed by Compose teardown.
            val pos = currentPositionMs
            if (pos > 0 && effectiveStreamId > 0) viewModel.savePlaybackPosition(effectiveStreamId, pos)
            onClose()
        },
        onAspectRatioToggle = { aspectMode = aspectMode.cycle() },
        onSleepTimerSelect = { sleepTimerManager.start(it) },
        onSleepTimerCancel = { sleepTimerManager.cancel() },
        errorMessage = errorMessage,
        onRetry = {
            errorMessage = null
            retryCount = 0
            isBuffering = true
            val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
            val normalized = MediaUrlNormalizer.normalize(movieUrl, outputFormat)
            NSURL.URLWithString(normalized)?.let { nsUrl ->
                avPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
                avPlayer.play()
                startWatchdog.arm()
            }
        },
        onDismissError = { errorMessage = null }
    )
}

/**
 * Presents [vc] from the top-most view controller.
 *
 * Resolves the host window through `connectedScenes` rather than `UIApplication.keyWindow`,
 * which is deprecated since iOS 13 and returns nil in states this app actually hits (during
 * scene activation, and while a modal is already up). The old `?: return` on a nil keyWindow
 * failed *silently* — the tap appeared to do nothing and no player ever opened.
 *
 * Prefers a window belonging to a foreground-active scene, then falls back to any connected
 * scene's window. Deliberately sticks to `connectedScenes` / `windows` / `rootViewController`,
 * whose Kotlin/Native cinterop mappings are unambiguous — this file cannot be compiled on a
 * Windows host, so anything subtler only fails in CI.
 */
internal fun presentFromTop(vc: UIViewController) {
    val rootVC = resolveRootViewController() ?: return
    var topVC = rootVC
    while (topVC.presentedViewController != null) topVC = topVC.presentedViewController!!
    topVC.presentViewController(vc, animated = true, completion = null)
}

private fun resolveRootViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
        .mapNotNull { it as? UIWindowScene }
        // Foreground-active scenes first; a backgrounded scene's window cannot present.
        .sortedByDescending { it.activationState == UISceneActivationStateForegroundActive }

    return scenes
        .flatMap { scene -> scene.windows.mapNotNull { it as? UIWindow } }
        .firstNotNullOfOrNull { it.rootViewController }
}

internal fun configureAudioSession() {
    try {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, null)
        session.setActive(true, null)
    } catch (_: Throwable) {}
}
