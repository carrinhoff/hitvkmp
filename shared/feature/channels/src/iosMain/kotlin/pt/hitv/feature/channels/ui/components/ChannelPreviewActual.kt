package pt.hitv.feature.channels.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSURL
import platform.QuartzCore.CALayer
import platform.UIKit.UIView
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.Channel

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ChannelPreviewComposable(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    modifier: Modifier
) {
    val themeColors = getThemeColors()
    var isMuted by remember { mutableStateOf(true) }
    var avPlayer: AVPlayer? by remember { mutableStateOf(null) }

    // Normalize URL: force .m3u8 for HLS on iOS
    val streamUrl = remember(channel.streamUrl) {
        val url = (channel.streamUrl ?: "").trim()
        val knownExtensions = listOf(".m3u8", ".mpd", ".ts", ".mp4", ".webm")
        val hasKnownExtension = knownExtensions.any { url.endsWith(it, ignoreCase = true) }
        if (!hasKnownExtension) "$url.m3u8" else url
    }

    DisposableEffect(streamUrl) {
        val nsUrl = NSURL.URLWithString(streamUrl)
        val player = if (nsUrl != null) {
            AVPlayer(uRL = nsUrl).apply {
                isMuted = true // Start muted
                play()
            }
        } else null
        avPlayer = player

        onDispose {
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
            // AVPlayerLayer in UIKitView
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
                        // Update layer frame when view resizes
                        val sublayers = view.layer.sublayers
                        if (sublayers != null) {
                            for (layer in sublayers) {
                                (layer as? CALayer)?.frame = view.bounds
                            }
                        }
                    }
                )
            } ?: run {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = themeColors.primaryColor)
                }
            }

            // Controls overlay
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        avPlayer?.let { it.isMuted = isMuted }
                    },
                    modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape)
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
