package pt.hitv.feature.player

/**
 * Collapses the three near-identical URL-normalize helpers (Android Movie,
 * Android Series, iOS launcher, plus the inline block in ChannelPreviewActual)
 * into a single shared implementation.
 *
 * Matches the behavior of `MoviePlayerActivity.normalizeUrl` / `SeriesPlayerActivity.normalizeUrl`:
 * trim the URL, and if the caller supplies a non-empty `outputFormat` preference
 * AND the URL does NOT already end with a known stream extension, append
 * `".$outputFormat"`.
 *
 * Callers are responsible for reading the `output` preference themselves (typically
 * via `PreferencesHelper.getStoredTag("output")`).
 */
object MediaUrlNormalizer {

    private val KNOWN_EXTENSIONS = listOf(
        ".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm"
    )

    /**
     * @param url Raw URL (may contain whitespace).
     * @param outputFormat The value of the user's `output` preference, or null/empty
     *   when unset. When non-empty and the URL lacks a known extension, the format
     *   is appended.
     * @return Normalized URL safe to hand to the native player.
     */
    fun normalize(url: String, outputFormat: String?): String {
        val trimmed = url.trim()
        val hasKnownExtension = KNOWN_EXTENSIONS.any { trimmed.endsWith(it, ignoreCase = true) }
        return if (!outputFormat.isNullOrEmpty() && !hasKnownExtension) {
            "$trimmed.$outputFormat"
        } else {
            trimmed
        }
    }
}
