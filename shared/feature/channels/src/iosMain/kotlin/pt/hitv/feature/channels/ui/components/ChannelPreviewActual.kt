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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.compose.koinInject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.currentItem
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.playbackBufferEmpty
import platform.AVFoundation.playbackLikelyToKeepUp
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setMuted
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.QuartzCore.CALayer
import platform.UIKit.UIView
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Channel
import pt.hitv.feature.player.MediaUrlNormalizer

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ChannelPreviewComposable(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    modifier: Modifier
) {
    val themeColors = getThemeColors()
    val preferencesHelper: PreferencesHelper = koinInject()

    var avPlayer: AVPlayer? by remember { mutableStateOf(null) }
    // PreferencesHelper stores "sound enabled" (default true in PreferencesHelper, but
    // original Android default for the preview is sound OFF — matches isSoundEnabled=false
    // initial state in ChannelPreviewPlayer.kt:91). Honour any persisted choice.
    var isSoundEnabled by remember {
        mutableStateOf(preferencesHelper.getStoredBoolean("channel_preview_sound_enabled", false))
    }
    var isBuffering by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    val streamUrl = remember(channel.streamUrl) {
        val outputFormat = preferencesHelper.getStoredTag("output").takeIf { it.isNotEmpty() }
        MediaUrlNormalizer.normalize(channel.streamUrl ?: "", outputFormat)
    }

    DisposableEffect(streamUrl) {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        } catch (_: Exception) {}

        val nsUrl = NSURL.URLWithString(streamUrl)
        val player = if (nsUrl != null) {
            AVPlayer(uRL = nsUrl).apply {
                setMuted(!isSoundEnabled)
                play()
            }
        } else null
        avPlayer = player
        isBuffering = true
        hasError = false

        // Loop: when the segment ends, seek to zero and replay (mirrors ExoPlayer
        // REPEAT_MODE_ONE on Android).
        val loopToken = NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue(),
            usingBlock = { _: NSNotification? ->
                player?.seekToTime(CMTimeMakeWithSeconds(0.0, preferredTimescale = 1000))
                player?.play()
            }
        )

        // Real buffering / ready / error observation via periodic time observer (same
        // pattern as PlayerHost.ios — avoids fragile KVO subclassing).
        val interval = CMTimeMakeWithSeconds(0.5, preferredTimescale = 1000)
        val timeObserver: Any? = player?.addPeriodicTimeObserverForInterval(
            interval = interval,
            queue = null,
            usingBlock = { _: CValue<CMTime> ->
                val item = player.currentItem
                if (item == null) {
                    isBuffering = true
                } else when (item.status) {
                    AVPlayerItemStatusReadyToPlay -> {
                        isBuffering = item.playbackBufferEmpty || !item.playbackLikelyToKeepUp
                        hasError = false
                    }
                    AVPlayerItemStatusFailed -> {
                        isBuffering = false
                        hasError = true
                    }
                    else -> isBuffering = true
                }
            }
        )

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(loopToken)
            if (timeObserver != null && player != null) {
                player.removeTimeObserver(timeObserver)
            }
            player?.pause()
            avPlayer = null
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable { onPreviewClicked() }
        ) {
            // Video layer
            avPlayer?.let { player ->
                UIKitView(
                    factory = {
                        val container = UIView(frame = CGRectMake(0.0, 0.0, 400.0, 225.0))
                        val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player)
                        playerLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                        playerLayer.frame = container.bounds
                        container.layer.addSublayer(playerLayer)
                        container
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        val sublayers = view.layer.sublayers
                        if (sublayers != null) {
                            for (layer in sublayers) {
                                (layer as? CALayer)?.frame = view.bounds
                            }
                        }
                    }
                )
            }

            when {
                hasError -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = themeColors.primaryColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                isBuffering || avPlayer == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = themeColors.primaryColor)
                            Text(
                                "Loading preview\u2026",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Controls: mute + close
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        isSoundEnabled = !isSoundEnabled
                        avPlayer?.setMuted(!isSoundEnabled)
                        preferencesHelper.setChannelPreviewSoundEnabled(isSoundEnabled)
                    },
                    modifier = Modifier.size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (isSoundEnabled) "Mute" else "Unmute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
