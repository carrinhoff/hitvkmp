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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.setActive
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
import platform.AVFoundation.playbackBufferEmpty
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSURL
import platform.UIKit.UIViewController
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.feature.player.composables.ChannelPlayerScreen
import pt.hitv.feature.player.helpers.ChannelNavigationHelper
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * iOS host for the channel player. Mirrors the role of `ChannelPlayerActivity` on
 * Android: a thin wrapper that owns the native player (AVPlayer here, ExoPlayer there),
 * supplies the video surface to the shared [ChannelPlayerScreen] via the
 * `playerViewFactory` slot, and bridges [LivePlayerViewModel] state changes to player
 * actions (URL → replaceCurrentItem; AVPlayerItem.status → playback state; aspect mode
 * → AVPlayerLayer.videoGravity).
 *
 * Presented modally via [ComposeUIViewController] from the top-most view controller —
 * matches the existing iOS launch pattern (the bare `presentAVPlayer` call this
 * replaces) and avoids depending on a Voyager Navigator handle from a top-level fun.
 */
fun presentChannelPlayer(
    url: String,
    name: String,
    titleEpg: String?,
    descEpg: String?,
    logoUrl: String?,
    licenseKey: String?,
    categoryTitle: String?,
    categoryId: Int
) {
    configureAudioSession()

    // Capture the VC reference in a holder so the @Composable content can dismiss it
    // (sleep-timer expiry, back button, error retry-cancel). The lambda closes over
    // `vcHolder`, which is mutated to the actual VC AFTER ComposeUIViewController()
    // builds it but BEFORE composition runs.
    val vcHolder = arrayOf<UIViewController?>(null)

    val composeVC: UIViewController = ComposeUIViewController {
        val dismiss: () -> Unit = remember { { vcHolder[0]?.dismissViewControllerAnimated(true, null) } }
        ChannelPlayerHostContent(
            initialUrl = url,
            initialName = name,
            categoryTitle = categoryTitle,
            categoryId = categoryId,
            licenseKey = licenseKey,
            onClose = dismiss
        )
    }
    vcHolder[0] = composeVC
    presentFromTop(composeVC)
}

