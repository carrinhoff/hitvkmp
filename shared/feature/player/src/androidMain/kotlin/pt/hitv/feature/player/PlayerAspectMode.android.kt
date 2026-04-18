package pt.hitv.feature.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout

/**
 * Maps the shared [PlayerAspectMode] to ExoPlayer's `AspectRatioFrameLayout`
 * resize-mode constants. Consolidates the duplicate extension that previously
 * lived as a private helper in [pt.hitv.android.player.ChannelPlayerActivity].
 */
@UnstableApi
fun PlayerAspectMode.toResizeMode(): Int = when (this) {
    PlayerAspectMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    PlayerAspectMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
    PlayerAspectMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
}
