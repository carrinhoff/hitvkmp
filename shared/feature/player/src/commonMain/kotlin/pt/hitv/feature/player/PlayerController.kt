package pt.hitv.feature.player

import kotlinx.coroutines.flow.StateFlow

/**
 * Playback state projected onto a cross-platform enum.
 * Android ExoPlayer and iOS AVPlayer both surface into this.
 */
enum class PlaybackState { Idle, Buffering, Ready, Ended, Error }

/**
 * Flow-based cross-platform player controller.
 * Android implements via ExoPlayer, iOS via AVPlayer.
 */
interface PlayerController {
    val isPlaying: StateFlow<Boolean>
    val currentPositionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val playbackState: StateFlow<PlaybackState>
    val error: StateFlow<String?>

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun release()
}
