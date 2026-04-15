package pt.hitv.android.player

import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.AppThemeProvider
import pt.hitv.feature.player.composables.SeriesPlayerScreen
import pt.hitv.feature.player.series.SeriesPlayerViewModel
import pt.hitv.feature.player.util.SleepTimerManager

@OptIn(UnstableApi::class)
class SeriesPlayerActivity : ComponentActivity() {

    private val preferencesHelper: PreferencesHelper by inject()
    private val viewModel: SeriesPlayerViewModel by inject()
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(8000).setReadTimeoutMs(8000).setAllowCrossProtocolRedirects(true)

    private val sleepTimerManager by lazy {
        SleepTimerManager(lifecycleScope) { exoPlayer?.stop(); finish() }
    }

    private val args by lazy {
        object {
            val seriesId: String = intent.getStringExtra("seriesId") ?: ""
            val seasonNumber: Int = intent.getIntExtra("seasonNumber", 0)
            val episodeIndex: Int = intent.getIntExtra("episodeIndex", 0)
        }
    }

    private val isBuffering = mutableStateOf(true)
    private val isPlaying = mutableStateOf(false)
    private val currentPositionMs = mutableLongStateOf(0L)
    private val durationMs = mutableLongStateOf(0L)
    private val currentEpisodeIndex = mutableIntStateOf(0)
    private val episodeTitle = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFullScreen()
        createPlayer()

        currentEpisodeIndex.intValue = args.episodeIndex

        // Load episodes then start playback
        viewModel.loadEpisodes(args.seriesId, args.seasonNumber)
        lifecycleScope.launch {
            // Wait for episodes to load
            viewModel.uiState.first { !it.isLoading && it.episodes.isNotEmpty() }
            playEpisode(args.episodeIndex)
        }

        // Periodic position update + save
        lifecycleScope.launch {
            while (true) {
                delay(1000)
                exoPlayer?.let { player ->
                    currentPositionMs.longValue = player.currentPosition.coerceAtLeast(0)
                    durationMs.longValue = player.duration.coerceAtLeast(0)
                    isPlaying.value = player.isPlaying
                }
            }
        }
        lifecycleScope.launch {
            while (true) {
                delay(5000)
                saveCurrentPosition()
            }
        }

        setContent {
            AppThemeProvider {
                val uiState by viewModel.uiState.collectAsState()
                val episodes = uiState.episodes

                SeriesPlayerScreen(
                    episodeTitle = episodeTitle.value,
                    isBuffering = isBuffering.value,
                    isPlaying = isPlaying.value,
                    currentPositionMs = currentPositionMs.longValue,
                    durationMs = durationMs.longValue,
                    hasNextEpisode = currentEpisodeIndex.intValue < episodes.size - 1,
                    hasPreviousEpisode = currentEpisodeIndex.intValue > 0,
                    sleepTimerManager = sleepTimerManager,
                    playerViewFactory = { modifier ->
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    playerView = this
                                    useController = false
                                    this.resizeMode = this@SeriesPlayerActivity.resizeMode
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                                    keepScreenOn = true
                                    player = exoPlayer
                                }
                            },
                            modifier = modifier,
                            update = { it.player = exoPlayer }
                        )
                    },
                    onBack = { finish() },
                    onPlayPause = { exoPlayer?.let { it.playWhenReady = !it.isPlaying } },
                    onSeekTo = { exoPlayer?.seekTo(it) },
                    onNextEpisode = {
                        val nextIdx = currentEpisodeIndex.intValue + 1
                        if (nextIdx < episodes.size) {
                            saveCurrentPosition()
                            playEpisode(nextIdx)
                        }
                    },
                    onPreviousEpisode = {
                        val prevIdx = currentEpisodeIndex.intValue - 1
                        if (prevIdx >= 0) {
                            saveCurrentPosition()
                            playEpisode(prevIdx)
                        }
                    },
                    onAspectRatioToggle = { toggleAspectRatio() },
                    onSleepTimerSelect = { sleepTimerManager.start(it) },
                    onSleepTimerCancel = { sleepTimerManager.cancel() }
                )
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun playEpisode(index: Int) {
        val episodes = viewModel.uiState.value.episodes
        if (index !in episodes.indices) return

        val episode = episodes[index]
        currentEpisodeIndex.intValue = index
        episodeTitle.value = "${episode.episodeNum}. ${episode.title ?: "Episode ${episode.episodeNum}"}"

        val host = preferencesHelper.getHostUrl()
        val user = preferencesHelper.getUsername()
        val pass = preferencesHelper.getPassword()
        val url = "${host}series/$user/$pass/${episode.id}.${episode.containerExtension ?: "m3u8"}"

        // Get saved position for this episode
        val savedPosition = episode.info?.playbackPosition ?: 0L

        val normalizedUrl = normalizeUrl(url)
        val mediaSource = createMediaSource(normalizedUrl)
        exoPlayer?.apply {
            stop()
            setMediaSource(mediaSource)
            prepare()
            if (savedPosition > 0) seekTo(savedPosition)
            playWhenReady = true
        }
        isBuffering.value = true
    }

    private fun saveCurrentPosition() {
        val episodes = viewModel.uiState.value.episodes
        val idx = currentEpisodeIndex.intValue
        if (idx !in episodes.indices) return
        val episode = episodes[idx]
        val pos = exoPlayer?.currentPosition ?: 0L
        val dur = exoPlayer?.duration ?: 0L
        if (pos > 0 && episode.id.isNotEmpty()) {
            viewModel.savePlaybackPosition(pos, episode.id)
            if (dur > 0) viewModel.saveEpisodeDuration(dur.toDouble(), episode.id)
        }
    }

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering.value = state == Player.STATE_BUFFERING
                }
                override fun onPlayerError(error: PlaybackException) {
                    isBuffering.value = false
                }
            })
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        val knownExtensions = listOf(".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm")
        val hasKnownExtension = knownExtensions.any { trimmed.endsWith(it, ignoreCase = true) }
        return if (!outputFormat.isNullOrEmpty() && !hasKnownExtension) "$trimmed.$outputFormat" else trimmed
    }

    private fun createMediaSource(url: String): MediaSource {
        val mediaItem = MediaItem.fromUri(url)
        val path = android.net.Uri.parse(url).path ?: ""
        return if (path.endsWith(".m3u8", true)) {
            HlsMediaSource.Factory(httpDataSourceFactory).setAllowChunklessPreparation(true).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(httpDataSourceFactory).createMediaSource(mediaItem)
        }
    }

    private fun toggleAspectRatio() {
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        playerView?.resizeMode = resizeMode
    }

    private fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onResume() { super.onResume(); setFullScreen() }
    override fun onPause() { saveCurrentPosition(); super.onPause() }
    override fun onStop() { exoPlayer?.pause(); super.onStop() }
    override fun onDestroy() { sleepTimerManager.cancel(); exoPlayer?.release(); exoPlayer = null; super.onDestroy() }
}
