package pt.hitv.epg

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * XMLTV parser for EPG data using pure Kotlin string/regex parsing.
 *
 * Parses the standard XMLTV format:
 * ```xml
 * <tv>
 *   <channel id="...">
 *     <display-name>...</display-name>
 *     <icon src="..." />
 *   </channel>
 *   <programme start="..." stop="..." channel="...">
 *     <title>...</title>
 *     <desc>...</desc>
 *     <icon src="..." />
 *   </programme>
 * </tv>
 * ```
 *
 * No external XML library dependency -- uses regex-based extraction
 * for the well-defined XMLTV subset.
 */
object EpgParser {

    private val CHANNEL_REGEX = Regex(
        """<channel\s+id="([^"]*)">([\s\S]*?)</channel>"""
    )
    private val DISPLAY_NAME_REGEX = Regex("""<display-name[^>]*>(.*?)</display-name>""")
    private val ICON_REGEX = Regex("""<icon\s+src="([^"]*)"[^/]*/?>""")
    private val PROGRAMME_REGEX = Regex(
        """<programme\s+start="([^"]*)"\s+stop="([^"]*)"\s+channel="([^"]*)"[^>]*>([\s\S]*?)</programme>"""
    )
    private val TITLE_REGEX = Regex("""<title[^>]*>(.*?)</title>""")
    private val DESC_REGEX = Regex("""<desc[^>]*>(.*?)</desc>""")

    // XMLTV date format: 20230615120000 +0000
    private val XMLTV_DATE_REGEX = Regex("""(\d{14})\s*([+-]\d{4})?""")

    /**
     * Parse raw XMLTV content into [EpgDomainData].
     *
     * @param xmlContent The raw XMLTV XML string
     * @return Parsed EPG data with channels and programmes
     */
    fun parse(xmlContent: String): EpgDomainData {
        val channels = parseChannels(xmlContent)
        val programmes = parseProgrammes(xmlContent)
        return EpgDomainData(channels = channels, programmes = programmes)
    }

    /**
     * Parse channel elements from XMLTV content.
     */
    fun parseChannels(xmlContent: String): List<EPGChannel> {
        return CHANNEL_REGEX.findAll(xmlContent).map { match ->
            val channelId = match.groupValues[1]
            val body = match.groupValues[2]
            val displayName = DISPLAY_NAME_REGEX.find(body)?.groupValues?.get(1)?.decodeXmlEntities() ?: ""
            val iconUrl = ICON_REGEX.find(body)?.groupValues?.get(1) ?: ""

            EPGChannel(
                channelID = channelId,
                name = displayName,
                imageURL = iconUrl
            )
        }.toList()
    }

    /**
     * Parse programme elements from XMLTV content, grouped by channel ID.
     */
    fun parseProgrammes(xmlContent: String): Map<String, List<EPGEvent>> {
        val programmesMap = mutableMapOf<String, MutableList<EPGEvent>>()
        var idCounter = 0

        PROGRAMME_REGEX.findAll(xmlContent).forEach { match ->
            val startStr = match.groupValues[1]
            val stopStr = match.groupValues[2]
            val channelId = match.groupValues[3]
            val body = match.groupValues[4]

            val title = TITLE_REGEX.find(body)?.groupValues?.get(1)?.decodeXmlEntities() ?: ""
            val desc = DESC_REGEX.find(body)?.groupValues?.get(1)?.decodeXmlEntities() ?: ""
            val iconUrl = ICON_REGEX.find(body)?.groupValues?.get(1) ?: ""

            val startMillis = parseXmltvDate(startStr)
            val stopMillis = parseXmltvDate(stopStr)

            if (startMillis != null && stopMillis != null) {
                val event = EPGEvent(
                    id = "epg_${idCounter++}",
                    start = startMillis,
                    end = stopMillis,
                    title = title,
                    description = desc,
                    imageURL = iconUrl
                )
                programmesMap.getOrPut(channelId) { mutableListOf() }.add(event)
            }
        }

        return programmesMap
    }

    /**
     * Parse XMLTV date format (e.g., "20230615120000 +0000") to epoch milliseconds.
     *
     * Format: YYYYMMDDHHmmss [+/-HHMM]
     */
    internal fun parseXmltvDate(dateStr: String): Long? {
        val match = XMLTV_DATE_REGEX.find(dateStr.trim()) ?: return null
        val dateDigits = match.groupValues[1]
        val offsetStr = match.groupValues[2].ifEmpty { "+0000" }

        if (dateDigits.length != 14) return null

        return try {
            val year = dateDigits.substring(0, 4).toInt()
            val month = dateDigits.substring(4, 6).toInt()
            val day = dateDigits.substring(6, 8).toInt()
            val hour = dateDigits.substring(8, 10).toInt()
            val minute = dateDigits.substring(10, 12).toInt()
            val second = dateDigits.substring(12, 14).toInt()

            // Parse timezone offset
            val offsetSign = if (offsetStr.startsWith('-')) -1 else 1
            val offsetHours = offsetStr.substring(1, 3).toInt()
            val offsetMinutes = offsetStr.substring(3, 5).toInt()
            val totalOffsetMinutes = offsetSign * (offsetHours * 60 + offsetMinutes)

            // Calculate epoch millis using a simple approach
            // Days from epoch to the given date
            val daysSinceEpoch = daysSinceEpoch(year, month, day)
            val utcMillis = daysSinceEpoch * 86400000L +
                    hour * 3600000L +
                    minute * 60000L +
                    second * 1000L -
                    totalOffsetMinutes * 60000L

            utcMillis
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calculate days since Unix epoch (1970-01-01) for a given date.
     */
    private fun daysSinceEpoch(year: Int, month: Int, day: Int): Long {
        // Adjusted month (March = 1, ..., February = 12 of previous year)
        val y = if (month <= 2) year - 1 else year
        val m = if (month <= 2) month + 9 else month - 3
        // Days from March 1, year 0 to target date
        val dayOfYear = (153 * m + 2) / 5 + day - 1
        val yearDays = 365L * y + y / 4 - y / 100 + y / 400
        // Epoch offset: days from year 0 March 1 to 1970-01-01
        val epochOffset = 719468L
        return yearDays + dayOfYear - epochOffset
    }

    private fun String.decodeXmlEntities(): String {
        return this
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
