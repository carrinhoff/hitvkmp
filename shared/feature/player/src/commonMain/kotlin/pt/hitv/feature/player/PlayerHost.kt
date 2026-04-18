package pt.hitv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import pt.hitv.feature.player.media.MediaItem

/**
 * The single platform abstraction owning a native player instance plus its rendering
 * surface. Created by the launcher (Activity on Android, ComposeUIViewController on
 * iOS), consumed by the shared overlay composables.
 *
 * Construction-time contract:
 * - [config] — buffer profile + initial aspect mode (see [PlayerConfig]).
 * - [items] — ordered playback list. For movies, a single-item list; for series,
 *   one entry per episode — the native player handles playlist transitions.
 * - [startIndex] — 0-based index into [items] to begin playback.
 * - [startPositionMs] — resume position within the start item.
 *
 * Members:
 * - [controller] — [PlayerController] exposing reactive playback state and transport
 *   commands.
 * - [currentIndexFlow] — StateFlow of the current item index; emits on
 *   prev/next/native-playlist transitions.
 * - [currentIndex] — convenience snapshot of the index.
 * - [seekToPrevious] / [seekToNext] — advance through [items]; no-ops at the
 *   boundaries.
 * - [Surface] — composable slot that actually renders the platform player view
 *   (PlayerView on Android, AVPlayerViewController via UIKitView on iOS). Must call
 *   the native controller layer so overlay composables only carry chrome.
 * - [release] — tear down native resources; idempotent. Call from the host
 *   lifecycle owner (Activity.onDestroy / DisposableEffect.onDispose).
 */
expect class PlayerHost(
    config: PlayerConfig,
    items: List<MediaItem>,
    startIndex: Int = 0,
    startPositionMs: Long = 0
) {
    val controller: PlayerController
    val currentIndexFlow: StateFlow<Int>

    fun currentIndex(): Int
    fun seekToPrevious()
    fun seekToNext()

    /**
     * Push a new aspect mode to the native player surface. Added to the contract by
     * Team B because the overlay lets users cycle aspect modes from the chrome and
     * needs a way to propagate the choice down to the native video surface
     * (PlayerView.resizeMode on Android, AVPlayerLayer.videoGravity on iOS).
     *
     * Idempotent; safe to call before the surface is composed (platform actuals
     * buffer the value until the native view is created).
     */
    fun setAspectMode(mode: PlayerAspectMode)

    @Composable
    fun Surface(modifier: Modifier)

    fun release()
}
