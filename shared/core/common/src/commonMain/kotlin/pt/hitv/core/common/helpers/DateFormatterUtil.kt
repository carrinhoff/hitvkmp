package pt.hitv.core.common.helpers

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility object for formatting dates using kotlinx.datetime (multiplatform).
 */
object DateFormatterUtil {

    /**
     * Formats a Unix timestamp (in seconds) to a date string in dd-MM-yyyy format.
     * @param timestampSeconds Unix timestamp in seconds
     * @return Formatted date string (e.g., "31-12-2024")
     */
    fun formatExpirationDate(timestampSeconds: Long): String? {
        return try {
            val instant = Instant.fromEpochSeconds(timestampSeconds)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val month = localDateTime.monthNumber.toString().padStart(2, '0')
            val year = localDateTime.year
            "$day-$month-$year"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a Unix timestamp (in seconds) to a more readable format.
     * @param timestampSeconds Unix timestamp in seconds
     * @return Formatted date string (e.g., "Dec 31, 2024")
     */
    fun formatExpirationDateLong(timestampSeconds: Long): String? {
        return try {
            val instant = Instant.fromEpochSeconds(timestampSeconds)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val monthName = localDateTime.month.name.lowercase()
                .replaceFirstChar { it.uppercase() }
                .take(3)
            val day = localDateTime.dayOfMonth.toString().padStart(2, '0')
            val year = localDateTime.year
            "$monthName $day, $year"
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if a timestamp has expired (is in the past).
     * @param timestampSeconds Unix timestamp in seconds
     * @return true if expired, false otherwise
     */
    fun isExpired(timestampSeconds: Long): Boolean {
        val now = Clock.System.now().epochSeconds
        return timestampSeconds < now
    }

    /**
     * Checks if expiration is within the specified number of days.
     * @param timestampSeconds Unix timestamp in seconds
     * @param days Number of days to check
     * @return true if expiring within specified days, false otherwise
     */
    fun isExpiringSoon(timestampSeconds: Long, days: Int = 7): Boolean {
        val now = Clock.System.now().epochSeconds
        val daysInSeconds = days * 24 * 60 * 60
        return timestampSeconds > now && (timestampSeconds - now) <= daysInSeconds
    }

    /**
     * Gets the number of days until expiration.
     * @param timestampSeconds Unix timestamp in seconds
     * @return Number of days (can be negative if expired)
     */
    fun getDaysUntilExpiration(timestampSeconds: Long): Long {
        val now = Clock.System.now().epochSeconds
        val secondsRemaining = timestampSeconds - now
        return secondsRemaining / (24 * 60 * 60)
    }
}