@Composable
private fun ChannelPlayerHostContent(
    initialUrl: String,
    initialName: String,
    categoryTitle: String?,
    categoryId: Int,
    licenseKey: String?,
    onClose: () -> Unit
) {
    val viewModel: LivePlayerViewModel = koinInject()
    val preferencesHelper: PreferencesHelper = koinInject()
    val coroutineScope = rememberCoroutineScope()

    val sleepTimerManager = remember { SleepTimerManager(coroutineScope) { onClose() } }

    // Seed VM exactly once. Mirrors Android `viewModel.initFromArgs(...)` in onCreate.
    LaunchedEffect(Unit) {
        viewModel.initFromArgs(
            url = initialUrl,
            name = initialName,
            position = "",
            categoryTitle = categoryTitle,
            categoryId = categoryId,
            licenseKey = licenseKey,
            isPiPSupported = false, // PiP via modal AVPlayer not supported on iOS without custom plumbing
            isTvDevice = false
        )
        viewModel.fetchChannelsFromDB()
        viewModel.fetchChannelCategories()
        viewModel.getFavorites()
    }

    val uiState by viewModel.uiState.collectAsState()

    // ---- Native player ----
    val avPlayer = remember { AVPlayer() }

    // Drive AVPlayer.replaceCurrentItem from VM state. Same shape as
    // `ChannelPlayerActivity.observePlaybackTriggers` → `startPlayback(url)`.
    var retryCount by remember { mutableStateOf(0) }
    val maxRetries = 3
    LaunchedEffect(Unit) {
        viewModel.uiState.map { it.currentChannelUrl }.distinctUntilChanged().collect { url ->
            if (url.isNotEmpty()) {
                retryCount = 0
                startPlayback(avPlayer, url, preferencesHelper)
                viewModel.setPlaybackBuffering()
            }
        }
    }

    // Aspect mode → AVPlayerLayer.videoGravity. Captured in a State so the surface's
    // update lambda re-fires on change.
    val currentAspectMode = uiState.currentAspectMode

    // Periodic time observer: derives buffering/ready/failed and triggers retry on
    // failure. Same cadence as PlayerHost.ios (0.5 s). While in catch-up mode,
    // the same tick pumps position + duration back into the ViewModel so the
    // shared slider stays in sync with AVPlayer.
    DisposableEffect(avPlayer) {
        val token: Any? = avPlayer.addPeriodicTimeObserverForInterval(
            interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000),
            queue = null,
            usingBlock = { _: CValue<CMTime> ->
                val item = avPlayer.currentItem ?: return@addPeriodicTimeObserverForInterval
                when (item.status) {
                    AVPlayerItemStatusReadyToPlay -> {
                        if (item.playbackBufferEmpty || !item.playbackLikelyToKeepUp) {
                            viewModel.setPlaybackBuffering()
                        } else {
                            viewModel.setPlaybackReady()
                            retryCount = 0
                        }
                    }
                    AVPlayerItemStatusFailed -> {
                        if (retryCount < maxRetries) {
                            retryCount++
                            viewModel.setAutoRetrying(retryCount, maxRetries)
                            coroutineScope.launch {
                                delay(1000L * retryCount)
                                startPlayback(avPlayer, viewModel.uiState.value.currentChannelUrl, preferencesHelper)
                            }
                        } else {
                            viewModel.setPlaybackError(
                                item.error?.localizedDescription ?: "Playback error",
                                retryCount,
                                maxRetries
                            )
                        }
                    }
                    else -> viewModel.setPlaybackBuffering()
                }

                // Catch-up progress — only pump when active to avoid churn on live.
                if (viewModel.uiState.value.catchUpState.isActive) {
                    val posSec = CMTimeGetSeconds(avPlayer.currentTime())
                    val durSec = CMTimeGetSeconds(item.duration)
                    val posMs = if (posSec.isFinite() && posSec >= 0.0) (posSec * 1000.0).toLong() else 0L
                    val durMs = if (durSec.isFinite() && durSec > 0.0) (durSec * 1000.0).toLong() else 0L
                    viewModel.updateCatchUpPosition(posMs, durMs)
                }
            }
        )
        onDispose {
            token?.let { avPlayer.removeTimeObserver(it) }
            avPlayer.pause()
            avPlayer.replaceCurrentItemWithPlayerItem(null)
            try {
                AVAudioSession.sharedInstance().setActive(false, null)
            } catch (_: Throwable) {}
        }
    }

    // EPG fetch on channel resolution — mirrors Android's observeFetchedChannel.
    LaunchedEffect(uiState.fetchedChannel?.id) {
        uiState.fetchedChannel?.let { ch ->
            viewModel.fetchCurrentEpg(ch, kotlinx.datetime.Clock.System.now().toEpochMilliseconds())
        }
    }

    // Catch-up seek: collect requests from the slider and drive AVPlayer.
    LaunchedEffect(Unit) {
        viewModel.catchUpSeekRequests.collect { positionMs ->
            val seconds = positionMs / 1000.0
            avPlayer.seekToTime(CMTimeMakeWithSeconds(seconds, preferredTimescale = 1000))
        }
    }

    // Catch-up playback speed → AVPlayer.rate. Only applied while catch-up is
    // active; the live stream always plays at 1x. Property-setter syntax —
    // `avPlayer.setRate(...)` does not exist as a standalone in Kotlin/Native
    // cinterop (the multi-arg `setRate(_:time:atHostTime:)` does, but the
    // simple form is the property setter).
    val catchUpActive = uiState.catchUpState.isActive
    val playbackSpeed = uiState.catchUpState.playbackSpeed
    LaunchedEffect(catchUpActive, playbackSpeed) {
        avPlayer.rate = if (catchUpActive) playbackSpeed else 1f
    }

    ChannelPlayerScreen(
        viewModel = viewModel,
        sleepTimerManager = sleepTimerManager,
        playerViewFactory = { mod ->
            AVPlayerSurface(player = avPlayer, aspectMode = currentAspectMode, modifier = mod)
        },
        onBack = onClose,
        onRetry = {
            retryCount = 0
            startPlayback(avPlayer, viewModel.uiState.value.currentChannelUrl, preferencesHelper)
            viewModel.setPlaybackBuffering()
        },
        onChannelClick = { channel -> viewModel.onChannelSelected(channel) },
        onNavigateNext = {
            val channels = viewModel.uiState.value.cachedChannels ?: emptyList()
            val currentUrl = viewModel.uiState.value.currentChannelUrl
            findNeighbour(channels, currentUrl, +1)?.let { viewModel.onChannelSelected(it) }
        },
        onNavigatePrevious = {
            val channels = viewModel.uiState.value.cachedChannels ?: emptyList()
            val currentUrl = viewModel.uiState.value.currentChannelUrl
            findNeighbour(channels, currentUrl, -1)?.let { viewModel.onChannelSelected(it) }
        },
        onPlayPause = {
            if (uiState.playbackState is LivePlaybackState.Playing) avPlayer.pause() else avPlayer.play()
        },
        onSleepTimerSelect = { sleepTimerManager.start(it) },
        onSleepTimerCancel = { sleepTimerManager.cancel() }
    )
}

private fun startPlayback(
    player: AVPlayer,
    url: String,
    preferencesHelper: PreferencesHelper
) {
    if (url.isBlank()) return
    val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
    val normalized = MediaUrlNormalizer.normalize(url, outputFormat)
    val nsUrl = NSURL.URLWithString(normalized) ?: return
    val item = AVPlayerItem(uRL = nsUrl)
    player.replaceCurrentItemWithPlayerItem(item)
    player.play()
}

private fun findNeighbour(
    channels: List<pt.hitv.core.model.Channel>,
    currentUrl: String,
    delta: Int
): pt.hitv.core.model.Channel? {
    if (channels.isEmpty()) return null
    val currentIdx = channels.indexOfFirst {
        ChannelNavigationHelper.isChannelPlaying(it.streamUrl, currentUrl)
    }
    val nextIdx = when {
        delta > 0 -> if (currentIdx in 0 until channels.size - 1) currentIdx + 1 else 0
        delta < 0 -> if (currentIdx > 0) currentIdx - 1 else channels.size - 1
        else -> currentIdx
    }
    return channels.getOrNull(nextIdx)
}
