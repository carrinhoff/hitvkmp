package pt.hitv.feature.player

/**
 * Extracts the numeric stream id from a playback URL.
 *
 * Matches the original hitv behavior exactly — take the segment after the last
 * `/`, strip anything from the first `.` onward, parse as Int. Returns 0 when the
 * URL is malformed or the segment is not numeric. Intentionally lenient so unusual
 * URL shapes (query strings, missing extensions) don't break position save/resume.
 */
object PlayerStreamIdExtractor {
    fun extract(url: String): Int =
        url.substringAfterLast("/").substringBefore(".").toIntOrNull() ?: 0
}
