package pt.hitv.epg

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Utility functions for EPG display formatting.
 *
 * Uses kotlinx-datetime instead of Joda Time for multiplatform compatibility.
 */
object EpgUtils {

    /**
     * Formats a timestamp to short time string (e.g., "14:30").
     */
    fun getShortTime(timeMillis: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(timeMillis)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = localDateTime.hour.toString().padStart(2, '0')
            val minute = localDateTime.minute.toString().padStart(2, '0')
            "$hour:$minute"
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Gets the day-of-week name for a given timestamp.
     */
    fun getWeekdayName(dateMillis: Long): String {
        return try {
            val instant = Instant.fromEpochMilliseconds(dateMillis)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            localDateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Calculate the progress percentage of a programme.
     *
     * @param startMillis Programme start time in epoch milliseconds
     * @param endMillis Programme end time in epoch milliseconds
     * @param currentMillis Current time in epoch milliseconds
     * @return Progress as a value between 0.0 and 1.0
     */
    fun calculateProgress(startMillis: Long, endMillis: Long, currentMillis: Long): Float {
        if (endMillis <= startMillis || currentMillis < startMillis) return 0f
        if (currentMillis >= endMillis) return 1f
        return ((currentMillis - startMillis).toFloat() / (endMillis - startMillis).toFloat())
            .coerceIn(0f, 1f)
    }

    /**
     * Calculate remaining minutes for a programme.
     *
     * @param endMillis Programme end time in epoch milliseconds
     * @param currentMillis Current time in epoch milliseconds
     * @return Remaining minutes, or 0 if already ended
     */
    fun remainingMinutes(endMillis: Long, currentMillis: Long): Long {
        val remaining = (endMillis - currentMillis) / 60000
        return if (remaining > 0) remaining else 0
    }

    // ===== Grid helpers =====
    //
    // The original used java.util.Calendar and SimpleDateFormat for these. kotlinx-datetime has
    // no pattern formatter on all targets, so the short formats are assembled by hand — keeping
    // the same output shape the original produced.

    private val shortDayNames =
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val shortMonthNames =
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    private fun localDateTime(millis: Long) =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())

    /**
     * The grid's left edge: now, rounded *down* to the previous half hour. Mirrors the original's
     * `getRoundedStartTime()` — programmes are laid out relative to this, so it must land exactly
     * on a :00 or :30 boundary for the timeline labels to line up with the blocks.
     */
    fun roundedHalfHourStart(nowMillis: Long): Long {
        val dt = localDateTime(nowMillis)
        val flooredMinute = if (dt.minute >= 30) 30 else 0
        val secondsIntoMinute = dt.second.toLong()
        val millisIntoSecond = (nowMillis % 1000 + 1000) % 1000
        val minutesToDrop = (dt.minute - flooredMinute).toLong()
        return nowMillis -
            millisIntoSecond -
            secondsIntoMinute * 1000L -
            minutesToDrop * 60_000L
    }

    /** "Sat, 26 Jul" — the grid's date header. Matches the original's "EEE, d MMM". */
    fun formatDayAndMonth(millis: Long): String {
        val dt = localDateTime(millis)
        val day = shortDayNames.getOrElse(dt.dayOfWeek.ordinal) { "" }
        val month = shortMonthNames.getOrElse(dt.monthNumber - 1) { "" }
        return "$day, ${dt.dayOfMonth} $month"
    }

    /** "Sat" — date chip primary label. */
    fun formatShortWeekday(millis: Long): String =
        shortDayNames.getOrElse(localDateTime(millis).dayOfWeek.ordinal) { "" }

    /** "Jul 26" — date chip secondary label. Matches the original's "MMM d". */
    fun formatMonthAndDay(millis: Long): String {
        val dt = localDateTime(millis)
        val month = shortMonthNames.getOrElse(dt.monthNumber - 1) { "" }
        return "$month ${dt.dayOfMonth}"
    }

    /** "Saturday, Jul 26" — archive-sheet day header. Matches the original's "EEEE, MMM d". */
    fun formatFullWeekdayAndDate(millis: Long): String {
        val dt = localDateTime(millis)
        val day = dt.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val month = shortMonthNames.getOrElse(dt.monthNumber - 1) { "" }
        return "$day, $month ${dt.dayOfMonth}"
    }

    /** True when both instants fall on the same local calendar day. */
    fun isSameDay(millisA: Long, millisB: Long): Boolean {
        val a = localDateTime(millisA)
        val b = localDateTime(millisB)
        return a.year == b.year && a.dayOfYear == b.dayOfYear
    }

    /** Local midnight for the day containing [millis]. Used to group archive programmes. */
    fun startOfLocalDay(millis: Long): Long {
        val dt = localDateTime(millis)
        val zone = TimeZone.currentSystemDefault()
        return kotlinx.datetime.LocalDateTime(dt.year, dt.monthNumber, dt.dayOfMonth, 0, 0, 0)
            .toInstant(zone)
            .toEpochMilliseconds()
    }

    /** Local noon for the day [daysAgo] before the day containing [nowMillis]. */
    fun localNoonDaysAgo(nowMillis: Long, daysAgo: Int): Long =
        startOfLocalDay(nowMillis) - daysAgo * MILLIS_PER_DAY + MILLIS_PER_HALF_DAY

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
    private const val MILLIS_PER_HALF_DAY = 12L * 60 * 60 * 1000
}
