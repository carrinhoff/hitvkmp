@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerLayer
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CALayer
import platform.UIKit.UIView

/**
 * Compose surface that hosts an [AVPlayerLayer] inside a [UIView] via [UIKitView].
 *
 * Shared between the channel / movie / series iOS player hosts — they all need the
 * same "render this AVPlayer with the current aspect mode" surface as the slot
 * supplied to the shared `playerViewFactory: @Composable (Modifier) -> Unit` parameter
 * in `ChannelPlayerScreen` / `MoviePlayerScreen` / `SeriesPlayerScreen`.
 *
 * The `update` lambda re-applies `videoGravity` and resizes the layer to match the
 * view bounds whenever the surface recomposes (aspect-mode change, layout change).
 */
@Composable
internal fun AVPlayerSurface(
    player: AVPlayer,
    aspectMode: PlayerAspectMode,
    modifier: Modifier = Modifier
) {
    UIKitView(
        factory = {
            val container = UIView(frame = CGRectMake(0.0, 0.0, 100.0, 100.0))
            val layer = AVPlayerLayer.playerLayerWithPlayer(player)
            layer.videoGravity = aspectMode.toVideoGravity()
            layer.frame = container.bounds
            container.layer.addSublayer(layer)
            container
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            val sublayers = view.layer.sublayers
            if (sublayers != null) {
                for (sublayer in sublayers) {
                    (sublayer as? CALayer)?.frame = view.bounds
                }
                (sublayers.firstOrNull() as? AVPlayerLayer)?.videoGravity = aspectMode.toVideoGravity()
            }
        }
    )
}
