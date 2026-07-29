@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.epg

import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Drives the real `NSXMLParser` and the real delegate over a fixture file on the simulator.
 *
 * `XmltvSaxAssemblerTest` covers the assembly logic on the JVM with hand-written SAX events. What
 * it cannot check is whether **NSXMLParser actually emits the events that assembler expects** —
 * element names as passed, attributes as a `Map<Any?, *>` of strings, text arriving already
 * entity-decoded and split across arbitrary callbacks. That is exactly the seam where a
 * Kotlin/Native ObjC adapter goes wrong, and it only fails at runtime, on Apple hardware.
 *
 * So these run in CI on `iosSimulatorArm64Test`, and assert the streaming path produces the same
 * result as [EpgParser] does for the same document — the regex parser Android still uses. If the
 * two ever disagree, the same account would get different EPG rows on the two platforms.
 */
class EpgStreamingLoaderIosTest {

    private val written = mutableListOf<String>()

    @AfterTest
    fun tearDown() {
        val fm = NSFileManager.defaultManager
        written.forEach { fm.removeItemAtPath(it, null) }
    }

    /** Writes [xml] to a temp file and returns its URL. */
    private fun fixture(xml: String): NSURL {
        val path = NSTemporaryDirectory() +
            "hitv-epg-test-${NSProcessInfo.processInfo.globallyUniqueString}.xml"
        val ok = (xml as NSString).writeToFile(
            path,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null,
        )
        if (!ok) fail("could not write the fixture to $path")
        written += path
        return NSURL.fileURLWithPath(path)
    }

    private fun parse(
        xml: String,
        channelFilter: Set<String>? = null,
        minEndTimeMs: Long = 0L,
        maxStartTimeMs: Long = 0L,
    ): EpgDomainData {
        val assembler = XmltvSaxAssembler(channelFilter, minEndTimeMs, maxStartTimeMs)
        EpgStreamingLoader.parseXmltvFile(fixture(xml), assembler)
        return assembler.build()
    }

    private val start1 = "20230615120000 +0000"
    private val stop1 = "20230615130000 +0000"
    private val start2 = "20230615130000 +0000"
    private val stop2 = "20230615140000 +0000"

    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tv>
          <channel id="ch1"><display-name>Channel One</display-name><icon src="c1.png" /></channel>
          <channel id="ch2"><display-name>Channel Two</display-name></channel>
          <programme channel="ch1" start="$start1" stop="$stop1"><title>First</title><desc>Desc one</desc><icon src="p1.png" /></programme>
          <programme channel="ch2" start="$start2" stop="$stop2"><title>Second</title></programme>
        </tv>
    """.trimIndent()

    @Test
    fun `streaming parse matches the regex parser`() {
        val streamed = parse(xml)
        val regexed = EpgParser.parse(xml)

        assertEquals(regexed.channels, streamed.channels)
        assertEquals(regexed.programmes, streamed.programmes)
    }

    @Test
    fun `the allowlist is applied while streaming`() {
        val streamed = parse(xml, channelFilter = setOf("CH1"))

        assertEquals(setOf("ch1"), streamed.programmes.keys)
        assertEquals(EpgParser.parse(xml, channelFilter = setOf("CH1")).programmes, streamed.programmes)
    }

    @Test
    fun `the time window is applied while streaming`() {
        val minEnd = EpgParser.parseXmltvDate(stop1)!! + 1
        val streamed = parse(xml, minEndTimeMs = minEnd)

        assertEquals(setOf("ch2"), streamed.programmes.keys)
    }

    @Test
    fun `NSXMLParser decodes entities exactly once`() {
        // The regex parser decodes by hand; NSXMLParser decodes for us. Applying both would turn
        // "&amp;amp;" into "&" instead of "&amp;". `&amp;` here must arrive as a single ampersand.
        val entityXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="ch1"><display-name>Sport &amp; News</display-name></channel>
              <programme channel="ch1" start="$start1" stop="$stop1">
                <title>Tom &amp; Jerry</title><desc>5 &lt; 10 &amp;amp; more</desc>
              </programme>
            </tv>
        """.trimIndent()

        val result = parse(entityXml)

