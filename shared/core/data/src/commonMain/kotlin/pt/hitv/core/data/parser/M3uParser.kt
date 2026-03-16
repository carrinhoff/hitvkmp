package pt.hitv.core.data.parser

import pt.hitv.core.model.Channel

/**
 * Parses M3U/M3U8 playlist content into domain models.
 *
 * Handles standard M3U format with #EXTINF directives, extracting channel metadata
 * (name, logo, group, EPG ID) and EPG URLs from the playlist header.
 */
class M3uParser {

    /**
     * Result of parsing an M3U playlist.
     *
     * @param channels The list of parsed channels from EXTINF entries.
     * @param epgUrls EPG guide URLs extracted from the M3U header (url-tvg, x-tvg-url).
     */
    data class M3uParseResult(
        val channels: List<Channel>,
        val epgUrls: List<String>
    )

    /**
     * Parses M3U content and returns both channels and EPG URLs.
     */
    fun parse(content: String): M3uParseResult {
        return M3uParseResult(
            channels = parseChannels(content),
            epgUrls = extractEpgUrls(content)
        )
    }

    /**
     * Parses M3U content and returns the list of channels.
     */
    fun parseChannels(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentExtInf: String? = null

        for (line in lines) {
            val trimmedLine = line.trim()
            when {
                trimmedLine.startsWith("#EXTINF:") -> {
                    currentExtInf = trimmedLine
                }
                trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && currentExtInf != null -> {
                    val channel = parseExtInfLine(currentExtInf, trimmedLine)
                    if (channel != null) {
                        channels.add(channel)
                    }
                    currentExtInf = null
                }
            }
        }
        return channels
    }

    /**
     * Extracts EPG guide URLs from the M3U header (#EXTM3U line).
     */
    fun extractEpgUrls(content: String): List<String> {
        val epgUrls = mutableListOf<String>()
        val lines = content.lines().take(50)

        for (line in lines) {
            if (line.startsWith("#EXTM3U")) {
                URL_TVG_REGEX.find(line)?.groupValues?.get(1)?.let {
                    if (it.isNotBlank()) epgUrls.add(it)
                }
                X_TVG_URL_REGEX.find(line)?.groupValues?.get(1)?.let {
                    if (it.isNotBlank()) epgUrls.add(it)
                }
                break
            }
        }
        return epgUrls.distinct()
    }

    /**
     * Parses a single #EXTINF line paired with its stream URL into a [Channel].
     */
    internal fun parseExtInfLine(extInf: String, url: String): Channel? {
        return try {
            val tvgId = TVG_ID_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
            val tvgName = TVG_NAME_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
            val tvgLogo = TVG_LOGO_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
            val groupTitle = GROUP_TITLE_REGEX.find(extInf)?.groupValues?.get(1) ?: "Uncategorized"
            val channelName = CHANNEL_NAME_REGEX.find(extInf)?.groupValues?.get(1)?.trim() ?: tvgName

            if (channelName.isEmpty() && tvgName.isEmpty()) return null

            Channel(
                name = channelName.ifEmpty { tvgName },
                streamIcon = tvgLogo,
                streamUrl = url,
                epgChannelId = tvgId,
                categoryId = groupTitle
            )
        } catch (e: Exception) {
            // Log omitted for KMP - caller can handle null
            null
        }
    }

    companion object {
        // Pre-compiled regex patterns for EXTINF attribute extraction
        private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""")
        private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""")
        private val TVG_LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""")
        private val GROUP_TITLE_REGEX = Regex("""group-title="([^"]*)"""")
        private val CHANNEL_NAME_REGEX = Regex(""",(.+)$""")

        // Pre-compiled regex patterns for EPG URL extraction from header
        private val URL_TVG_REGEX = Regex("""url-tvg="([^"]*)"""")
        private val X_TVG_URL_REGEX = Regex("""x-tvg-url="([^"]*)"""")
    }
}
