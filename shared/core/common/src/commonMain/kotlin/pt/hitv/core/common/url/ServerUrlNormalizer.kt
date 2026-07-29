package pt.hitv.core.common.url

/**
 * Single, permissive home for normalizing user-entered IPTV server URLs. Fixes the two mistakes
 * real users actually make — stray whitespace (the #1 cause of IPTV login failures) and a missing
 * scheme — WITHOUT ever corrupting a valid URL and WITHOUT rejecting anything (the login round-trip
 * is the only real validator).
 *
 * There are two shapes with genuinely different needs, so two entry points:
 *  - [normalize] for an **Xtream base** (`scheme://host:port/`): appends exactly one trailing slash
 *    because Xtream consumers concatenate `"${host}player_api.php"`.
 *  - [normalizePlaylistUrl] for a **full direct-M3U URL** (`http://h/get.php?...`, `....m3u8`, or an
 *    extension-less `http://h/playlist`): fully shape-preserving (path + query untouched), because
 *    the playlist is fetched verbatim and adding a slash can 404 a valid URL.
 *
 * Ported verbatim from `core/common/url/ServerUrlNormalizer.kt` in the original, which the port had
 * omitted entirely. `AccountManagerRepositoryImpl` carried a partial reimplementation that trimmed
 * the ends of the string and added a trailing slash, but did not prepend a scheme, did not strip
 * interior whitespace, did not collapse repeated trailing slashes and did not strip a pasted
 * `player_api.php` endpoint. A host typed as `myserver.com:8080` was therefore stored without
 * `http://` and failed every request — on the login screen, which is the first thing a new user
 * touches.
 */
object ServerUrlNormalizer {

    private val WHITESPACE = Regex("\\s+")

    /**
     * Normalize an Xtream base host. Strips whitespace, prepends `http://` if missing, strips a
     * pasted `/player_api.php...` endpoint back to the base, and guarantees exactly one trailing
     * slash. M3U-shaped values (`?`, `.m3u`, `.m3u8`) are passed through untouched as a safety net.
     * Returns "" for blank / host-less input.
     */
    fun normalize(raw: String): String {
        val url = withSchemeAndTrimmed(raw) ?: return ""

        // Strip a pasted Xtream endpoint (…/player_api.php?…) back to the base host.
        val apiIndex = url.indexOf("/player_api.php", ignoreCase = true)
        var base = if (apiIndex >= 0) url.substring(0, apiIndex) else url

        // Safety net: never trailing-slash a value that is clearly a playlist URL.
        if (base.contains("?") ||
            base.endsWith(".m3u", ignoreCase = true) ||
            base.endsWith(".m3u8", ignoreCase = true)
        ) {
            return base
        }

        // Force exactly one trailing slash. The length guard (> "https://".length) ensures we never
        // strip the scheme's own slashes.
        while (base.endsWith("/") && base.length > 8) {
            base = base.dropLast(1)
        }
        return "$base/"
    }

    /**
     * Normalize a full direct-M3U playlist URL: whitespace stripped, scheme prepended if missing,
     * and NOTHING else — path, query and any trailing slash the server expects are preserved
     * exactly, because the playlist is fetched verbatim. Returns "" for blank / host-less input.
     */
    fun normalizePlaylistUrl(raw: String): String = withSchemeAndTrimmed(raw) ?: ""

    /**
     * Shared prefix: strip all whitespace (incl. interior, e.g. `"hi tv"`), prepend `http://` when
     * neither scheme is present, and return null when there is no authority left (blank, or input
     * like `"/"`) so callers can map that to "".
     */
    private fun withSchemeAndTrimmed(raw: String): String? {
        var url = raw.replace(WHITESPACE, "")
        if (url.isEmpty()) return null

        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            url = "http://$url"
        }

        // No authority after the scheme (e.g. input was just "/") — not a usable host.
        if (url.substringAfter("://", "").trimStart('/').isEmpty()) return null

        return url
    }
}
