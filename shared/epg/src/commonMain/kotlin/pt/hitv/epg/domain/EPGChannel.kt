package pt.hitv.epg.domain

/**
 * Domain model representing a channel in the EPG (Electronic Program Guide).
 */
data class EPGChannel(
    val imageURL: String,
    val name: String,
    val channelID: String,
    /**
     * Whether this channel supports catch-up / timeshift. Drives the replay affordance on past
     * programmes in the grid and the long-press channel archive sheet.
     */
    val hasCatchUp: Boolean = false
)
