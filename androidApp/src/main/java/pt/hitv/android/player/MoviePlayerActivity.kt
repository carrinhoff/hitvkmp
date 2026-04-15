package pt.hitv.android.player

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.AppThemeProvider
import pt.hitv.feature.player.composables.SleepTimerIndicator
import pt.hitv.feature.player.movies.MoviePlayerViewModel
import pt.hitv.feature.player.util.SleepTimerManager

@OptIn(UnstableApi::class)
class MoviePlayerActivity : ComponentActivity() {

    private val preferencesHelper: PreferencesHelper by inject()
    private val viewModel: MoviePlayerViewModel by inject()
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var hasPlayerStarted = false
    private var playWhenReady = true
    private var playbackPosition: Long = 0
    private var currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var movieStreamId: Int = 0
    private var isInitializing = false

    private val sleepTimerManager by lazy {
        SleepTimerManager(lifecycleScope) { exoPlayer?.stop(); finish() }
    }

    private val args by lazy {
        object {
            val movieUrl: String = intent.getStringExtra("url") ?: ""
            val movieTitle: String = intent.getStringExtra("title") ?: ""
        }
    }

    // Handler-based periodic position saving (matches original)
    private val savePositionHandler = Handler(Looper.getMainLooper())
    private val savePositionRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.playbackState == Player.STATE_READY && player.isPlaying) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    if (pos > 0 && movieStreamId > 0) viewModel.savePlaybackPosition(movieStreamId, pos)
                }
            }
            savePositionHandler.postDelayed(this, 5000)
        }
    }

    // Compose state
    private val showControls = mutableStateOf(false)
    private val errorMessage = mutableStateOf<String?>(null)
    private val showSleepDialog = mutableStateOf(false)

    // Player listener (matches original — tracks state, saves position, handles errors)
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    errorMessage.value = null
                    if (!hasPlayerStarted) {
                        hasPlayerStarted = true
                        savePositionHandler.removeCallbacks(savePositionRunnable)
                        savePositionHandler.post(savePositionRunnable)
                    }
                }
                Player.STATE_ENDED -> {
                    savePositionHandler.removeCallbacks(savePositionRunnable)
                    if (movieStreamId > 0) viewModel.savePlaybackPosition(movieStreamId, 0)
                    hasPlayerStarted = false
                }
                Player.STATE_IDLE -> {
                    hasPlayerStarted = false
                    savePositionHandler.removeCallbacks(savePositionRunnable)
                }
                else -> {}
            }
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && hasPlayerStarted) {
                savePositionHandler.removeCallbacks(savePositionRunnable)
                saveCurrentPosition()
            }
            if (playWhenReady && exoPlayer?.playbackState == Player.STATE_READY) {
                hasPlayerStarted = true
                savePositionHandler.removeCallbacks(savePositionRunnable)
                savePositionHandler.post(savePositionRunnable)
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            errorMessage.value = "Error: ${error.message}"
            Toast.makeText(this@MoviePlayerActivity, "Playback failed: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFullScreen()

        // Extract streamId from URL (host/movie/user/pass/streamId.ext)
        try {
            movieStreamId = args.movieUrl.substringAfterLast("/").substringBefore(".").toIntOrNull() ?: 0
        } catch (_: Exception) {}

        setContent {
            AppThemeProvider {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    // ExoPlayer PlayerView with BUILT-IN controller (seekbar, play/pause, time)
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                playerView = this
                                useController = true
                                controllerShowTimeoutMs = 5000
                                controllerAutoShow = true
                                resizeMode = currentResizeMode
                                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                keepScreenOn = true
                                setControllerVisibilityListener(
                                    PlayerView.ControllerVisibilityListener { visibility ->
                                        showControls.value = visibility == View.VISIBLE
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { view -> view.player = exoPlayer }
                    )

                    // Top bar overlay (back + title + aspect ratio + sleep timer)
                    if (showControls.value) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.75f),
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = args.movieTitle,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                val sleepMs by sleepTimerManager.remainingMs.collectAsState()
                                if (sleepMs > 0) {
                                    SleepTimerIndicator(remainingMs = sleepMs, onClick = { showSleepDialog.value = true })
                                    Spacer(Modifier.width(4.dp))
                                }
                                IconButton(onClick = { toggleAspectRatio() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { showSleepDialog.value = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Timer, "Sleep Timer", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Error message
                    errorMessage.value?.let { err ->
                        Text(err, color = Color.Red, fontSize = 14.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
                    }
                }

                // Sleep timer dialog
                if (showSleepDialog.value) {
                    val sleepMs by sleepTimerManager.remainingMs.collectAsState()
                    pt.hitv.feature.player.composables.SleepTimerDialog(
                        isTimerActive = sleepMs > 0,
                        remainingMs = sleepMs,
                        onDurationSelected = { sleepTimerManager.start(it); showSleepDialog.value = false },
                        onCancel = { sleepTimerManager.cancel(); showSleepDialog.value = false },
                        onDismiss = { showSleepDialog.value = false }
                    )
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })

        // Load saved position BEFORE initializing player (matches original pattern)
        isInitializing = true
        if (movieStreamId > 0) {
            lifecycleScope.launch {
                try {
                    val savedPos = viewModel.getPlaybackPosition(movieStreamId)
                    if (savedPos != null && savedPos > 0) playbackPosition = savedPos
                } catch (_: Exception) {}
                initializePlayer()
                isInitializing = false
            }
        } else {
            initializePlayer()
            isInitializing = false
        }
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build().apply {
                addListener(playerListener)
            }
            playerView?.player = exoPlayer
        }
        startPlayback()
    }

    private fun startPlayback() {
        val url = args.movieUrl
        if (url.isEmpty()) { errorMessage.value = "Invalid video URL"; return }

        val normalizedUrl = normalizeUrl(url)
        val mediaItem = MediaItem.Builder().setUri(normalizedUrl).build()

        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
            seekTo(playbackPosition)
            playWhenReady = this@MoviePlayerActivity.playWhenReady
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        val knownExtensions = listOf(".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm")
        val hasKnownExtension = knownExtensions.any { trimmed.endsWith(it, ignoreCase = true) }
        return if (!outputFormat.isNullOrEmpty() && !hasKnownExtension) "$trimmed.$outputFormat" else trimmed
    }

    private fun toggleAspectRatio() {
        currentResizeMode = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
        playerView?.resizeMode = currentResizeMode
        val label = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Fit"
            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Fill"
            else -> "Zoom"
        }
        Toast.makeText(this, "Aspect Ratio: $label", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentPosition() {
        exoPlayer?.let { player ->
            val pos = player.currentPosition.coerceAtLeast(0)
            if (pos > 0 && movieStreamId > 0) viewModel.savePlaybackPosition(movieStreamId, pos)
        }
    }

    private fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun releasePlayer() {
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        playerView?.player = null
    }

    // Lifecycle — matches original exactly
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24 && exoPlayer == null && !isInitializing) initializePlayer()
    }
    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT < 24 || exoPlayer == null) && !isInitializing) { setFullScreen(); initializePlayer() }
    }
    override fun onPause() {
        super.onPause()
        savePositionHandler.removeCallbacks(savePositionRunnable)
        if (Util.SDK_INT < 24) {
            exoPlayer?.let { playWhenReady = it.playWhenReady; playbackPosition = it.currentPosition.coerceAtLeast(0) }
            saveCurrentPosition()
            releasePlayer()
        }
    }
    override fun onStop() {
        super.onStop()
        savePositionHandler.removeCallbacks(savePositionRunnable)
        if (Util.SDK_INT >= 24) {
            exoPlayer?.let { playWhenReady = it.playWhenReady; playbackPosition = it.currentPosition.coerceAtLeast(0) }
            saveCurrentPosition()
            releasePlayer()
        }
    }
    override fun onDestroy() {
        sleepTimerManager.cancel()
        savePositionHandler.removeCallbacks(savePositionRunnable)
        saveCurrentPosition()
        releasePlayer()
        super.onDestroy()
    }
}
