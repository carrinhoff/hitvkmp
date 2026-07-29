package pt.hitv.epg.data

import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * Adapts the flat `List<ChannelEpgInfo>` the repository returns into the channel-major [EPGData]
 * shape the grid renders from.
 *
 * Faithful port of the original's `EpgDataFilter.kt` (hitv/feature/channels/.../epg): groups by
 * (channelId, channelName, logo), builds one [EPGEvent] per programme row with the same
 * `"${channelId}_${startTime}"` id, drops channels with no events, and takes `hasCatchUp` from the
 * first row of each group.
 *
 * One correction against the original: events are sorted by start time. The original relied on the
 * SQL `ORDER BY` alone, but the grid's `ProgramsCanvas` does an `indexOfFirst` visibility scan and
 * `break`s on the first block past the right edge — which silently truncates a row if the events
 * are not monotonically ordered. Sorting here makes that invariant local to the data, rather than
 * a property of whichever query happened to produce the list.
 */
fun filterEpgData(fullEpg: List<ChannelEpgInfo>): EPGData {
    val channelToEventsMap: Map<EPGChannel, List<EPGEvent>> = fullEpg
        .groupBy { Triple(it.channelId, it.channelName, it.logo) }
        .mapNotNull { (channelKey, channelItems) ->
            val events = channelItems
                .map { item ->
                    EPGEvent(
                        id = "${item.channelId}_${item.startTime}",
                        title = item.programmeTitle ?: "",
                        start = item.startTime ?: 0L,
                        end = item.endTime ?: 0L,
                        description = item.programmeDescription ?: "",
                        imageURL = item.logo ?: "",
                    )
                }
                .sortedBy { it.start }

            if (events.isEmpty()) return@mapNotNull null

            val epgChannel = EPGChannel(
                imageURL = channelKey.third ?: "",
                name = channelKey.second ?: "",
                channelID = channelKey.first ?: "",
                hasCatchUp = channelItems.firstOrNull()?.hasCatchUp ?: false,
            )
            epgChannel to events
        }
        .toMap()

    return object : EPGData {
        private val channelList = channelToEventsMap.keys.toList()

        override val channelCount: Int get() = channelList.size

        override fun getChannel(position: Int): EPGChannel? = channelList.getOrNull(position)

        override fun getEvents(channelPosition: Int): List<EPGEvent?>? {
            val channel = getChannel(channelPosition) ?: return null
            return channelToEventsMap[channel]
        }

        override fun getEvent(channelPosition: Int, programPosition: Int): EPGEvent? =
            getEvents(channelPosition)?.getOrNull(programPosition)

        override fun hasData(): Boolean = channelList.isNotEmpty()
    }
}
