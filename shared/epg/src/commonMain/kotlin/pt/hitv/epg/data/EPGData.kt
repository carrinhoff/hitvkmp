package pt.hitv.epg.data

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * Interface for accessing EPG data (channels and their events).
 */
interface EPGData {
    fun getChannel(position: Int): EPGChannel?
    fun getEvents(channelPosition: Int): List<EPGEvent?>?
    fun getEvent(channelPosition: Int, programPosition: Int): EPGEvent?
    val channelCount: Int
    fun hasData(): Boolean
}
