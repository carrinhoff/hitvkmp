@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import platform.AVFoundation.AVLayerVideoGravityResize
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill

/**
 * Translates the shared [PlayerAspectMode] enum into the AVFoundation videoGravity
 * string constants consumed by `AVPlayerLayer` / `AVPlayerViewController`.
 *
 * Mapping matches the docstring in [PlayerAspectMode]:
 * - Fit → letterbox: scale to fit, preserve aspect ratio → `resizeAspect`.
 * - Fill → stretch to fill: no aspect preservation → `resize`.
 * - Zoom → crop: scale to fill, preserve aspect ratio → `resizeAspectFill`.
 */
internal fun PlayerAspectMode.toVideoGravity(): String = when (this) {
    PlayerAspectMode.Fit -> AVLayerVideoGravityResizeAspect
    PlayerAspectMode.Fill -> AVLayerVideoGravityResize
    PlayerAspectMode.Zoom -> AVLayerVideoGravityResizeAspectFill
}
