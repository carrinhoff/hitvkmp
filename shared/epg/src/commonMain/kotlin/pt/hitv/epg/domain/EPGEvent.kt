package pt.hitv.epg.domain

import kotlinx.datetime.Clock

/**
 * Domain model representing a programme event in the EPG.
 */
data class EPGEvent(
    val id: String,
    val start: Long,
    val end: Long,
    val title: String,
    val description: String,
    val imageURL: String
) {
    /**
     * Whether this event is currently airing.
     */
    val isCurrent: Boolean
        get() {
            val now = Clock.System.now().toEpochMilliseconds()
            return now in start..end
        }

    /** Whether this programme has already finished. Mirrors the original's `isPast`. */
    val isPast: Boolean
        get() = Clock.System.now().toEpochMilliseconds() > end
}
