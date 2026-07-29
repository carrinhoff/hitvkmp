package pt.hitv.feature.player

/**
 * Platform-independent configuration for a [PlayerHost] instance.
 *
 * @property bufferProfile Controls how aggressively the native player buffers. Picks
 *   one of the preset profiles declared by [BufferProfile].
 * @property initialAspectMode The aspect-ratio mode the player surface should boot
 *   with. Usually [PlayerAspectMode.Fit] to match original hitv behavior.
 */
data class PlayerConfig(
    val bufferProfile: BufferProfile,
    val initialAspectMode: PlayerAspectMode = PlayerAspectMode.Fit
)

/**
 * High-level buffering strategy selector.
 *
 * - [Vod] — Movie / Series (full-VOD): the original hitv numbers (10s min buffer,
 *   50s max, 1.5s playback, 3s rebuffer).
 * - [Live] — Channel / live streams: shorter buffers to keep latency low.
 * - [Preview] — Small inline previews (channel grid thumbnails): even shorter.
 */
enum class BufferProfile {
    Vod,
    Live,
    Preview
}

/**
 * Concrete buffer duration numbers used by platform actuals when they translate a
 * [BufferProfile] into the native load-control values (`DefaultLoadControl` on
 * Android, `preferredForwardBufferDuration` on iOS).
 */
data class BufferDurations(
    val minMs: Int,
    val maxMs: Int,
    val playbackMs: Int,
    val rebufferMs: Int
)

/**
 * Shared commonMain constants that platforms read when configuring their native
 * players. Keeps the VOD numbers locked to the original hitv project so parity is
 * mechanical, not a re-tune.
 */
object PlayerConfigFactory {
    /** Matches the original hitv `PlayerConfigFactory.VOD` load-control values. */
    val VOD_BUFFER: BufferDurations = BufferDurations(
        minMs = 10_000,
        maxMs = 50_000,
        playbackMs = 1_500,
        rebufferMs = 3_000
    )

    /** Initial bandwidth estimate (bits per second) used by the native player. */
    const val INITIAL_BITRATE_ESTIMATE: Long = 2_000_000L

    /** Preference value used when the user has never chosen a live buffer size. */
    const val DEFAULT_LIVE_BUFFER: String = "medium"

    /**
     * Live buffer durations for the user's "Live Buffer Size" setting.
     *
     * Ported verbatim from the original's
     * `core/common/.../media/PlayerConfigFactory.kt:29-39` (`createLiveLoadControl`), which maps
     * the same four preference values onto `DefaultLoadControl` buffer durations.
     *
     * The port previously read and wrote the preference but **no player ever consumed it** — the
     * setting was inert, so changing it did nothing. The original wires it into
     * `PlaybackManager.createExoPlayer` and `ExoPlayerController`. Buffer sizing matters most on
     * exactly the mobile networks this app is used on, so a dead control here is not cosmetic.
     *
     * Unknown values fall back to `medium`, matching the original's `else` branch.
     */
    fun liveBufferFor(bufferSize: String?): BufferDurations = when (bufferSize) {
        "small" -> BufferDurations(minMs = 3_000, maxMs = 10_000, playbackMs = 500, rebufferMs = 1_500)
        "large" -> BufferDurations(minMs = 15_000, maxMs = 50_000, playbackMs = 2_000, rebufferMs = 5_000)
        "very_large" -> BufferDurations(minMs = 30_000, maxMs = 120_000, playbackMs = 3_000, rebufferMs = 8_000)
        else -> BufferDurations(minMs = 5_000, maxMs = 20_000, playbackMs = 800, rebufferMs = 2_000)
    }
}
