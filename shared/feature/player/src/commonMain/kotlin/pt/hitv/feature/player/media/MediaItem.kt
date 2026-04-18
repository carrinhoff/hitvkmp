package pt.hitv.feature.player.media

/**
 * A single playback item for the shared player.
 *
 * Intentionally distinct from `androidx.media3.common.MediaItem` so the shared layer
 * does not depend on media3. Platform actuals translate [MediaItem] to their native
 * equivalents (media3 MediaItem on Android, AVPlayerItem on iOS).
 *
 * External subtitle loading is deferred — [subtitles] is a placeholder the player
 * surface can wire later.
 *
 * @property url Remote or local playback URL. Already normalized — see
 *   [pt.hitv.feature.player.MediaUrlNormalizer].
 * @property title Human-readable title used by the overlay chrome.
 * @property subtitles Optional external subtitle tracks (wiring is future work).
 * @property tag Caller-provided payload (e.g. episode id, stream id) — opaque to
 *   the player.
 */
data class MediaItem(
    val url: String,
    val title: String,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val tag: Any? = null
)

/**
 * External subtitle track descriptor. Placeholder for future wiring — no actual
 * loading logic is required in the initial shared-foundation milestone.
 */
data class SubtitleTrack(
    val url: String,
    val language: String?,
    val label: String?,
    val mimeType: String?
)
