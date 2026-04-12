package pt.hitv.feature.channels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CancellationException
import pt.hitv.core.model.Channel
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.getThemeColors
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource

private const val TAG = "ChannelPreviewPlayer"
private const val PREVIEW_VOLUME = 0.5f

/**
 * Android-only video preview player component for channels.
 * Uses ExoPlayer for video playback - not available on iOS.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ChannelPreviewPlayer(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    isPipModeActive: MutableStateFlow<Boolean>,
    preferencesHelper: PreferencesHelper,
    viewModel: StreamViewModel,
    useFixedSize: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeColors = getThemeColors()

    val isPipActive by isPipModeActive.collectAsState()

    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    var playerState by remember { mutableStateOf(Player.STATE_IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPlayerReady by remember { mutableStateOf(false) }

    var isSoundEnabled by remember { mutableStateOf(false) }

    // Fetch user's output formats to normalize the URL (same as original project)
    var userOutputFormats by remember { mutableStateOf<List<String>?>(null) }
    var formatsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(channel.userId) {
        userOutputFormats = try {
            viewModel.getCredentialsByUserId(channel.userId)?.allowedOutputFormats
        } catch (_: Exception) { null }
        formatsLoaded = true
    }

    if (formatsLoaded) {
    DisposableEffect(channel.streamUrl, userOutputFormats) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)
            .setAllowCrossProtocolRedirects(true)

        // Normalize URL: append output format if no known extension
        // (matches original MediaSourceFactory.normalizePlaybackUrl)
        val rawUrl = (channel.streamUrl ?: "").trim()
        val outputFormat = userOutputFormats?.firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        val knownExtensions = listOf(".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm")
        val hasKnownExtension = knownExtensions.any { rawUrl.endsWith(it, ignoreCase = true) }
        val url = if (!outputFormat.isNullOrEmpty() && !hasKnownExtension && !rawUrl.contains(".m3u8", ignoreCase = true)) {
            "$rawUrl.$outputFormat"
        } else {
            rawUrl
        }

        val mediaItem = MediaItem.fromUri(url)
        val path = android.net.Uri.parse(url).path ?: ""
        val isHls = path.endsWith(".m3u8", ignoreCase = true)
        val mediaSource = if (isHls) {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            // Progressive handles .ts, .mp4, and other formats
            ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(mediaItem)
        }

        val player = ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(5_000, 15_000, 1_000, 2_000)
                    .build()
            )
            .build().apply {
                setMediaSource(mediaSource)
                prepare()
                playWhenReady = true
                volume = when {
                    isPipActive -> 0f
                    isSoundEnabled -> PREVIEW_VOLUME
                    else -> 0f
                }
                repeatMode = Player.REPEAT_MODE_ONE

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        playerState = state
                        isPlayerReady = state == Player.STATE_READY
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        errorMessage = error.message
                    }
                })
            }
        exoPlayer = player

        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) { exoPlayer?.playWhenReady = false }
            override fun onResume(owner: LifecycleOwner) { exoPlayer?.playWhenReady = true }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    } // end if (formatsLoaded)

    LaunchedEffect(isPipActive, isSoundEnabled) {
        exoPlayer?.volume = when {
            isPipActive -> 0f
            isSoundEnabled -> PREVIEW_VOLUME
            else -> 0f
        }
    }

    Card(
        shape = if (useFixedSize) RoundedCornerShape(12.dp) else RoundedCornerShape(0.dp),
        elevation = if (useFixedSize) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        modifier = if (useFixedSize) {
            Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(vertical = 8.dp)
        } else {
            Modifier.fillMaxSize()
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onPreviewClicked() }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view -> view.player = exoPlayer }
            )

            when {
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = themeColors.primaryColor, modifier = Modifier.size(40.dp))
                    }
                }
                playerState == Player.STATE_IDLE || playerState == Player.STATE_BUFFERING -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = themeColors.primaryColor, trackColor = Color.White.copy(alpha = 0.2f))
                            Text(text = "Loading preview\u2026", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { isSoundEnabled = !isSoundEnabled; preferencesHelper.setChannelPreviewSoundEnabled(isSoundEnabled) },
                    modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = if (isSoundEnabled) "Mute" else "Unmute", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
