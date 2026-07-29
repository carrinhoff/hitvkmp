@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlinx.cinterop.BetaInteropApi::class
)

package pt.hitv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.AVFAudio.AVAudioSession
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
import platform.UIKit.UIViewController
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.model.seriesInfo.Episode
import pt.hitv.feature.player.composables.SeriesPlayerScreen
import pt.hitv.feature.player.series.SeriesPlayerViewModel
import pt.hitv.feature.player.AvFoundationSupport
import pt.hitv.feature.player.util.PlaybackStartWatchdog
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * iOS host for the series player. Mirrors `SeriesPlayerActivity`: loads episodes via
 * [SeriesPlayerViewModel.loadEpisodes], builds the URL for each episode from
 * `${host}series/$user/$pass/${id}.${containerExtension}`, navigates prev / next,
 * resumes per-episode positions, saves on tick + on dispose.
 */
fun presentSeriesPlayer(
    seriesId: String,
    seasonNumber: Int,
    episodeIndex: Int
) {
    configureAudioSession()
    val vcHolder = arrayOf<UIViewController?>(null)
    val composeVC: UIViewController = ComposeUIViewController {
        val dismiss: () -> Unit = remember { { vcHolder[0]?.dismissViewControllerAnimated(true, null) } }
        SeriesPlayerHostContent(
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            initialEpisodeIndex = episodeIndex,
            onClose = dismiss
        )
    }
    vcHolder[0] = composeVC
    presentFromTop(composeVC)
}

