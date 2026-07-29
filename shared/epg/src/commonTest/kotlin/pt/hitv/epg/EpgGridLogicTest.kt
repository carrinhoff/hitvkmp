package pt.hitv.epg

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.epg.data.filterEpgData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the non-visual logic behind the ported EPG grid.
 *
 * The grid lays every programme block out relative to [EpgUtils.roundedHalfHourStart], so if that
 * lands even a second off a :00/:30 boundary, every block and every timeline label is skewed by
 * that amount — a bug that is very hard to spot by eye. The original got this from
 * `Calendar.set(MINUTE, ...)`; this port reimplements it on kotlinx-datetime, so it is worth
 * pinning precisely.
 */
class EpgGridLogicTest {

    private fun minuteAndSecondOf(millis: Long): Triple<Int, Int, Int> {
        val dt = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault())
        return Triple(dt.minute, dt.second, dt.nanosecond / 1_000_000)
    }

    @Test
    fun `rounds down to the same half hour when past the half hour mark`() {
        val now = Instant.parse("2026-07-26T14:47:33.412Z").toEpochMilliseconds()
        val start = EpgUtils.roundedHalfHourStart(now)
        val (minute, second, milli) = minuteAndSecondOf(start)
        assertEquals(30, minute)
        assertEquals(0, second)
        assertEquals(0, milli)
        assertTrue(start <= now, "rounding must go backwards, never forwards")
    }

    @Test
    fun `rounds down to the hour when before the half hour mark`() {
        val now = Instant.parse("2026-07-26T14:12:59.999Z").toEpochMilliseconds()
        val start = EpgUtils.roundedHalfHourStart(now)
        val (minute, second, milli) = minuteAndSecondOf(start)
        assertEquals(0, minute)
        assertEquals(0, second)
        assertEquals(0, milli)
        assertTrue(start <= now)
    }

    @Test
    fun `is idempotent on an exact boundary`() {
        val onBoundary = Instant.parse("2026-07-26T14:30:00Z").toEpochMilliseconds()
        assertEquals(onBoundary, EpgUtils.roundedHalfHourStart(onBoundary))
    }

    @Test
    fun `never rounds back more than 30 minutes`() {
        // Sweep a full hour a minute at a time; the offset must always stay in [0, 30min).
        val base = Instant.parse("2026-07-26T09:00:00Z").toEpochMilliseconds()
        for (m in 0 until 60) {
            val now = base + m * 60_000L + 17_000L // add a non-zero second component
            val delta = now - EpgUtils.roundedHalfHourStart(now)
            assertTrue(
                delta in 0 until 30 * 60_000L,
                "offset $delta out of range at minute $m",
            )
        }
    }

    @Test
    fun `isSameDay is zone-independent for clearly separated instants`() {
        // Deliberately NOT using instants an hour either side of UTC midnight: whether those fall
        // on the same *local* day depends on the machine's zone, so such a test would pass in UTC
        // and fail in UTC+1. Anything 48h apart is a different local day in every zone.
        val noon = Instant.parse("2026-07-26T12:00:00Z").toEpochMilliseconds()
        val twoDaysLater = noon + 2 * 24 * 60 * 60 * 1000L

        assertTrue(EpgUtils.isSameDay(noon, noon))
        assertTrue(EpgUtils.isSameDay(noon, noon + 60_000L), "a minute later is the same day")
        assertFalse(EpgUtils.isSameDay(noon, twoDaysLater))
    }

    @Test
    fun `localNoonDaysAgo steps back a whole day at a time`() {
        val now = Instant.parse("2026-07-26T12:00:00Z").toEpochMilliseconds()
        val today = EpgUtils.localNoonDaysAgo(now, 0)
        val yesterday = EpgUtils.localNoonDaysAgo(now, 1)

        assertTrue(EpgUtils.isSameDay(today, now), "day 0 must be today")
        assertFalse(EpgUtils.isSameDay(today, yesterday))
        assertEquals(24 * 60 * 60 * 1000L, today - yesterday)
    }

    @Test
    fun `startOfLocalDay is stable and not in the future`() {
        val now = Instant.parse("2026-07-26T14:47:33Z").toEpochMilliseconds()
        val midnight = EpgUtils.startOfLocalDay(now)
        assertTrue(midnight <= now)
        assertEquals(midnight, EpgUtils.startOfLocalDay(midnight), "should be idempotent")
    }

    // ===== filterEpgData =====

    private fun info(
        channelId: String,
        start: Long,
        title: String,
        hasCatchUp: Boolean = false,
        name: String = "Channel $channelId",
    ) = ChannelEpgInfo(
        channelId = channelId,
        channelName = name,
        programmeTitle = title,
        programmeDescription = "desc $title",
        startTime = start,
        endTime = start + 1_800_000L,
        logo = "http://logo/$channelId",
        hasCatchUp = hasCatchUp,
    )

    @Test
    fun `groups programmes by channel`() {
        val data = filterEpgData(
            listOf(
                info("a", 1_000L, "A1"),
                info("b", 1_000L, "B1"),
                info("a", 2_000L, "A2"),
            )
        )
        assertEquals(2, data.channelCount)
        assertTrue(data.hasData())

        val channelA = (0 until data.channelCount).map { data.getChannel(it)!! }
            .first { it.channelID == "a" }
        val indexA = (0 until data.channelCount).first { data.getChannel(it) == channelA }
        assertEquals(2, data.getEvents(indexA)!!.size)
    }

    @Test
    fun `sorts events by start time regardless of input order`() {
        // The grid's canvas breaks out of its draw loop at the first block past the right edge,
        // so out-of-order events would silently truncate a row.
        val data = filterEpgData(
            listOf(
                info("a", 9_000L, "late"),
                info("a", 1_000L, "early"),
                info("a", 5_000L, "middle"),
            )
        )
        val titles = data.getEvents(0)!!.filterNotNull().map { it.title }
        assertEquals(listOf("early", "middle", "late"), titles)
    }

    @Test
    fun `carries hasCatchUp onto the channel`() {
        val data = filterEpgData(listOf(info("a", 1_000L, "A1", hasCatchUp = true)))
        assertTrue(data.getChannel(0)!!.hasCatchUp)

        val plain = filterEpgData(listOf(info("b", 1_000L, "B1")))
        assertFalse(plain.getChannel(0)!!.hasCatchUp)
    }

    @Test
    fun `builds stable unique event ids`() {
        val data = filterEpgData(listOf(info("a", 1_000L, "A1"), info("a", 2_000L, "A2")))
        val ids = data.getEvents(0)!!.filterNotNull().map { it.id }
        assertEquals(listOf("a_1000", "a_2000"), ids)
    }

    @Test
    fun `empty input yields no data`() {
        val data = filterEpgData(emptyList())
        assertEquals(0, data.channelCount)
        assertFalse(data.hasData())
        assertNull(data.getChannel(0))
        assertNull(data.getEvents(0))
    }

    @Test
    fun `null fields degrade to empty strings rather than throwing`() {
        val data = filterEpgData(
            listOf(
                ChannelEpgInfo(
                    channelId = "a",
                    channelName = null,
                    programmeTitle = null,
                    programmeDescription = null,
                    startTime = null,
                    endTime = null,
                    logo = null,
                )
            )
        )
        val event = data.getEvent(0, 0)!!
        assertEquals("", event.title)
        assertEquals("", event.description)
        assertEquals(0L, event.start)
    }
}