        assertEquals("Sport & News", result.channels.single().name)
        val event = result.programmes.getValue("ch1").single()
        assertEquals("Tom & Jerry", event.title)
        assertEquals("5 < 10 &amp; more", event.description)
    }

    @Test
    fun `text split across callbacks is reassembled`() {
        // A CDATA section plus adjacent text makes NSXMLParser deliver several foundCharacters
        // calls for one element — the case the assembler's StringBuilder exists for.
        val splitXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="ch1"><display-name>Part one <![CDATA[and part two]]></display-name></channel>
            </tv>
        """.trimIndent()

        assertEquals("Part one and part two", parse(splitXml).channels.single().name)
    }

    @Test
    fun `a self-closing channel is captured`() {
        val selfClosing = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv><channel id="ch1"/></tv>
        """.trimIndent()

        val ch = parse(selfClosing).channels.single()
        assertEquals("ch1", ch.channelID)
        assertEquals("", ch.name)
    }

    @Test
    fun `a malformed tail keeps whatever parsed before it`() {
        // Android skips a bad element and carries on, returning what it read. iOS must match:
        // real XMLTV feeds are frequently truncated or malformed, and throwing the whole guide
        // away over a bad tail would make EPG fail on iOS where it works on Android.
        val truncated = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <channel id="ch1"><display-name>Good One</display-name></channel>
              <channel id="ch2"><display-name>Unclosed
        """.trimIndent()

        val assembler = XmltvSaxAssembler()
        // Reports failure...
        val ok = EpgStreamingLoader.parseXmltvFile(fixture(truncated), assembler)
        assertTrue(!ok, "a truncated feed should report an unclean parse")

        // ...but the channel that did parse is still there for the caller to keep.
        val salvaged = assembler.build()
        assertEquals(listOf("ch1"), salvaged.channels.map { it.channelID })
    }

    @Test
    fun `a well-formed feed reports a clean parse`() {
        val assembler = XmltvSaxAssembler()
        assertTrue(EpgStreamingLoader.parseXmltvFile(fixture(xml), assembler))
    }

    @Test
    fun `control characters are stripped so the feed still parses`() {
        // NSXMLParser is strict: a single raw control byte (outside tab/LF/CR) aborts the whole
        // document. Real XMLTV feeds contain them, which is why the original ships
        // XmlSanitizingInputStream and the Android actual pipes every response through it. Without
        // the equivalent here, a feed Android handles fine would produce no EPG at all on iOS.
        val dirty = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            append('\n')
            append("<tv><channel id=\"ch1\"><display-name>Bad")
            append('\u0000')
            append("Chars")
            append('\u0007')
            append("Here</display-name></channel></tv>")
        }

        // Unsanitized, the parser gives up.
        val rawAssembler = XmltvSaxAssembler()
        val rawOk = EpgStreamingLoader.parseXmltvFile(fixture(dirty), rawAssembler)
        assertTrue(!rawOk, "expected NSXMLParser to reject the raw control bytes")

        // Sanitized, it reads cleanly and the illegal bytes are simply gone.
        val cleaned = EpgStreamingLoader.sanitizeXmlFile(fixture(dirty))
        written += cleaned.path.orEmpty()
        val assembler = XmltvSaxAssembler()
        assertTrue(
            EpgStreamingLoader.parseXmltvFile(cleaned, assembler),
            "the sanitized feed should parse cleanly",
        )
        assertEquals("BadCharsHere", assembler.build().channels.single().name)
    }

    @Test
    fun `a large feed parses without materialising it`() {
        // Not a memory assertion — the simulator would not make one meaningful — but it does prove
        // the parser walks a file far bigger than any fixture above and produces every record.
        val programmes = buildString {
            repeat(2_000) { i ->
                append("""<programme channel="ch1" start="$start1" stop="$stop1"><title>P$i</title></programme>""")
            }
        }
        val big = """<?xml version="1.0" encoding="UTF-8"?><tv><channel id="ch1"><display-name>One</display-name></channel>$programmes</tv>"""

        val result = parse(big)

        assertEquals(2_000, result.programmes.getValue("ch1").size)
        assertEquals("P0", result.programmes.getValue("ch1").first().title)
        assertEquals("P1999", result.programmes.getValue("ch1").last().title)
    }
}