@Composable
private fun SeriesPlayerHostContent(
    seriesId: String,
    seasonNumber: Int,
    initialEpisodeIndex: Int,
    onClose: () -> Unit
) {
    val viewModel: SeriesPlayerViewModel = koinInject()
    val preferencesHelper: PreferencesHelper = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val sleepTimerManager = remember { SleepTimerManager(coroutineScope) { onClose() } }
    var aspectMode by remember { mutableStateOf(PlayerAspectMode.Fit) }
    val avPlayer = remember { AVPlayer() }

    var isBuffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentEpisodeIndex by remember { mutableIntStateOf(initialEpisodeIndex) }
    var episodeTitle by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val episodes = uiState.episodes

    // User-visible failure state — previously an episode that failed to load showed black.
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // See PlaybackStartWatchdog's KDoc: the periodic time observer below cannot detect an item
    // that fails during load, because it needs the timeline to advance to run at all.
    val startWatchdog = remember {
        PlaybackStartWatchdog(scope = coroutineScope) {
            isBuffering = false
            errorMessage = "The episode did not start playing. The stream may be unavailable."
        }
    }

    // Keep the screen awake for the duration of playback (iOS otherwise dims and locks
    // mid-episode — AVPlayerViewController embedded in Compose doesn't get this for free).
    KeepScreenOnAndFullscreen()

    // Trigger episode load + start playback once they arrive (matches Android Activity).
    LaunchedEffect(seriesId, seasonNumber) {
        viewModel.loadEpisodes(seriesId, seasonNumber)
        viewModel.uiState.first { !it.isLoading && it.episodes.isNotEmpty() }
        playEpisode(
            avPlayer = avPlayer,
            preferencesHelper = preferencesHelper,
            episodes = viewModel.uiState.value.episodes,
            index = initialEpisodeIndex,
            onTitle = { episodeTitle = it },
            onSeekTarget = { /* resume handled by periodic-observer one-shot below */ }
        )
        startWatchdog.arm()
    }

    // Resume to saved position once readyToPlay is reached for the current item —
    // AVPlayer doesn't honour pre-`readyToPlay` seeks reliably.
    var pendingResumeMs by remember { mutableLongStateOf(initialResumeForIndex(episodes, initialEpisodeIndex)) }
    var resumed by remember { mutableStateOf(pendingResumeMs <= 0L) }
    // Retry counter for AVPlayerItemStatusFailed, mirrors ChannelPlayerHost.
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 3

    DisposableEffect(avPlayer) {
        val token: Any? = avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000),
            queue = null,
            usingBlock = { time: CValue<CMTime> ->
                val seconds = CMTimeGetSeconds(time)
                if (!seconds.isNaN() && seconds >= 0.0) currentPositionMs = (seconds * 1000).toLong()
                val item = avPlayer.currentItem
                if (item != null) {
                    val durSec = CMTimeGetSeconds(item.duration)
                    if (!durSec.isNaN() && durSec > 0.0) durationMs = (durSec * 1000).toLong()
                    when (item.status) {
                        AVPlayerItemStatusReadyToPlay -> {
                            isBuffering = item.playbackBufferEmpty || !item.playbackLikelyToKeepUp
                            if (!resumed && pendingResumeMs > 0L) {
                                resumed = true
                                avPlayer.seekToTime(
                                    CMTimeMakeWithSeconds(pendingResumeMs / 1000.0, preferredTimescale = 1000)
                                )
                            }
                            retryCount = 0
                            errorMessage = null
                            startWatchdog.cancel()
                        }
                        AVPlayerItemStatusFailed -> {
                            isBuffering = false
                            // Episodes carry the provider's container just like movies do, and
                            // AVFoundation cannot open Matroska or AVI. Retrying spends three
                            // attempts on something that can never decode — say what is actually
                            // wrong instead. See AvFoundationSupport.
                            val unsupported = episodes.getOrNull(currentEpisodeIndex)
                                ?.containerExtension
                                ?.let { AvFoundationSupport.unsupportedContainerMessage("x.$it") }
                            if (unsupported != null) {
                                startWatchdog.cancel()
                                errorMessage = unsupported
                            } else if (retryCount < maxRetries) {
                                retryCount++
                                coroutineScope.launch {
                                    delay(1000L * retryCount)
                                    playEpisode(avPlayer, preferencesHelper, episodes, currentEpisodeIndex, { episodeTitle = it }, {})
                                    startWatchdog.arm()
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
                }
                isPlaying = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying
            }
        )
        onDispose {
            startWatchdog.cancel()
            token?.let { avPlayer.removeTimeObserver(it) }
            // Final position + duration save (matches Android onPause).
            saveCurrent(viewModel, episodes, currentEpisodeIndex, currentPositionMs, durationMs)
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
            try { AVAudioSession.sharedInstance().setActive(false, null) } catch (_: Throwable) {}
        }
    }

    // Periodic save every 1 s, matching the original Android handler interval
    // (SeriesPlayerManager.kt:75). The port was previously at 5s which lost
    // up to five seconds of progress on app foreground/background transitions.
    LaunchedEffect(seriesId, seasonNumber) {
        while (true) {
            delay(1_000L)
            saveCurrent(viewModel, viewModel.uiState.value.episodes, currentEpisodeIndex, currentPositionMs, durationMs)
        }
    }

    val hasNext = currentEpisodeIndex < episodes.size - 1
    val hasPrev = currentEpisodeIndex > 0

    SeriesPlayerScreen(
        episodeTitle = episodeTitle,
        hasNextEpisode = hasNext,
        hasPreviousEpisode = hasPrev,
        sleepTimerManager = sleepTimerManager,
        // Native AVKit controls handle scrub / play-pause / AirPlay / PiP /
        // subtitles — matches the original Android project using ExoPlayer's
        // PlayerView.useController. The Compose top bar only renders the
        // chrome on top (back / title / prev / next / aspect / sleep).
        playerViewFactory = { mod ->
            AVPlayerSurface(
                player = avPlayer,
                aspectMode = aspectMode,
                modifier = mod,
                showsPlaybackControls = true,
            )
        },
        onBack = {
            saveCurrent(viewModel, episodes, currentEpisodeIndex, currentPositionMs, durationMs)
            onClose()
        },
        onNextEpisode = {
            if (hasNext) {
                saveCurrent(viewModel, episodes, currentEpisodeIndex, currentPositionMs, durationMs)
                val nextIdx = currentEpisodeIndex + 1
                currentEpisodeIndex = nextIdx
                pendingResumeMs = initialResumeForIndex(episodes, nextIdx)
                resumed = pendingResumeMs <= 0L
                errorMessage = null
                retryCount = 0
                coroutineScope.launch {
                    playEpisode(avPlayer, preferencesHelper, episodes, nextIdx, { episodeTitle = it }, {})
                    startWatchdog.arm()
                }
            }
        },
        onPreviousEpisode = {
            if (hasPrev) {
                saveCurrent(viewModel, episodes, currentEpisodeIndex, currentPositionMs, durationMs)
                val prevIdx = currentEpisodeIndex - 1
                currentEpisodeIndex = prevIdx
                pendingResumeMs = initialResumeForIndex(episodes, prevIdx)
                resumed = pendingResumeMs <= 0L
                errorMessage = null
                retryCount = 0
                coroutineScope.launch {
                    playEpisode(avPlayer, preferencesHelper, episodes, prevIdx, { episodeTitle = it }, {})
                    startWatchdog.arm()
                }
            }
        },
        onAspectRatioToggle = { aspectMode = aspectMode.cycle() },
        onSleepTimerSelect = { sleepTimerManager.start(it) },
        onSleepTimerCancel = { sleepTimerManager.cancel() },
        errorMessage = errorMessage,
        onRetry = {
            errorMessage = null
            retryCount = 0
            isBuffering = true
            coroutineScope.launch {
                playEpisode(avPlayer, preferencesHelper, episodes, currentEpisodeIndex, { episodeTitle = it }, {})
                startWatchdog.arm()
            }
        },
        onDismissError = { errorMessage = null }
    )
}

private fun playEpisode(
    avPlayer: AVPlayer,
    preferencesHelper: PreferencesHelper,
    episodes: List<Episode>,
    index: Int,
    onTitle: (String) -> Unit,
    onSeekTarget: (Long) -> Unit
) {
    if (index !in episodes.indices) return
    val episode = episodes[index]
    onTitle("${episode.episodeNum}. ${episode.title ?: "Episode ${episode.episodeNum}"}")

    val host = preferencesHelper.getHostUrl()
    val user = preferencesHelper.getUsername()
    val pass = preferencesHelper.getPassword()
    val ext = episode.containerExtension ?: "m3u8"
    val rawUrl = "${host}series/$user/$pass/${episode.id}.$ext"
    val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
    val normalized = MediaUrlNormalizer.normalize(rawUrl, outputFormat)
    val nsUrl = NSURL.URLWithString(normalized) ?: return
    avPlayer.replaceCurrentItemWithPlayerItem(AVPlayerItem(uRL = nsUrl))
    avPlayer.play()
    onSeekTarget(episode.info?.playbackPosition ?: 0L)
}

private fun initialResumeForIndex(episodes: List<Episode>, index: Int): Long =
    episodes.getOrNull(index)?.info?.playbackPosition ?: 0L

private fun saveCurrent(
    viewModel: SeriesPlayerViewModel,
    episodes: List<Episode>,
    index: Int,
    posMs: Long,
    durMs: Long
) {
    val episode = episodes.getOrNull(index) ?: return
    if (episode.id.isEmpty()) return
    if (posMs > 0) viewModel.savePlaybackPosition(posMs, episode.id)
    if (durMs > 0) viewModel.saveEpisodeDuration(durMs.toDouble(), episode.id)
}
