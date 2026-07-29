package pt.hitv.core.common.util

/**
 * Normalizes the many shapes an Xtream `youtube_trailer` field can take into a bare 11-character
 * YouTube video ID.
 *
 * Faithful port of the original's `core/common/.../util/YouTubeUrl.kt`, which the KMP port had
 * dropped entirely. Providers are wildly inconsistent: some send a bare ID (`dQw4w9WgXcQ`), many
 * send a full URL (`watch?v=`, `youtu.be/`, `/embed/`, `/shorts/`, `/v/`), and plenty send an empty
 * string or junk.
 *
 * Without normalization the port had two visible bugs:
 *
 *  - **Broken links.** The trailer action built `https://www.youtube.com/watch?v=$raw`, so a field
 *    already containing a URL produced `watch?v=https://youtu.be/abc`, which opens nothing useful.
 *  - **Phantom buttons.** The trailer button was gated on `!isNullOrBlank()`, so any non-empty
 *    junk rendered a "Watch trailer" button that led nowhere.
 *
 * [extractVideoId] returns a valid ID or null, so callers can use the same call to gate the button
 * and to build the URL.
 */
object YouTubeUrl {

    private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

    // Ordered by specificity; the first capture that is a valid 11-char id wins.
    private val URL_PATTERNS = listOf(
        Regex("""[?&]v=([A-Za-z0-9_-]{11})"""),      // youtube.com/watch?v=ID
        Regex("""youtu\.be/([A-Za-z0-9_-]{11})"""),  // youtu.be/ID
        Regex("""/embed/([A-Za-z0-9_-]{11})"""),     // youtube.com/embed/ID
        Regex("""/shorts/([A-Za-z0-9_-]{11})"""),    // youtube.com/shorts/ID
        Regex("""/v/([A-Za-z0-9_-]{11})"""),         // youtube.com/v/ID
    )

    /**
     * @return an 11-char YouTube video id, or null when [raw] is blank or contains no recognizable
     *   id. Use the null case to hide the trailer affordance entirely.
     */
    fun extractVideoId(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null

        // Already a bare id — use as-is.
        if (VIDEO_ID.matches(value)) return value

        for (pattern in URL_PATTERNS) {
            pattern.find(value)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
    }

    /**
     * Full watch URL for [raw], or null when there is no usable id.
     *
     * The port opens trailers in the browser rather than the in-app player the original uses —
     * that gap is tracked separately in KMP_MIGRATION_AUDIT.md — but the URL it opens should at
     * least be correct.
     */
    fun watchUrlOrNull(raw: String?): String? =
        extractVideoId(raw)?.let { "https://www.youtube.com/watch?v=$it" }
}
