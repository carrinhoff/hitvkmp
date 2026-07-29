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
        if (outputFormat.isNullOrEmpty()) return trimmed
        return withExtension(trimmed, outputFormat)
    }

    /**
     * Live-stream variant for AVFoundation. Use this wherever an iOS `AVPlayer` is handed a **live**
     * channel URL; [normalize] remains correct for VOD, whose URLs already carry a container
     * extension.
     *
     * ## Why iOS cannot use [normalize] here
     *
     * [normalize] appends the `output` preference, which is
     * `allowedOutputFormats.firstOrNull() ?: ""`. Two very common cases produce a URL AVPlayer
     * cannot play:
     *
     *  - **`output` is empty** — always true for M3U accounts, which have no Xtream `user_info`,
     *    and true for any provider that does not report the field. The URL is then left
     *    extension-less and an Xtream server serves raw MPEG-TS.
     *  - **`output` is `"ts"`** — a normal thing for a provider to list first.
     *
     * ExoPlayer plays raw MPEG-TS over HTTP quite happily, so the original — Android-only — never
     * had to care, and neither does this port's Android side. **AVFoundation does not.** It handles
     * HLS and progressive MP4/MOV; a raw TS stream fails. So the exact accounts that work on
     * Android would show a channel that never starts on iOS.
     *
     * This therefore always requests `.m3u8` when the URL carries no extension of its own. If a
     * provider genuinely offers no HLS endpoint the request 404s — but `.ts` would have failed to
     * decode anyway, so this is never worse and is usually the difference between playing and not.
     *
     * A `.ts` URL is **rewritten** to `.m3u8` rather than left alone. That case is not hypothetical:
     * `CatchUpUrlBuilder` emits Flussonic timeshift URLs as `…/timeshift_abs-{utc}.ts`, which is
     * the documented Flussonic convention and plays fine on Android. Flussonic serves the same
     * recording as HLS at the `.m3u8` variant, and AVFoundation can play only that one. A provider
     * with no HLS variant fails either way, so the rewrite is never worse.
     *
     * **A deliberate, platform-driven divergence from the original**, which simply appends whatever
     * `output` holds.
     */
    fun normalizeLiveForAvPlayer(url: String): String {
        val trimmed = url.trim()
        val separator = trimmed.indexOfFirst { it == '?' || it == '#' }
        val path = if (separator >= 0) trimmed.substring(0, separator) else trimmed
        val suffix = if (separator >= 0) trimmed.substring(separator) else ""
        return if (path.endsWith(".$TS_EXTENSION", ignoreCase = true)) {
            path.dropLast(TS_EXTENSION.length) + HLS_EXTENSION + suffix
        } else {
            withExtension(trimmed, HLS_EXTENSION)
        }
    }

    /**
     * Appends `.extension` unless the URL's **path** already ends in a known one.
     *
     * The query string is deliberately excluded from both the check and the append. Testing
     * `endsWith` against the whole URL misses an extension whenever a provider appends a token, so
     * `…/12345.m3u8?token=abc` looked extension-less and became
     * `…/12345.m3u8?token=abc.ts` — a URL that cannot resolve. The original guards the same case
     * with an extra `!url.contains(".m3u8")` check, which this port had dropped; splitting on the
     * query is the general form of that guard and also fixes the append side, since an extension
     * belongs on the path rather than after the query.
     */
    private fun withExtension(url: String, extension: String): String {
        val separator = url.indexOfFirst { it == '?' || it == '#' }
        val path = if (separator >= 0) url.substring(0, separator) else url
        val suffix = if (separator >= 0) url.substring(separator) else ""
        val hasKnownExtension = KNOWN_EXTENSIONS.any { path.endsWith(it, ignoreCase = true) }
        return if (hasKnownExtension) url else "$path.$extension$suffix"
    }

    private const val HLS_EXTENSION = "m3u8"
    private const val TS_EXTENSION = "ts"
}
