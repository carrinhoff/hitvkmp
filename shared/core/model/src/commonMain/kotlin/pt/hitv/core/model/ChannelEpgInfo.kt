package pt.hitv.core.model

/**
 * Domain model representing EPG (Electronic Program Guide) information for a channel.
 *
 * This is the presentation-layer representation of EPG data, abstracting away
 * database-specific details from the UI layer.
 *
 * @property channelId The channel's EPG identifier
 * @property channelName The display name of the channel
 * @property programmeTitle The title of the current/upcoming programme
 * @property programmeDescription The description of the programme
 * @property startTime The programme start time as Unix timestamp in milliseconds
 * @property endTime The programme end time as Unix timestamp in milliseconds
 * @property logo The URL of the channel logo (from EPG source)
 */
data class ChannelEpgInfo(
    val channelId: String?,
    val channelName: String?,
    val programmeTitle: String?,
    val programmeDescription: String?,
    val startTime: Long?,
    val endTime: Long?,
    val logo: String? = null
) {
    /**
     * Checks if this EPG info has valid programme data.
     */
    val hasProgrammeData: Boolean
        get() = !programmeTitle.isNullOrBlank()

    /**
     * Calculates the progress percentage of the current programme.
     * @param currentTimeMillis The current time in milliseconds
     * @return Progress as a value between 0.0 and 1.0, or 0.0 if times are invalid
     */
    fun calculateProgress(currentTimeMillis: Long): Float {
        val start = startTime ?: return 0f
        val end = endTime ?: return 0f
        if (end <= start || currentTimeMillis < start) return 0f
        if (currentTimeMillis >= end) return 1f
        return ((currentTimeMillis - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Calculates the remaining time in minutes.
     * @param currentTimeMillis The current time in milliseconds
     * @return Remaining minutes, or 0 if times are invalid
     */
    fun remainingMinutes(currentTimeMillis: Long): Long {
        val end = endTime ?: return 0
        val remaining = (end - currentTimeMillis) / 60000
        return if (remaining > 0) remaining else 0
    }
}
