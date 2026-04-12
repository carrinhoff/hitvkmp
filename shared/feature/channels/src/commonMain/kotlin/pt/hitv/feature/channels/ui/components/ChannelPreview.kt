package pt.hitv.feature.channels.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.hitv.core.model.Channel

/**
 * Platform-specific channel preview player.
 * Android: ExoPlayer inline preview.
 * iOS: AVPlayer inline preview (stub for now).
 */
@Composable
expect fun ChannelPreviewComposable(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    modifier: Modifier = Modifier
)
