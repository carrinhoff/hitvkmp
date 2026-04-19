@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController

/**
 * Compose surface that renders an [AVPlayer] inside an [AVPlayerViewController]
 * mounted via [UIKitViewController]. Shared between the channel / movie / series
 * iOS hosts as the slot they pass to `playerViewFactory: (Modifier) -> Unit` on
 * the shared overlay screens.
 *
 * Earlier this was a raw UIView + AVPlayerLayer wrapped in [UIKitView]. That
 * surfaced as a "tiny video in the top-left corner" bug at full screen — CALayer
 * doesn't auto-resize when its host UIView's bounds change, and Compose's `update`
 * lambda only fires on recomposition (not layout). AVPlayerViewController owns
 * its own auto-layout so the video always fills the host bounds.
 *
 * `showsPlaybackControls = false`: the channel player overlay supplies its own
 * Compose controls (top bar, sidebar, sleep timer). For the movie / series
 * players, the existing [PlayerHost] surface keeps the native controls — this
 * helper is for the no-native-chrome case.
 */
@Composable
internal fun AVPlayerSurface(
    player: AVPlayer,
    aspectMode: PlayerAspectMode,
    modifier: Modifier = Modifier
) {
    val vc = remember(player) {
        AVPlayerViewController().apply {
            this.player = player
            showsPlaybackControls = false
            videoGravity = aspectMode.toVideoGravity()
        }
    }
    // Re-apply gravity on aspect-mode change (recomposition).
    LaunchedEffect(aspectMode) {
        vc.videoGravity = aspectMode.toVideoGravity()
    }
    UIKitViewController(
        modifier = modifier.fillMaxSize(),
        factory = { vc }
    )
}
