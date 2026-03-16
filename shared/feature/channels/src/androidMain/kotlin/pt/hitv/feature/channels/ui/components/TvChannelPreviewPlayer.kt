package pt.hitv.feature.channels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.media.MediaSourceFactory
import pt.hitv.core.common.media.PlayerConfigFactory
import pt.hitv.core.designsystem.R
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Channel
import pt.hitv.feature.channels.StreamViewModel

private const val PREVIEW_VOLUME = 0.5f

/**
 * TV-optimized video preview player for channels.
 * Android-only (uses ExoPlayer).
 */
@androidx.media3.common.util.UnstableApi
@Composable
fun TvChannelPreviewPlayer(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    isPipModeActive: MutableStateFlow<Boolean>,
    preferencesHelper: PreferencesHelper,
    viewModel: StreamViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val themeColors = getThemeColors()
    val isPipActive by isPipModeActive.collectAsState()

    var exoPlayer: ExoPlayer? by remember { mutableStateOf(null) }
    var playerState by remember { mutableStateOf(Player.STATE_IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSoundEnabled by remember { mutableStateOf(preferencesHelper.getChannelPreviewSoundEnabled()) }

    LaunchedEffect(channel.streamUrl) {
        playerState = Player.STATE_IDLE
        errorMessage = null
    }

    var userOutputFormats by remember { mutableStateOf<List<String>?>(null) }
    var formatsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(channel.userId) {
        userOutputFormats = try {
            viewModel.getCredentialsByUserId(channel.userId)?.allowedOutputFormats
        } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
        formatsLoaded = true
    }

    if (formatsLoaded) {
        DisposableEffect(channel.streamUrl, userOutputFormats) {
            val player = ExoPlayer.Builder(context)
                .setLoadControl(PlayerConfigFactory.createPreviewLoadControl())
                .build().apply {
                    val mediaSource = MediaSourceFactory.createMediaSource(context, channel.streamUrl ?: "", userOutputFormats, channel.licenseKey, preferencesHelper, skipNetworkDetection = true)
                    setMediaSource(mediaSource); prepare(); playWhenReady = true
                    volume = when { isPipActive -> 0f; isSoundEnabled -> PREVIEW_VOLUME; else -> 0f }
                    repeatMode = Player.REPEAT_MODE_ONE
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) { playerState = state }
                        override fun onPlayerError(error: PlaybackException) { errorMessage = error.message }
                    })
                }
            exoPlayer = player
            val obs = object : DefaultLifecycleObserver {
                override fun onPause(owner: LifecycleOwner) { exoPlayer?.playWhenReady = false }
                override fun onResume(owner: LifecycleOwner) { exoPlayer?.playWhenReady = true }
            }
            lifecycleOwner.lifecycle.addObserver(obs)
            onDispose { exoPlayer?.release(); exoPlayer = null; lifecycleOwner.lifecycle.removeObserver(obs) }
        }
    }

    LaunchedEffect(isPipActive, isSoundEnabled) {
        exoPlayer?.volume = when { isPipActive -> 0f; isSoundEnabled -> PREVIEW_VOLUME; else -> 0f }
    }

    Card(shape = RoundedCornerShape(0.dp), elevation = CardDefaults.cardElevation(0.dp), colors = CardDefaults.cardColors(containerColor = Color.Black), modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onPreviewClicked() }) {
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { ctx ->
                PlayerView(ctx).apply { useController = false; resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM }
            }, update = { view -> view.player = exoPlayer })

            when {
                errorMessage != null -> Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Icon(Icons.Default.Error, "Error", tint = themeColors.primaryColor, modifier = Modifier.size(40.dp)) }
                playerState == Player.STATE_IDLE || playerState == Player.STATE_BUFFERING -> Box(Modifier.fillMaxSize().background(Color.Black.copy(0.7f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) { CircularProgressIndicator(Modifier.size(48.dp), color = themeColors.primaryColor); Text(stringResource(R.string.loading_preview_video), color = Color.White, fontSize = 13.sp) }
                }
            }

            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { isSoundEnabled = !isSoundEnabled; preferencesHelper.setChannelPreviewSoundEnabled(isSoundEnabled) }, Modifier.size(40.dp).background(Color.Black.copy(0.6f), CircleShape)) {
                    Icon(if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff, if (isSoundEnabled) "Mute" else "Unmute", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClose, Modifier.size(40.dp).background(Color.Black.copy(0.6f), CircleShape)) { Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp)) }
            }
        }
    }
}
