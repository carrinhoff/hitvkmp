package pt.hitv.core.common.media

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.hitv.core.model.Channel

/**
 * Builds catch-up / timeshift playback URLs from channel live-stream URLs.
 *
 * Ported from the original hitv Android project's `CatchUpUrlBuilder.kt`.
 * The Android version uses `SimpleDateFormat` + `java.util.Calendar`; this KMP
 * version uses `kotlinx-datetime` so it works on both Android and iOS.
 *
 * Supports the five modes emitted by different IPTV providers:
 * - XC (Xtream Codes): `/timeshift/{user}/{pass}/{duration}/{start}/{streamId}`
 * - Flussonic: `/timeshift_abs-{utcTimestamp}.ts`
 * - Shift: appends `?utc={start}&lutc={now}` to channel URL
 * - Default: uses `catchup-source` template with format specifiers
 * - Append: appends `catchup-source` (a query string) to channel URL
 */
object CatchUpUrlBuilder {

    // Xtream Codes live URL:
    //   http(s)://host[:port]/live/user/pass/streamId.ext
    //   http(s)://host[:port]/user/pass/streamId               (no /live/, no extension)
    private val XC_URL_REGEX = Regex("""^(https?://[^/]+)/(?:live/)?([^/]+)/([^/]+)/([^/.]+)(\.[^/.]+)?$""")

    // Flussonic live URL:
    //   http(s)://host[:port]/channelId/mono.m3u8?token=xxx
    private val FLUSSONIC_URL_REGEX = Regex("""^(https?://[^/]+)/([^/]+)/([^/]*)(\.[^/.?]+)(\?.+)?$""")

    /**
     * @param channel channel with `streamUrl`, `catchupType`, `catchupSource`.
     * @param programStart programme start time (UTC milliseconds).
     * @param programEnd programme end time (UTC milliseconds).
     * @param username Xtream username — used by XC mode when the live URL has no embedded credentials.
     * @param password Xtream password — same fallback as above.
     * @param serverTimezone IANA zone id (e.g. `Europe/Lisbon`) the server expects for XC start times.
     *        Falls back to device time zone when null/empty.
     * @return the replay URL, or null when the live URL can't be parsed or required fields are missing.
     */
    fun buildCatchUpUrl(
        channel: Channel,
        programStart: Long,
        programEnd: Long,
        username: String? = null,
        password: String? = null,
        serverTimezone: String? = null,
    ): String? {
        val streamUrl = channel.streamUrl ?: return null
        return when (resolveCatchUpType(channel)) {
            CatchUpType.XC -> buildXcUrl(streamUrl, programStart, programEnd, username, password, serverTimezone)
            CatchUpType.FLUSSONIC -> buildFlussonicUrl(streamUrl, programStart)
            CatchUpType.SHIFT -> buildShiftUrl(streamUrl, programStart)
            CatchUpType.DEFAULT -> buildDefaultUrl(channel.catchupSource, programStart, programEnd)
            CatchUpType.APPEND -> buildAppendUrl(streamUrl, channel.catchupSource, programStart, programEnd)
        }
    }

    private fun resolveCatchUpType(channel: Channel): CatchUpType = when (channel.catchupType?.lowercase()) {
        "xc" -> CatchUpType.XC
        "fs", "flussonic" -> CatchUpType.FLUSSONIC
        "shift" -> CatchUpType.SHIFT
        "default" -> CatchUpType.DEFAULT
        "append" -> CatchUpType.APPEND
        else -> CatchUpType.XC // default for any Xtream API source
    }

    // {host}/timeshift/{user}/{pass}/{durationMinutes}/{yyyy-MM-dd:HH-mm}/{streamId}{ext}
    private fun buildXcUrl(
        streamUrl: String,
        programStart: Long,
        programEnd: Long,
        username: String?,
        password: String?,
        serverTimezone: String?,
    ): String? {
        val match = XC_URL_REGEX.find(streamUrl) ?: return null
        val host = match.groupValues[1]
        val urlUser = match.groupValues[2]
        val urlPass = match.groupValues[3]
        val streamId = match.groupValues[4]
        val ext = match.groupValues[5] // may be empty

        // Prefer URL-embedded credentials, fall back to provided ones.
        val user = urlUser.ifEmpty { username ?: return null }
        val pass = urlPass.ifEmpty { password ?: return null }

        val durationMinutes = ((programEnd - programStart) / 60_000).toInt()
        val startFormatted = formatDateForXc(programStart, serverTimezone)

        return "$host/timeshift/$user/$pass/$durationMinutes/$startFormatted/$streamId$ext"
    }

    // {host}/{channelId}/timeshift_abs-{utcSeconds}.ts[?original-query]
    private fun buildFlussonicUrl(streamUrl: String, programStart: Long): String? {
        val match = FLUSSONIC_URL_REGEX.find(streamUrl) ?: return null
        val host = match.groupValues[1]
        val channelId = match.groupValues[2]
        val queryParams = match.groupValues[5] // may be empty, includes leading ?

        val utcSeconds = programStart / 1000
        return "$host/$channelId/timeshift_abs-$utcSeconds.ts$queryParams"
    }

