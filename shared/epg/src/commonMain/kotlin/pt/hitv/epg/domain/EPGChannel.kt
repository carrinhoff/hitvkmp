package pt.hitv.epg.domain

/**
 * Domain model representing a channel in the EPG (Electronic Program Guide).
 */
data class EPGChannel(
    val imageURL: String,
    val name: String,
    val channelID: String
)
