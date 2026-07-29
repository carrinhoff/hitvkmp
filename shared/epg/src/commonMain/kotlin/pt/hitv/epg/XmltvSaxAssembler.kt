package pt.hitv.epg

import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent

/**
 * Builds [EpgDomainData] from a stream of SAX events, so a feed never has to exist in memory as a
 * single string.
 *
 * ## Why this exists
 *
 * [EpgParser] runs regexes over the whole document. That is fine on Android, which streams the
 * response through `XmlPullParser` and never materialises it — but the iOS actual was downloading
 * the body into `NSData`, copying it into a `ByteArray`, and decoding that into a Kotlin `String`.
 * Those three live at once, and a Kotlin string is UTF-16, so an 80 MB feed peaks somewhere near
 * 320 MB. The foreground app might survive it on a recent device; a `BGTask`, which is granted far
 * less, will be jetsam-killed. That is the whole reason the iOS EPG sync was unreliable.
 *
 * This class holds only the record being assembled plus the filtered result, so peak memory tracks
 * the *retained* EPG rather than the size of the feed.
 *
 * ## Parity with [EpgParser]
 *
 * Deliberately mirrors it rather than improving on it, because both must produce the same database:
 *
 *  - Channels need an `id`; without one the element is dropped.
 *  - First `display-name` and first `icon` win, matching `Regex.find` taking the first match.
 *  - Programmes need `start`, `stop` and `channel`; any missing attribute drops the element.
 *  - **Rejection order is preserved and load-bearing**: allowlist first, then date parsing, then
 *    the window. `EpgParser` documents this as what keeps peak memory bounded, and it matters even
 *    more here — a rejected programme never accumulates its title or description at all.
 *  - Event ids are `epg_<n>` with the counter advancing only for *kept* events, exactly as
 *    `idCounter++` at construction time does.
 *
 * One intentional difference: a SAX parser reports self-closing `<channel id="x"/>` and
 * `<programme .../>` elements, whereas the regexes require a closing tag and silently skip them.
 * Accepting them is strictly more correct and cannot lose data.
 *
 * Text arrives **already entity-decoded** — SAX parsers resolve `&amp;` and friends themselves —
 * so [EpgParser.decodeXmlEntities] must not be applied on top, or `&amp;amp;` in a title would
 * decode twice.
 *
 * Pure Kotlin and free of platform types, so the iOS delegate stays a thin adapter and all of this
 * logic is covered by ordinary common tests.
 */
