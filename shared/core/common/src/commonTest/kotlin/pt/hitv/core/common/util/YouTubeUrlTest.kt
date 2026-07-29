package pt.hitv.core.common.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Xtream `youtube_trailer` fields are wildly inconsistent between providers, and the port used the
 * raw value for two things it was not fit for:
 *
 *  - built `https://www.youtube.com/watch?v=$raw`, so a field already holding a URL produced
 *    `watch?v=https://youtu.be/abc`;
 *  - gated the Trailer button on `!isNullOrBlank()`, so any junk rendered a button to nowhere.
 *
 * Both now go through [YouTubeUrl], ported from the original.
 */
class YouTubeUrlTest {

    private val id = "dQw4w9WgXcQ" // 11 chars, the only shape the player accepts

    @Test
    fun `a bare id passes through`() {
        assertEquals(id, YouTubeUrl.extractVideoId(id))
    }

    @Test
    fun `extracts from every URL shape providers send`() {
        val urls = listOf(
            "https://www.youtube.com/watch?v=$id",
            "http://youtube.com/watch?v=$id&feature=share",
            "https://youtu.be/$id",
            "https://www.youtube.com/embed/$id",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/v/$id",
        )
        urls.forEach { url ->
            assertEquals(id, YouTubeUrl.extractVideoId(url), "failed for $url")
        }
    }

    @Test
    fun `handles v as a non-first query parameter`() {
        assertEquals(id, YouTubeUrl.extractVideoId("https://youtube.com/watch?list=PL123&v=$id"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(id, YouTubeUrl.extractVideoId("  $id  "))
    }

    @Test
    fun `null blank and junk yield null so the button can be hidden`() {
        listOf(null, "", "   ", "n/a", "0", "false", "no").forEach { raw ->
            assertNull(YouTubeUrl.extractVideoId(raw), "expected null for '$raw'")
        }
    }

    @Test
    fun `an arbitrary 11-char token is accepted - and that is correct`() {
        // "not-a-video" is 11 characters of [A-Za-z0-9_-], i.e. structurally indistinguishable
        // from a real video id. Neither this nor the original can tell them apart without asking
        // YouTube, so it is accepted. Documented because it looks like a gap and is not one — my
        // first version of this test asserted null here and was wrong.
        assertEquals("not-a-video", YouTubeUrl.extractVideoId("not-a-video"))
    }

    @Test
    fun `ids of the wrong length are rejected`() {
        assertNull(YouTubeUrl.extractVideoId("tooshort"))
        assertNull(YouTubeUrl.extractVideoId("waaaaaaaaaaaaytoolong123"))
    }

    @Test
    fun `watchUrlOrNull builds a canonical URL rather than concatenating the raw value`() {
        assertEquals("https://www.youtube.com/watch?v=$id", YouTubeUrl.watchUrlOrNull(id))
        // The regression: a URL in, a correct URL out — not watch?v=https://...
        assertEquals(
            "https://www.youtube.com/watch?v=$id",
            YouTubeUrl.watchUrlOrNull("https://youtu.be/$id"),
        )
    }

    @Test
    fun `watchUrlOrNull returns null for unusable input`() {
        assertNull(YouTubeUrl.watchUrlOrNull(null))
        assertNull(YouTubeUrl.watchUrlOrNull(""))
        assertNull(YouTubeUrl.watchUrlOrNull("garbage"))
    }
}
