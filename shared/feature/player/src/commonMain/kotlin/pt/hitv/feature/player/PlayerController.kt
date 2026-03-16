package pt.hitv.feature.player

/**
 * Cross-platform interface for player control.
 * Android implements with ExoPlayer, iOS with AVPlayer.
 */
interface PlayerController {
    fun play()
    fun pause()
    fun stop()
    fun seekTo(positionMs: Long)
    fun release()

    val isPlaying: Boolean
    val currentPosition: Long
    val duration: Long
    val playbackState: PlaybackState
}