class XmltvSaxAssembler(
    channelFilter: Set<String>? = null,
    private val minEndTimeMs: Long = 0L,
    private val maxStartTimeMs: Long = 0L,
) {
    private val normalizedFilter = channelFilter?.mapTo(mutableSetOf()) { it.trim().lowercase() }

    private val channels = mutableListOf<EPGChannel>()
    private val programmes = mutableMapOf<String, MutableList<EPGEvent>>()
    private var idCounter = 0

    private var inChannel = false
    private var channelId: String? = null
    private var channelName: String? = null
    private var channelIcon: String = ""

    private var inProgramme = false
    private var programmeChannel: String? = null
    private var programmeStart: Long = 0L
    private var programmeStop: Long = 0L
    /** False once the element has been rejected; suppresses all further accumulation for it. */
    private var programmeKept = false
    private var programmeTitle: String? = null
    private var programmeDesc: String? = null
    private var programmeIcon: String = ""

    /** Non-null only while inside a leaf whose text we actually want. */
    private var textBuffer: StringBuilder? = null

    fun startElement(name: String, attributes: Map<String, String>) {
        when (name) {
            ELEMENT_CHANNEL -> {
                inChannel = true
                channelId = attributes[ATTR_ID]
                channelName = null
                channelIcon = ""
            }

            ELEMENT_PROGRAMME -> {
                inProgramme = true
                programmeTitle = null
                programmeDesc = null
                programmeIcon = ""
                programmeChannel = attributes[ATTR_CHANNEL]
                programmeKept = evaluateProgramme(
                    channel = attributes[ATTR_CHANNEL],
                    start = attributes[ATTR_START],
                    stop = attributes[ATTR_STOP],
                )
            }

            ELEMENT_DISPLAY_NAME ->
                // Only buffer the first one; later duplicates are ignored, as `find` does.
                if (inChannel && channelName == null) textBuffer = StringBuilder()

            ELEMENT_TITLE ->
                if (inProgramme && programmeKept && programmeTitle == null) textBuffer = StringBuilder()

            ELEMENT_DESC ->
                if (inProgramme && programmeKept && programmeDesc == null) textBuffer = StringBuilder()

            ELEMENT_ICON -> {
                val src = attributes[ATTR_SRC] ?: return
                when {
                    inChannel && channelIcon.isEmpty() -> channelIcon = src
                    inProgramme && programmeKept && programmeIcon.isEmpty() -> programmeIcon = src
                }
            }
        }
    }

    fun characters(text: String) {
        textBuffer?.append(text)
    }

    fun endElement(name: String) {
        when (name) {
            ELEMENT_DISPLAY_NAME -> {
                textBuffer?.let { channelName = it.toString() }
                textBuffer = null
            }

            ELEMENT_TITLE -> {
                textBuffer?.let { programmeTitle = it.toString() }
                textBuffer = null
            }

            ELEMENT_DESC -> {
                textBuffer?.let { programmeDesc = it.toString() }
                textBuffer = null
            }

            ELEMENT_CHANNEL -> {
                val id = channelId
                if (id != null) {
                    channels += EPGChannel(
                        channelID = id,
                        name = channelName ?: "",
                        imageURL = channelIcon,
                    )
                }
                inChannel = false
                channelId = null
                channelName = null
                channelIcon = ""
                textBuffer = null
            }

            ELEMENT_PROGRAMME -> {
                val channel = programmeChannel
                if (programmeKept && channel != null) {
                    val event = EPGEvent(
                        id = "epg_${idCounter++}",
                        start = programmeStart,
                        end = programmeStop,
                        title = programmeTitle ?: "",
                        description = programmeDesc ?: "",
                        imageURL = programmeIcon,
                    )
                    programmes.getOrPut(channel) { mutableListOf() }.add(event)
                }
                inProgramme = false
                programmeKept = false
                programmeChannel = null
                programmeTitle = null
                programmeDesc = null
                programmeIcon = ""
                textBuffer = null
            }
        }
    }

    fun build(): EpgDomainData = EpgDomainData(channels = channels, programmes = programmes)

    /**
     * Applies the same rejections as `EpgParser.parseProgrammes`, in the same order: allowlist
     * before date parsing, date parsing before the window. Returning false here is what stops the
     * element's title and description ever being buffered.
     */
    private fun evaluateProgramme(channel: String?, start: String?, stop: String?): Boolean {
        if (channel == null || start == null || stop == null) return false

        if (normalizedFilter != null && channel.trim().lowercase() !in normalizedFilter) return false

        val startMillis = EpgParser.parseXmltvDate(start) ?: return false
        val stopMillis = EpgParser.parseXmltvDate(stop) ?: return false

        if (minEndTimeMs != 0L && stopMillis < minEndTimeMs) return false
        if (maxStartTimeMs != 0L && startMillis > maxStartTimeMs) return false

        programmeStart = startMillis
        programmeStop = stopMillis
        return true
    }

    private companion object {
        const val ELEMENT_CHANNEL = "channel"
        const val ELEMENT_PROGRAMME = "programme"
        const val ELEMENT_DISPLAY_NAME = "display-name"
        const val ELEMENT_TITLE = "title"
        const val ELEMENT_DESC = "desc"
        const val ELEMENT_ICON = "icon"
        const val ATTR_ID = "id"
        const val ATTR_SRC = "src"
        const val ATTR_CHANNEL = "channel"
        const val ATTR_START = "start"
        const val ATTR_STOP = "stop"
    }
}
