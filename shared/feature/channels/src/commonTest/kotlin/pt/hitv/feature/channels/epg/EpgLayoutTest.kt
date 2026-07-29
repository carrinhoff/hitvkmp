package pt.hitv.feature.channels.epg

import pt.hitv.epg.domain.EPGEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the grid's horizontal layout arithmetic.
 *
 * This is the part of the EPG port most likely to be subtly wrong and least likely to be caught by
 * eye: every programme block's x-offset and width come from here, and a scaling or origin error
 * shifts the whole grid relative to the timeline labels — which still *looks* like a plausible
 * guide, just showing the wrong times.
 *
 * `pxPerMinute` is fixed at 4f throughout (the real value is `TimeIntervalWidth / 30min` in
 * density pixels), so 30 minutes = 120px and the numbers stay readable.
 */
class EpgLayoutTest {

    private val start = 1_800_000_000_000L
    private val pxPerMinute = 4f
    private val halfHourMs = 30 * 60_000L

    private fun event(startOffsetMin: Long, durationMin: Long, title: String = "p") = EPGEvent(
        id = title,
        start = start + startOffsetMin * 60_000L,
        end = start + (startOffsetMin + durationMin) * 60_000L,
        title = title,
        description = "",
        imageURL = "",
    )

    @Test
    fun `a programme starting at the grid origin sits at x zero`() {
        val layouts = computeEventLayouts(listOf(event(0, 30)), start, pxPerMinute)
        assertEquals(1, layouts.size)
        assertEquals(0, layouts[0].xOffsetPx)
        assertEquals(120, layouts[0].widthPx, "30 minutes at 4px/min should be 120px")
    }

    @Test
    fun `offset scales linearly with start time`() {
        val layouts = computeEventLayouts(
            listOf(event(0, 30, "a"), event(30, 30, "b"), event(60, 30, "c")),
            start,
            pxPerMinute,
        )
        assertEquals(listOf(0, 120, 240), layouts.map { it.xOffsetPx })
    }

    @Test
    fun `blocks are contiguous - each starts exactly where the previous ended`() {
        // A gap or overlap here shows up as visible seams or overlapping text in the grid.
        val layouts = computeEventLayouts(
            (0 until 6).map { event(it * 30L, 30, "p$it") },
            start,
            pxPerMinute,
        )
        for (i in 1 until layouts.size) {
            val prevEnd = layouts[i - 1].xOffsetPx + layouts[i - 1].widthPx
            assertEquals(prevEnd, layouts[i].xOffsetPx, "seam at index $i")
        }
    }

    @Test
    fun `a programme already in progress is clipped to the left edge`() {
        // Started 20 minutes before the grid origin, runs 60 minutes total.
        val inProgress = EPGEvent(
            id = "live",
            start = start - 20 * 60_000L,
            end = start + 40 * 60_000L,
            title = "live",
            description = "",
            imageURL = "",
        )
        val layouts = computeEventLayouts(listOf(inProgress), start, pxPerMinute)
        assertEquals(1, layouts.size)
        assertEquals(0, layouts[0].xOffsetPx, "must render from the left edge, not off-screen")
        assertEquals(160, layouts[0].widthPx, "only the remaining 40 minutes is drawn")
    }

    @Test
    fun `programmes that already ended are dropped`() {
        val past = EPGEvent(
            id = "past",
            start = start - 2 * halfHourMs,
            end = start - halfHourMs,
            title = "past",
            description = "",
            imageURL = "",
        )
        assertTrue(computeEventLayouts(listOf(past), start, pxPerMinute).isEmpty())
    }

    @Test
    fun `zero and negative duration programmes are dropped`() {
        val zero = event(30, 0, "zero")
        val inverted = EPGEvent(
            id = "inverted",
            start = start + 2 * halfHourMs,
            end = start + halfHourMs,
            title = "inverted",
            description = "",
            imageURL = "",
        )
        assertTrue(computeEventLayouts(listOf(zero, inverted), start, pxPerMinute).isEmpty())
    }

    @Test
    fun `a programme ending exactly at the origin is kept but has zero usable width`() {
        // end == startTime is not < startTime, so it survives the first filter, but its clipped
        // duration is zero and it is dropped by the duration check. Pinning the boundary.
        val edge = EPGEvent(
            id = "edge",
            start = start - halfHourMs,
            end = start,
            title = "edge",
            description = "",
            imageURL = "",
        )
        assertTrue(computeEventLayouts(listOf(edge), start, pxPerMinute).isEmpty())
    }

    @Test
    fun `a full 24 hours of half-hour blocks spans the expected total width`() {
        // The grid sizes its scrollable area as 24 * 60 * pxPerMinute; the blocks must fill it.
        val layouts = computeEventLayouts(
            (0 until 48).map { event(it * 30L, 30, "p$it") },
            start,
            pxPerMinute,
        )
        assertEquals(48, layouts.size)
        val totalWidth = layouts.last().xOffsetPx + layouts.last().widthPx
        assertEquals((24 * 60 * pxPerMinute).toInt(), totalWidth)
    }

    @Test
    fun `input order is preserved so the canvas early-exit scan stays valid`() {
        // ProgramsCanvas breaks out of its draw loop at the first block past the right edge, which
        // is only correct if layouts are ordered by x. filterEpgData sorts; this asserts the
        // layout step does not reorder.
        val layouts = computeEventLayouts(
            listOf(event(0, 30, "a"), event(30, 30, "b"), event(60, 30, "c")),
            start,
            pxPerMinute,
        )
        assertEquals(listOf("a", "b", "c"), layouts.map { it.epgEvent.title })
    }
}
