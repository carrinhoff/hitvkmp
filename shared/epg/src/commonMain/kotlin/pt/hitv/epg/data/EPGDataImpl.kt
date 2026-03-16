package pt.hitv.epg.data

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * In-memory implementation of [EPGData] backed by a map of channels to events.
 */
class EPGDataImpl(data: Map<EPGChannel, List<EPGEvent>>) : EPGData {

    private val channels: List<EPGChannel> = data.keys.toList()
    private val events: List<List<EPGEvent?>> = data.values.toList()

    override fun getChannel(position: Int): EPGChannel? {
        return channels.getOrNull(position)
    }

    override fun getEvents(channelPosition: Int): List<EPGEvent?>? {
        return events.getOrNull(channelPosition)
    }

    override fun getEvent(channelPosition: Int, programPosition: Int): EPGEvent? {
        return events.getOrNull(channelPosition)?.getOrNull(programPosition)
    }

    override val channelCount: Int
        get() = channels.size

    override fun hasData(): Boolean {
        return channels.isNotEmpty()
    }
}
