package pt.hitv.epg

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the channel-allowlist and time-window filters added to [EpgParser] to bound the
 * retained programme graph (the iOS jetsam fix). These are the behaviours the original's
 * `XmltvParser` gets via `channelFilter` / `minEndTime`.
 */
class EpgParserFilterTest {

    /**
     * Two channels, one programme each, plus a third programme far in the past.
     * `mine.tv` is the "subscribed" channel; `theirs.tv` is feed noise.
     */
    private val xml = """
        <tv>
          <channel id="mine.tv">
            <display-name>Mine</display-name>
            <icon src="http://x/mine.png"/>
          </channel>
          <channel id="theirs.tv">
            <display-name>Theirs</display-name>
          </channel>
          <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="mine.tv">
            <title>Mine Now</title>
            <desc>Current programme</desc>
          </programme>
          <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="theirs.tv">
            <title>Theirs Now</title>
          </programme>
          <programme start="20200101000000 +0000" stop="20200101010000 +0000" channel="mine.tv">
            <title>Mine Ancient</title>
          </programme>
          <programme start="20990101000000 +0000" stop="20990101010000 +0000" channel="mine.tv">
            <title>Mine Far Future</title>
          </programme>
        </tv>
    """.trimIndent()

    /**
     * 2026-07-26 12:30:00 UTC — inside the "Now" programmes (12:00–13:00).
     * Derived rather than hardcoded: an off-by-days literal here silently turns the window
     * assertions into tautologies.
     */
    private val noonIsh = Instant.parse("2026-07-26T12:30:00Z").toEpochMilliseconds()

    @Test
    fun `parses XMLTV start and stop into the expected UTC instants`() {
        // Anchors the window tests: if date parsing drifts, they'd pass or fail for the
        // wrong reason.
        val now = EpgParser.parse(xml).programmes.getValue("mine.tv")
            .first { it.title == "Mine Now" }
        assertEquals(Instant.parse("2026-07-26T12:00:00Z").toEpochMilliseconds(), now.start)
        assertEquals(Instant.parse("2026-07-26T13:00:00Z").toEpochMilliseconds(), now.end)
    }

    @Test
    fun `no filters retains every programme`() {
        val result = EpgParser.parse(xml)
        assertEquals(2, result.channels.size)
        val titles = result.programmes.values.flatten().map { it.title }
        assertEquals(4, titles.size, "expected all four programmes, got $titles")
        assertTrue("Theirs Now" in titles)
    }

    @Test
    fun `channel allowlist drops programmes for unsubscribed channels`() {
        val result = EpgParser.parse(xml, channelFilter = setOf("mine.tv"))
        val titles = result.programmes.values.flatten().map { it.title }
        assertTrue("Theirs Now" !in titles, "foreign channel leaked through: $titles")
        assertTrue("Mine Now" in titles)
        assertEquals(3, titles.size, "only mine.tv's three programmes should survive")
    }

    @Test
    fun `channel allowlist is case and whitespace insensitive`() {
        // Real feeds disagree on casing; the original compares trim().lowercase().
        val result = EpgParser.parse(xml, channelFilter = setOf("  MINE.TV  "))
        val titles = result.programmes.values.flatten().map { it.title }
        assertTrue("Mine Now" in titles, "normalized allowlist should still match: $titles")
    }

    @Test
    fun `minEndTime drops programmes that already ended`() {
        val result = EpgParser.parse(xml, minEndTimeMs = noonIsh)
        val titles = result.programmes.values.flatten().map { it.title }
        assertTrue("Mine Ancient" !in titles, "expired programme retained: $titles")
        assertTrue("Mine Now" in titles, "in-flight programme must survive its own end check")
    }

    @Test
    fun `maxStartTime drops programmes beyond the window`() {
        val result = EpgParser.parse(xml, maxStartTimeMs = noonIsh)
        val titles = result.programmes.values.flatten().map { it.title }
        assertTrue("Mine Far Future" !in titles, "out-of-window programme retained: $titles")
        assertTrue("Mine Now" in titles)
    }

    @Test
    fun `allowlist and window compose to just the current programme`() {
        val result = EpgParser.parse(
            xmlContent = xml,
            channelFilter = setOf("mine.tv"),
            minEndTimeMs = noonIsh,
            maxStartTimeMs = noonIsh,
        )
        val titles = result.programmes.values.flatten().map { it.title }
        assertEquals(listOf("Mine Now"), titles)
    }

    @Test
    fun `channels are never filtered - only programmes are`() {
        // The channel allowlist is derived FROM the channel table, so dropping <channel>
        // elements would be self-defeating. Both must survive regardless.
        val result = EpgParser.parse(xml, channelFilter = setOf("mine.tv"))
        assertEquals(2, result.channels.size)
    }

    @Test
    fun `filtered programmes do not consume id counter slots for kept ones`() {
        // Ids must stay unique among retained programmes; this guards the early-return
        // refactor that moved the channel check above id assignment.
        val result = EpgParser.parse(xml, channelFilter = setOf("mine.tv"))
        val ids = result.programmes.values.flatten().map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate programme ids: $ids")
    }

    @Test
    fun `body regexes still populate title desc and icon after the early-exit refactor`() {
        val result = EpgParser.parse(xml, channelFilter = setOf("mine.tv"))
        val now = result.programmes.getValue("mine.tv").first { it.title == "Mine Now" }
        assertEquals("Current programme", now.description)
    }
}
