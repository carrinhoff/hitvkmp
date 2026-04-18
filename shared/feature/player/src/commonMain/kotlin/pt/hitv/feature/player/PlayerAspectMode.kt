package pt.hitv.feature.player

/**
 * Shared aspect-ratio resize mode for the video surface.
 *
 * Platforms map this to their native equivalents:
 * - Android (ExoPlayer) → `AspectRatioFrameLayout.RESIZE_MODE_FIT/FILL/ZOOM`
 * - iOS (AVPlayerLayer) → `AVLayerVideoGravity.resizeAspect/.resize/.resizeAspectFill`
 */
enum class PlayerAspectMode {
    Fit,
    Fill,
    Zoom;

    /** Returns the next mode in the cycle: Fit → Fill → Zoom → Fit. */
    fun cycle(): PlayerAspectMode = when (this) {
        Fit -> Fill
        Fill -> Zoom
        Zoom -> Fit
    }
}
