package pt.hitv.epg.data

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * Listener interface for EPG user interactions.
 */
interface EPGClickListener {
    fun onChannelClicked(channelPosition: Int, epgChannel: EPGChannel?)
    fun onEventClicked(channelPosition: Int, programPosition: Int, epgEvent: EPGEvent?)
    fun onResetButtonClicked()
}
