package pt.hitv.epg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Text-handling fidelity of the regex XMLTV parser.
 *
 * These defects are **iOS-only in effect**: Android streams the feed through `XmlPullParser`,
 * which decodes entities and handles multi-line elements itself. Only iOS uses this regex path, so
 * a Portuguese guide rendered correctly on Android and came through mangled on iPhone.
 */
class EpgTextDecodingTest {

    private fun programme(title: String, desc: String = "") = """
        <tv>
          <channel id="c1"><display-name>Canal Um</display-name></channel>
          <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="c1">
            <title>$title</title>
            <desc>$desc</desc>
          </programme>
        </tv>
    """.trimIndent()

    private fun firstEvent(xml: String) =
        EpgParser.parse(xml).programmes.getValue("c1").first()

    @Test
    fun `decodes decimal character references`() {
        // The reason this matters here: a PT guide is full of these.
        val event = firstEvent(programme("Not&#237;cias da Manh&#227;"))
        assertEquals("Notícias da Manhã", event.title)
    }

    @Test
    fun `decodes hexadecimal character references`() {
        val event = firstEvent(programme("Jos&#x00E9; &#x2014; Document&#xE1;rio"))
        assertEquals("José — Documentário", event.title)
    }

    @Test
    fun `decodes the five predefined entities`() {
        val event = firstEvent(programme("Tom &amp; Jerry &lt;live&gt; &quot;HD&quot; &apos;24&apos;"))
        assertEquals("Tom & Jerry <live> \"HD\" '24'", event.title)
    }

    @Test
    fun `does not double-decode escaped entities`() {
        // "&amp;lt;" means the literal text "&lt;", NOT the character "<". Decoding &amp; first
        // produced "&lt;" and then decoded that again into "<", turning escaped text into markup.
        val event = firstEvent(programme("&amp;lt;not a tag&amp;gt;"))
        assertEquals("&lt;not a tag&gt;", event.title)
    }

    @Test
    fun `reads a description that spans multiple lines`() {
        // Kotlin's `.` does not match newlines, so the old regex missed wrapped <desc> entirely
        // and the programme kept an empty description.
        val xml = """
            <tv>
              <channel id="c1"><display-name>Canal Um</display-name></channel>
              <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="c1">
                <title>Filme</title>
                <desc>Primeira linha da sinopse.
            Segunda linha, que continua aqui.
            Terceira linha.</desc>
              </programme>
            </tv>
        """.trimIndent()
        val event = firstEvent(xml)
        assertTrue(event.description.contains("Primeira linha"), "start missing: ${event.description}")
        assertTrue(event.description.contains("Terceira linha"), "end missing: ${event.description}")
    }

    @Test
    fun `reads a display-name that spans multiple lines`() {
        val xml = """
            <tv>
              <channel id="c1"><display-name>Canal
            Um HD</display-name></channel>
              <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="c1">
                <title>X</title>
              </programme>
            </tv>
        """.trimIndent()
        val channel = EpgParser.parse(xml).channels.first { it.channelID == "c1" }
        assertTrue(channel.name.contains("Canal"), "start missing: ${channel.name}")
        assertTrue(channel.name.contains("Um HD"), "end missing: ${channel.name}")
    }

    @Test
    fun `leaves malformed numeric references untouched`() {
        // Better to show the raw text than to emit a replacement character or throw.
        val event = firstEvent(programme("Bad &#; and &#xZZ; refs"))
        assertEquals("Bad &#; and &#xZZ; refs", event.title)
    }

    @Test
    fun `handles a reference above the basic multilingual plane`() {
        // Surrogate pairing — an emoji in a title should not corrupt the string.
        val event = firstEvent(programme("Futebol &#x1F1F5;&#x1F1F9;"))
        assertEquals("Futebol 🇵🇹", event.title)
    }

    @Test
    fun `plain text is unaffected`() {
        val event = firstEvent(programme("Telejornal", "Sem entidades aqui"))
        assertEquals("Telejornal", event.title)
        assertEquals("Sem entidades aqui", event.description)
    }
}
