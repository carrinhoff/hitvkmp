package pt.hitv.android.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.AppThemeProvider
import pt.hitv.feature.player.LivePlaybackState
import pt.hitv.feature.player.LivePlayerViewModel
import pt.hitv.feature.player.toResizeMode
import pt.hitv.feature.player.composables.ChannelPlayerScreen
import pt.hitv.feature.player.helpers.ChannelNavigationHelper
import pt.hitv.feature.player.util.SleepTimerManager

@OptIn(UnstableApi::class)
class ChannelPlayerActivity : ComponentActivity() {

    private val viewModel: LivePlayerViewModel by inject()
    private val preferencesHelper: PreferencesHelper by inject()

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var retryCount = 0
    private val maxRetries = 3

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)
        .setAllowCrossProtocolRedirects(true)

    private val sleepTimerManager by lazy {
        SleepTimerManager(lifecycleScope) { finishAndRemoveTask() }
    }

    private val args by lazy {
        object {
            val url: String = intent.getStringExtra("url") ?: ""
            val name: String = intent.getStringExtra("name") ?: ""
            val titleEpg: String? = intent.getStringExtra("titleEpg")
            val descEpg: String? = intent.getStringExtra("descEpg")
            val logoUrl: String? = intent.getStringExtra("imgEpg")
            val categoryTitle: String? = intent.getStringExtra("categoryTitle")
            val categoryId: Int = intent.getIntExtra("categoryId", -1)
            val licenseKey: String? = intent.getStringExtra("licenseKey")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFullScreen()

        viewModel.initFromArgs(
            url = args.url,
            name = args.name,
            position = "",
            categoryTitle = args.categoryTitle,
            categoryId = args.categoryId,
            licenseKey = args.licenseKey,
            isPiPSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
            isTvDevice = false
        )

        createPlayer()
        viewModel.fetchChannelsFromDB()
        viewModel.fetchChannelCategories()
        viewModel.getFavorites()

        setContent {
            AppThemeProvider {
                ChannelPlayerScreen(
                    viewModel = viewModel,
                    sleepTimerManager = sleepTimerManager,
                    playerViewFactory = { modifier ->
                        AndroidView(
                            factory = { ctx -> createPlayerView(ctx) },
                            modifier = modifier,
                            update = { view ->
                                if (view.player != exoPlayer) {
                                    view.player = exoPlayer
                                }
                            }
                        )
                    },
                    onBack = ::handleBack,
                    onRetry = ::handleRetry,
                    onChannelClick = { channel ->
                        viewModel.onChannelSelected(channel)
                    },
                    onNavigateNext = {
                        val channels = viewModel.uiState.value.cachedChannels ?: emptyList()
                        val currentUrl = viewModel.uiState.value.currentChannelUrl
                        val nextChannel = findNextChannel(channels, currentUrl)
                        nextChannel?.let { viewModel.onChannelSelected(it) }
                    },
                    onNavigatePrevious = {
                        val channels = viewModel.uiState.value.cachedChannels ?: emptyList()
                        val currentUrl = viewModel.uiState.value.currentChannelUrl
                        val prevChannel = findPreviousChannel(channels, currentUrl)
                        prevChannel?.let { viewModel.onChannelSelected(it) }
                    },
                    onPlayPause = {
                        exoPlayer?.let { it.playWhenReady = !it.isPlaying }
                    },
                    onSleepTimerSelect = { sleepTimerManager.start(it) },
                    onSleepTimerCancel = { sleepTimerManager.cancel() },
                    onForceRotation = ::toggleForceRotation
                )
            }
        }

        initBackPressHandler()
        observePlaybackTriggers()
        observeResizeMode()
        observeFetchedChannel()
        observeCatchUp()
    }

    // --- Player creation ---

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> viewModel.setPlaybackBuffering()
                        Player.STATE_READY -> {
                            viewModel.setPlaybackReady()
                            retryCount = 0
                        }
                        else -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (retryCount < maxRetries) {
                        retryCount++
                        viewModel.setAutoRetrying(retryCount, maxRetries)
                        lifecycleScope.launch {
                            delay(1000L * retryCount)
                            val url = viewModel.uiState.value.currentChannelUrl
                            if (url.isNotEmpty()) startPlayback(url)
                        }
                    } else {
                        viewModel.setPlaybackError(
                            error.message ?: "Playback error",
                            retryCount,
                            maxRetries
                        )
                    }
                }
            })
        }
    }

    private fun createPlayerView(context: android.content.Context): PlayerView {
        return PlayerView(context).apply {
            playerView = this
            useController = false
            resizeMode = viewModel.uiState.value.currentAspectMode.toResizeMode()
            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            keepScreenOn = true
            player = exoPlayer
        }
    }

    // --- Playback ---

    private fun startPlayback(url: String) {
        viewModel.setPlaybackBuffering()
        val normalizedUrl = normalizePlaybackUrl(url)
        val mediaSource = createMediaSource(normalizedUrl)
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    /**
     * Normalizes URL by appending output format extension if needed.
     * Matches original MediaSourceFactory.normalizePlaybackUrl logic.
     */
    private fun normalizePlaybackUrl(url: String): String {
        val trimmedUrl = url.trim()
        val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        val knownExtensions = listOf(".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm")
        val hasKnownExtension = knownExtensions.any { trimmedUrl.endsWith(it, ignoreCase = true) }

        return if (!outputFormat.isNullOrEmpty() && !hasKnownExtension && !trimmedUrl.contains(".m3u8", ignoreCase = true)) {
            "$trimmedUrl.$outputFormat"
        } else {
            trimmedUrl
        }
    }

    private fun createMediaSource(url: String): MediaSource {
        val mediaItem = MediaItem.fromUri(url)
        val path = android.net.Uri.parse(url).path ?: ""

        val isHls = path.endsWith(".m3u8", ignoreCase = true)
        val isDash = path.endsWith(".mpd", ignoreCase = true)

        return when {
            isHls -> {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            }
            isDash -> {
                // Would need DashMediaSource but skipping for now
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            else -> {
                // Progressive handles .ts, .mp4, raw TS streams
                ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }
    }

    // --- Observers ---

    private fun observePlaybackTriggers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.currentChannelUrl }.distinctUntilChanged()
                    .collect { url ->
                        if (url.isNotEmpty()) {
                            retryCount = 0
                            startPlayback(url)
                        }
                    }
            }
        }
    }

    private fun observeResizeMode() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.currentAspectMode }.distinctUntilChanged()
                    .collect { mode ->
                        playerView?.resizeMode = mode.toResizeMode()
                    }
            }
        }
    }

    private fun observeFetchedChannel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.fetchedChannel }.distinctUntilChanged()
                    .collect { channel ->
                        if (channel != null) {
                            viewModel.fetchCurrentEpg(channel, System.currentTimeMillis())
                        }
                    }
            }
        }
    }

    // --- Catch-up (time-shift) bridges ---
    // Three pieces: 1) collect seek requests from the slider and hand them to
    // ExoPlayer; 2) track playback speed changes (1x on live, 0.5-2x in catch-up);
    // 3) pump periodic position updates into the VM so the slider stays in sync.
    private fun observeCatchUp() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.catchUpSeekRequests.collect { positionMs ->
                    exoPlayer?.seekTo(positionMs)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.catchUpState.isActive to it.catchUpState.playbackSpeed }
                    .distinctUntilChanged()
                    .collect { (active, speed) ->
                        exoPlayer?.playbackParameters = PlaybackParameters(if (active) speed else 1f)
                    }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (viewModel.uiState.value.catchUpState.isActive) {
                        val p = exoPlayer
                        if (p != null) {
                            val pos = p.currentPosition.coerceAtLeast(0L)
                            val dur = if (p.duration > 0L) p.duration else 0L
                            viewModel.updateCatchUpPosition(pos, dur)
                        }
                    }
                    delay(500L)
                }
            }
        }
    }

    // --- Channel navigation ---

    private fun findNextChannel(channels: List<pt.hitv.core.model.Channel>, currentUrl: String): pt.hitv.core.model.Channel? {
        if (channels.isEmpty()) return null
        val currentIdx = channels.indexOfFirst {
            ChannelNavigationHelper.isChannelPlaying(it.streamUrl, currentUrl)
        }
        val nextIdx = if (currentIdx >= 0 && currentIdx < channels.size - 1) currentIdx + 1 else 0
        return channels.getOrNull(nextIdx)
    }

    private fun findPreviousChannel(channels: List<pt.hitv.core.model.Channel>, currentUrl: String): pt.hitv.core.model.Channel? {
        if (channels.isEmpty()) return null
        val currentIdx = channels.indexOfFirst {
            ChannelNavigationHelper.isChannelPlaying(it.streamUrl, currentUrl)
        }
        val prevIdx = if (currentIdx > 0) currentIdx - 1 else channels.size - 1
        return channels.getOrNull(prevIdx)
    }

    // --- Navigation ---

    private var currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

    private fun toggleForceRotation() {
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        currentOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        requestedOrientation = currentOrientation
    }

    private fun handleBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {
                finishAndRemoveTask()
            }
        } else {
            finishAndRemoveTask()
        }
    }

    private fun handleRetry() {
        retryCount = 0
        exoPlayer?.stop()
        startPlayback(viewModel.uiState.value.currentChannelUrl)
    }

    private fun initBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = handleBack()
        })
    }

    // --- Window ---

    private fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // --- Lifecycle ---

    override fun onResume() {
        super.onResume()
        setFullScreen()
    }

    override fun onPause() {
        preferencesHelper.setStoredTag("lastChannelUrl", viewModel.uiState.value.currentChannelUrl)
        super.onPause()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            // Keep playing in PiP
        } else {
            exoPlayer?.stop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        sleepTimerManager.cancel()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    // --- PiP ---

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode && lifecycle.currentState == Lifecycle.State.CREATED) {
            // User dismissed PiP
            finishAndRemoveTask()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        retryCount = 0
        setIntent(intent)
        val newUrl = intent.getStringExtra("url")?.trim() ?: return
        val newName = intent.getStringExtra("name") ?: ""
        viewModel.handleNewIntent(
            url = newUrl,
            name = newName,
            position = null,
            categoryTitle = intent.getStringExtra("categoryTitle"),
            categoryId = intent.getIntExtra("categoryId", -1),
            licenseKey = intent.getStringExtra("licenseKey"),
            titleEpg = intent.getStringExtra("titleEpg"),
            descEpg = intent.getStringExtra("descEpg"),
            imgEpg = intent.getStringExtra("imgEpg")
        )
    }
}
