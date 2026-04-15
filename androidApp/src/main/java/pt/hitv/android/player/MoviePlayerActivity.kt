package pt.hitv.android.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.AppThemeProvider
import pt.hitv.feature.player.composables.BufferingIndicator

@OptIn(UnstableApi::class)
class MoviePlayerActivity : ComponentActivity() {

    private val preferencesHelper: PreferencesHelper by inject()
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(8000)
        .setReadTimeoutMs(8000)
        .setAllowCrossProtocolRedirects(true)

    private val args by lazy {
        object {
            val movieUrl: String = intent.getStringExtra("url") ?: ""
            val movieTitle: String = intent.getStringExtra("title") ?: ""
            val streamId: Int = intent.getIntExtra("streamId", 0)
            val startPositionMs: Long = intent.getLongExtra("startPositionMs", 0L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setFullScreen()
        createPlayer()

        val movieTitle = mutableStateOf(args.movieTitle)
        val isBuffering = mutableStateOf(true)
        val showControls = mutableStateOf(false)

        setContent {
            AppThemeProvider {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showControls.value = !showControls.value }
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                playerView = this
                                useController = false
                                this.resizeMode = this@MoviePlayerActivity.resizeMode
                                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                keepScreenOn = true
                                player = exoPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isBuffering.value) {
                        BufferingIndicator()
                    }

                    // Top bar
                    if (showControls.value) {
                        LaunchedEffect(Unit) {
                            delay(5000)
                            showControls.value = false
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { finish() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = movieTitle.value,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                IconButton(onClick = { toggleAspectRatio() }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Player listener
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering.value = state == Player.STATE_BUFFERING
            }
            override fun onPlayerError(error: PlaybackException) {
                isBuffering.value = false
            }
        })

        // Start playback
        startPlayback(args.movieUrl, args.startPositionMs)

        // Save position periodically
        lifecycleScope.launch {
            while (true) {
                delay(5000)
                // Position saving would go here via MoviePlayerViewModel
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finish() }
        })
    }

    private fun createPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
    }

    private fun startPlayback(url: String, startPositionMs: Long) {
        val normalizedUrl = normalizeUrl(url)
        val mediaSource = createMediaSource(normalizedUrl)
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            if (startPositionMs > 0) seekTo(startPositionMs)
            playWhenReady = true
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
    override fun onStop() { exoPlayer?.stop(); super.onStop() }
    override fun onDestroy() { exoPlayer?.release(); exoPlayer = null; super.onDestroy() }
}
