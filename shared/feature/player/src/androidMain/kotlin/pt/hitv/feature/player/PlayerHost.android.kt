package pt.hitv.feature.player

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.hitv.core.common.AndroidContextHolder
import pt.hitv.feature.player.media.MediaItem
import androidx.media3.common.MediaItem as Media3Item

/**
 * Android actual for [PlayerHost]. Builds an ExoPlayer tuned with the shared
 * [PlayerConfigFactory] numbers (VOD load-control + 2 Mbps bandwidth estimate
 * + EXTENSION_RENDERER_MODE_ON), exposes a reactive [PlayerController], and
 * renders the native `PlayerView` via [Surface].
 *
 * The ExoPlayer instance is created eagerly in the constructor so overlay
 * composables can read its state before the surface is first composed.
 *
 * Matches the original hitv behavior found in `MoviePlayerActivity.initializePlayers`
 * / `SeriesPlayerManager.initialize` — subtitle loading, Cast, and analytics are
 * out of scope for this port.
 */
@UnstableApi
actual class PlayerHost actual constructor(
    private val config: PlayerConfig,
    private val items: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long
) {

    private val context = AndroidContextHolder.applicationContext

    private val renderersFactory = DefaultRenderersFactory(context)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            PlayerConfigFactory.VOD_BUFFER.minMs,
            PlayerConfigFactory.VOD_BUFFER.maxMs,
            PlayerConfigFactory.VOD_BUFFER.playbackMs,
            PlayerConfigFactory.VOD_BUFFER.rebufferMs
        )
        .build()

    private val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
        .setInitialBitrateEstimate(PlayerConfigFactory.INITIAL_BITRATE_ESTIMATE)
        .build()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setLoadControl(loadControl)
        .setBandwidthMeter(bandwidthMeter)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    private val _currentPositionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _playbackState = MutableStateFlow(PlaybackState.Idle)
    private val _error = MutableStateFlow<String?>(null)
    private val _currentIndex = MutableStateFlow(startIndex.coerceAtLeast(0))
    private val _aspectMode = MutableStateFlow(config.initialAspectMode)

    private var playerView: PlayerView? = null
    private var released = false

    actual val controller: PlayerController = object : PlayerController {
        override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
        override val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
        override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
        override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
        override val error: StateFlow<String?> = _error.asStateFlow()

        override fun play() { exoPlayer.playWhenReady = true }
        override fun pause() { exoPlayer.playWhenReady = false }
        override fun seekTo(positionMs: Long) { exoPlayer.seekTo(positionMs) }
        override fun release() { this@PlayerHost.release() }
    }

    actual val currentIndexFlow: StateFlow<Int> = _currentIndex.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = when (state) {
                Player.STATE_IDLE -> PlaybackState.Idle
                Player.STATE_BUFFERING -> PlaybackState.Buffering
                Player.STATE_READY -> PlaybackState.Ready
                Player.STATE_ENDED -> PlaybackState.Ended
                else -> PlaybackState.Idle
            }
            if (state == Player.STATE_READY) {
                _error.value = null
                val d = exoPlayer.duration
                _durationMs.value = if (d > 0) d else 0L
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            _isPlaying.value = playWhenReady && exoPlayer.playbackState == Player.STATE_READY
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _error.value = error.message
            _playbackState.value = PlaybackState.Error
        }

        override fun onMediaItemTransition(mediaItem: Media3Item?, reason: Int) {
            _currentIndex.value = exoPlayer.currentMediaItemIndex
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentIndex.value = exoPlayer.currentMediaItemIndex
        }
    }

    init {
        exoPlayer.addListener(listener)
        val media3Items = items.map { Media3Item.fromUri(it.url) }
        exoPlayer.setMediaItems(
            media3Items,
            startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0)),
            startPositionMs
        )
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        // Periodic position polling — 500 ms cadence matches ExoPlayer's default
        // `PlayerControlView` refresh rate so the scrubber and overlay stay in sync.
        positionJob = scope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition
                if (pos >= 0) _currentPositionMs.value = pos
                delay(500L)
            }
        }
    }

    actual fun currentIndex(): Int = _currentIndex.value

    actual fun seekToPrevious() {
        exoPlayer.seekToPreviousMediaItem()
    }

    actual fun seekToNext() {
        exoPlayer.seekToNextMediaItem()
    }

    actual fun setAspectMode(mode: PlayerAspectMode) {
        _aspectMode.value = mode
        playerView?.resizeMode = mode.toResizeMode()
    }

    @Composable
    actual fun Surface(modifier: Modifier) {
        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                PlayerView(ctx).apply {
                    playerView = this
                    player = exoPlayer
                    useController = true
                    controllerShowTimeoutMs = 5_000
                    controllerHideOnTouch = true
                    resizeMode = _aspectMode.value.toResizeMode()
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setShowPreviousButton(items.size > 1)
                    setShowNextButton(items.size > 1)
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowSubtitleButton(true)
                    setKeepContentOnPlayerReset(false)
                    subtitleView?.apply {
                        visibility = View.VISIBLE
                        setStyle(CaptionStyleCompat.DEFAULT)
                        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION)
                    }
                    keepScreenOn = true
                }
            },
            update = { view ->
                if (view.player != exoPlayer) view.player = exoPlayer
                view.resizeMode = _aspectMode.value.toResizeMode()
            }
        )
    }

    actual fun release() {
        if (released) return
        released = true
        positionJob?.cancel()
        positionJob = null
        scope.cancel()
        exoPlayer.removeListener(listener)
        playerView?.player = null
        playerView = null
        exoPlayer.release()
    }
}
