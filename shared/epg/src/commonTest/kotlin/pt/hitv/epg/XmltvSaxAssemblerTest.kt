package pt.hitv.epg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Parity tests for the streaming assembler against [EpgParser], the regex parser it has to agree
 * with.
 *
 * Both must produce the same database from the same feed — Android streams through `XmlPullParser`
 * and iOS will stream through `NSXMLParser`, but a channel row written on one platform has to match
 * the other. So each case here drives the assembler with the SAX events a parser would emit, and
 * asserts the result equals what `EpgParser` extracts from the equivalent XML text.
 *
 * The events are written out by hand rather than generated: producing them with a parser would mean
 * testing a parser with a parser, and the point is to pin the *contract* the iOS delegate has to
 * satisfy. That delegate is a thin adapter over these three calls, so everything of substance in
 * the streaming path is covered here, on the JVM, without a device.
 */
class XmltvSaxAssemblerTest {

    // ---- helpers -------------------------------------------------------------------------

    private fun channelEvents(a: XmltvSaxAssembler, id: String, name: String, icon: String? = null) {
        a.startElement("channel", mapOf("id" to id))
        a.startElement("display-name", emptyMap())
        a.characters(name)
        a.endElement("display-name")
        if (icon != null) a.startElement("icon", mapOf("src" to icon))
        a.endElement("channel")
    }

    private fun programmeEvents(
        a: XmltvSaxAssembler,
        channel: String,
        start: String,
        stop: String,
        title: String,
        desc: String? = null,
        icon: String? = null,
    ) {
        a.startElement("programme", mapOf("channel" to channel, "start" to start, "stop" to stop))
        a.startElement("title", emptyMap())
        a.characters(title)
        a.endElement("title")
        if (desc != null) {
            a.startElement("desc", emptyMap())
            a.characters(desc)
            a.endElement("desc")
        }
        if (icon != null) a.startElement("icon", mapOf("src" to icon))
        a.endElement("programme")
    }

    private val start1 = "20230615120000 +0000"
    private val stop1 = "20230615130000 +0000"
    private val start2 = "20230615130000 +0000"
    private val stop2 = "20230615140000 +0000"

    private val xml = """
        <tv>
          <channel id="ch1"><display-name>Channel One</display-name><icon src="c1.png" /></channel>
          <channel id="ch2"><display-name>Channel Two</display-name></channel>
          <programme channel="ch1" start="$start1" stop="$stop1"><title>First</title><desc>Desc one</desc><icon src="p1.png" /></programme>
          <programme channel="ch2" start="$start2" stop="$stop2"><title>Second</title></programme>
        </tv>
    """.trimIndent()

    private fun feedFullDocument(a: XmltvSaxAssembler) {
        channelEvents(a, "ch1", "Channel One", "c1.png")
        channelEvents(a, "ch2", "Channel Two")
        programmeEvents(a, "ch1", start1, stop1, "First", "Desc one", "p1.png")
        programmeEvents(a, "ch2", start2, stop2, "Second")
    }

    // ---- parity --------------------------------------------------------------------------

    @Test
    fun `channels match the regex parser`() {
        val a = XmltvSaxAssembler()
        feedFullDocument(a)

        assertEquals(EpgParser.parse(xml).channels, a.build().channels)
    }

    @Test
    fun `programmes match the regex parser`() {
        val a = XmltvSaxAssembler()
        feedFullDocument(a)

        assertEquals(EpgParser.parse(xml).programmes, a.build().programmes)
    }

    @Test
    fun `allowlist filtering matches the regex parser`() {
        val filter = setOf("CH1")  // case-insensitive, as EpgParser normalises
        val a = XmltvSaxAssembler(channelFilter = filter)
        feedFullDocument(a)

        val expected = EpgParser.parse(xml, channelFilter = filter)
        assertEquals(expected.programmes, a.build().programmes)
        assertEquals(setOf("ch1"), a.build().programmes.keys)
    }

    @Test
    fun `window filtering matches the regex parser`() {
        // Drops the first programme (ends before minEnd), keeps the second.
        val minEnd = EpgParser.parseXmltvDate(stop1)!! + 1
        val a = XmltvSaxAssembler(minEndTimeMs = minEnd)
        feedFullDocument(a)

        val expected = EpgParser.parse(xml, minEndTimeMs = minEnd)
        assertEquals(expected.programmes, a.build().programmes)
        assertEquals(setOf("ch2"), a.build().programmes.keys)
    }

    @Test
    fun `maxStartTime filtering matches the regex parser`() {
        val maxStart = EpgParser.parseXmltvDate(start1)!!
        val a = XmltvSaxAssembler(maxStartTimeMs = maxStart)
        feedFullDocument(a)

        assertEquals(EpgParser.parse(xml, maxStartTimeMs = maxStart).programmes, a.build().programmes)
    }

