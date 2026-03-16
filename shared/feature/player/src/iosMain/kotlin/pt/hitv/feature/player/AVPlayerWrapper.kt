package pt.hitv.feature.player

/**
 * Stub for AVPlayer implementation of PlayerController.
 * TODO: AVPlayer integration via Kotlin/Native interop.
 * This will be implemented with actual AVPlayer calls.
 */
class AVPlayerWrapper : PlayerController {
    override fun play() { /* TODO: AVPlayer play */ }
    override fun pause() { /* TODO: AVPlayer pause */ }
    override fun stop() { /* TODO: AVPlayer stop */ }
    override fun seekTo(positionMs: Long) { /* TODO: AVPlayer seekTo */ }
    override fun release() { /* TODO: AVPlayer release */ }

    override val isPlaying: Boolean get() = false
    override val currentPosition: Long get() = 0L
    override val duration: Long get() = 0L
    override val playbackState: PlaybackState get() = PlaybackState.Idle
}
