package pt.hitv.epg

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
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
}
