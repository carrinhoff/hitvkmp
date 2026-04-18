@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.preferredForwardBufferDuration

/**
 * iOS-side helpers mirroring [PlayerConfigFactory] numbers onto AVFoundation primitives.
 *
 * Android parity: on Android, [PlayerConfigFactory.VOD_BUFFER] is fed to
 * `DefaultLoadControl.Builder`. On iOS, the closest knobs are:
 * - [AVPlayerItem.preferredForwardBufferDuration] — maxMs / 1000 (seconds).
 * - [AVPlayer.automaticallyWaitsToMinimizeStalling] = false, so playback starts as
 *   soon as the minimum buffer is filled instead of waiting for the system's internal
 *   heuristic. Matches ExoPlayer's deterministic load-control behavior.
 */
internal fun AVPlayerItem.applyVodBufferProfile() {
    preferredForwardBufferDuration = PlayerConfigFactory.VOD_BUFFER.maxMs / 1000.0
}

/**
 * Disables AVPlayer's internal "wait to minimize stalling" heuristic so playback begins
 * as soon as the preferred forward buffer is filled — matches the deterministic startup
 * that ExoPlayer delivers on Android.
 */
internal fun AVPlayer.applyVodBufferProfile() {
    automaticallyWaitsToMinimizeStalling = false
}
