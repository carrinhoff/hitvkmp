package pt.hitv.epg

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * Container for EPG domain data parsed from an XMLTV source.
 *
 * @property channels List of channels with their EPG identifiers
 * @property programmes Map of channel ID to list of programme events
 */
data class EpgDomainData(
    val channels: List<EPGChannel>,
    val programmes: Map<String, List<EPGEvent>>
)