    // ---- id numbering --------------------------------------------------------------------

    @Test
    fun `event ids advance only for kept events`() {
        // EpgParser increments its counter at construction, i.e. after every rejection, so ids are
        // contiguous across what survives. A filtered-out programme must not consume an id.
        val a = XmltvSaxAssembler(channelFilter = setOf("ch2"))
        feedFullDocument(a)

        val kept = a.build().programmes.getValue("ch2")
        assertEquals(listOf("epg_0"), kept.map { it.id })
    }

    @Test
    fun `ids are contiguous across channels`() {
        val a = XmltvSaxAssembler()
        feedFullDocument(a)

        val result = a.build()
        assertEquals(listOf("epg_0"), result.programmes.getValue("ch1").map { it.id })
        assertEquals(listOf("epg_1"), result.programmes.getValue("ch2").map { it.id })
    }

    // ---- element-level rules ---------------------------------------------------------------

    @Test
    fun `a channel without an id is dropped`() {
        val a = XmltvSaxAssembler()
        a.startElement("channel", emptyMap())
        a.startElement("display-name", emptyMap())
        a.characters("Nameless")
        a.endElement("display-name")
        a.endElement("channel")

        assertTrue(a.build().channels.isEmpty())
    }

    @Test
    fun `the first display-name and icon win`() {
        val a = XmltvSaxAssembler()
        a.startElement("channel", mapOf("id" to "ch1"))
        a.startElement("display-name", emptyMap()); a.characters("First"); a.endElement("display-name")
        a.startElement("display-name", emptyMap()); a.characters("Second"); a.endElement("display-name")
        a.startElement("icon", mapOf("src" to "one.png"))
        a.startElement("icon", mapOf("src" to "two.png"))
        a.endElement("channel")

        val ch = a.build().channels.single()
        assertEquals("First", ch.name)
        assertEquals("one.png", ch.imageURL)
    }

    @Test
    fun `a programme missing any required attribute is dropped`() {
        val a = XmltvSaxAssembler()
        // no stop
        a.startElement("programme", mapOf("channel" to "ch1", "start" to start1))
        a.startElement("title", emptyMap()); a.characters("Nope"); a.endElement("title")
        a.endElement("programme")
        // unparseable start
        a.startElement("programme", mapOf("channel" to "ch1", "start" to "not-a-date", "stop" to stop1))
        a.endElement("programme")

        assertTrue(a.build().programmes.isEmpty())
    }

    @Test
    fun `a rejected programme does not leak text into the next one`() {
        // The rejected element's title is never buffered; the following kept element must still
        // get its own, and must not inherit anything.
        val a = XmltvSaxAssembler(channelFilter = setOf("ch2"))
        programmeEvents(a, "ch1", start1, stop1, "Rejected title", "Rejected desc")
        programmeEvents(a, "ch2", start2, stop2, "Kept title", "Kept desc")

        val kept = a.build().programmes.getValue("ch2").single()
        assertEquals("Kept title", kept.title)
        assertEquals("Kept desc", kept.description)
    }

    @Test
    fun `text split across several character callbacks is joined`() {
        // A real SAX parser splits text at buffer boundaries and around entities; the assembler
        // must accumulate rather than take the last chunk.
        val a = XmltvSaxAssembler()
        a.startElement("channel", mapOf("id" to "ch1"))
        a.startElement("display-name", emptyMap())
        a.characters("Sport")
        a.characters(" & ")
        a.characters("News")
        a.endElement("display-name")
        a.endElement("channel")

        assertEquals("Sport & News", a.build().channels.single().name)
    }

    @Test
    fun `a self-closing channel is accepted`() {
        // SAX reports these; the regex parser skips them. Accepting is strictly more correct.
        val a = XmltvSaxAssembler()
        a.startElement("channel", mapOf("id" to "ch1"))
        a.endElement("channel")

        val ch = a.build().channels.single()
        assertEquals("ch1", ch.channelID)
        assertEquals("", ch.name)
    }

    @Test
    fun `a programme icon does not bleed into the channel icon`() {
        val a = XmltvSaxAssembler()
        channelEvents(a, "ch1", "Channel One")
        programmeEvents(a, "ch1", start1, stop1, "First", icon = "prog.png")

        assertEquals("", a.build().channels.single().imageURL)
        assertEquals("prog.png", a.build().programmes.getValue("ch1").single().imageURL)
    }

    @Test
    fun `missing title and desc become empty strings rather than nulls`() {
        val a = XmltvSaxAssembler()
        a.startElement("programme", mapOf("channel" to "ch1", "start" to start1, "stop" to stop1))
        a.endElement("programme")

        val e = a.build().programmes.getValue("ch1").single()
        assertEquals("", e.title)
        assertEquals("", e.description)
        assertEquals("", e.imageURL)
    }
}