    // channelUrl[?&]utc={startSeconds}&lutc={nowSeconds}
    private fun buildShiftUrl(streamUrl: String, programStart: Long): String {
        val utcSeconds = programStart / 1000
        val nowSeconds = Clock.System.now().toEpochMilliseconds() / 1000
        val separator = if (streamUrl.contains("?")) "&" else "?"
        return "$streamUrl${separator}utc=$utcSeconds&lutc=$nowSeconds"
    }

    private fun buildDefaultUrl(catchupSource: String?, programStart: Long, programEnd: Long): String? {
        if (catchupSource.isNullOrEmpty()) return null
        return replaceFormatSpecifiers(catchupSource, programStart, programEnd)
    }

    private fun buildAppendUrl(
        streamUrl: String,
        catchupSource: String?,
        programStart: Long,
        programEnd: Long,
    ): String? {
        if (catchupSource.isNullOrEmpty()) return null
        return streamUrl + replaceFormatSpecifiers(catchupSource, programStart, programEnd)
    }

    /**
     * Substitutes catch-up-source format specifiers. Matches the original
     * Android builder's set so templates shipped by providers keep working:
     *
     * - `{utc}`, `${start}` — programme start in UTC seconds
     * - `{lutc}`, `${now}`, `${timestamp}` — current time in UTC seconds
     * - `{utcend}`, `${end}` — programme end in UTC seconds
     * - `{duration}`, `${duration}` — programme length in seconds
     * - `{duration:N}` — duration divided by N (e.g. `{duration:60}` = minutes)
     * - `{offset}`, `${offset}` — seconds since programme start
     * - `{offset:N}` — offset divided by N
     * - `{Y}` `{m}` `{d}` `{H}` `{M}` `{S}` — UTC date/time components, zero-padded
     */
    private fun replaceFormatSpecifiers(template: String, programStart: Long, programEnd: Long): String {
        val utcSeconds = programStart / 1000
        val endSeconds = programEnd / 1000
        val nowSeconds = Clock.System.now().toEpochMilliseconds() / 1000
        val durationSeconds = endSeconds - utcSeconds
        val offsetSeconds = nowSeconds - utcSeconds

        val utcDt = Instant.fromEpochMilliseconds(programStart).toLocalDateTime(TimeZone.UTC)

        var result = template
        result = result.replace("{utc}", utcSeconds.toString())
        result = result.replace("\${start}", utcSeconds.toString())
        result = result.replace("{lutc}", nowSeconds.toString())
        result = result.replace("\${now}", nowSeconds.toString())
        result = result.replace("\${timestamp}", nowSeconds.toString())
        result = result.replace("{utcend}", endSeconds.toString())
        result = result.replace("\${end}", endSeconds.toString())

        val durationDivisorRegex = Regex("""\{duration:(\d+)}""")
        result = durationDivisorRegex.replace(result) { m ->
            val divisor = m.groupValues[1].toLongOrNull() ?: 1L
            (durationSeconds / divisor).toString()
        }
        result = result.replace("{duration}", durationSeconds.toString())
        result = result.replace("\${duration}", durationSeconds.toString())

        val offsetDivisorRegex = Regex("""\{offset:(\d+)}""")
        result = offsetDivisorRegex.replace(result) { m ->
            val divisor = m.groupValues[1].toLongOrNull() ?: 1L
            (offsetSeconds / divisor).toString()
        }
        result = result.replace("{offset}", offsetSeconds.toString())
        result = result.replace("\${offset}", offsetSeconds.toString())

        result = result.replace("{Y}", utcDt.year.toString().padStart(4, '0'))
        result = result.replace("{m}", utcDt.monthNumber.toString().padStart(2, '0'))
        result = result.replace("{d}", utcDt.dayOfMonth.toString().padStart(2, '0'))
        result = result.replace("{H}", utcDt.hour.toString().padStart(2, '0'))
        result = result.replace("{M}", utcDt.minute.toString().padStart(2, '0'))
        result = result.replace("{S}", utcDt.second.toString().padStart(2, '0'))
        return result
    }

    /**
     * Formats a UTC millisecond timestamp as `YYYY-MM-DD:HH-MM` in the server's
     * local time zone. XC timeshift expects server-local time, not UTC.
     *
     * `TimeZone.of("Europe/Lisbon")` throws if the zone id is unknown — fall
     * back to UTC in that case rather than crashing the URL build.
     */
    private fun formatDateForXc(timestampMillis: Long, serverTimezone: String?): String {
        val zone = if (!serverTimezone.isNullOrEmpty()) {
            runCatching { TimeZone.of(serverTimezone) }.getOrDefault(TimeZone.currentSystemDefault())
        } else {
            TimeZone.currentSystemDefault()
        }
        val dt: LocalDateTime = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(zone)
        val y = dt.year.toString().padStart(4, '0')
        val mo = dt.monthNumber.toString().padStart(2, '0')
        val d = dt.dayOfMonth.toString().padStart(2, '0')
        val h = dt.hour.toString().padStart(2, '0')
        val mi = dt.minute.toString().padStart(2, '0')
        return "$y-$mo-$d:$h-$mi"
    }

    private enum class CatchUpType { XC, FLUSSONIC, SHIFT, DEFAULT, APPEND }
}
