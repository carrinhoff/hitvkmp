package pt.hitv.feature.movies.detail

import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins the rule deciding between the full movie-detail screen and the minimal
 * "Limited information available" fallback.
 *
 * The port required `info.name` **and** `info.movieImage`, so a movie with complete metadata but
 * no poster art was dumped to the limited screen — as was any movie whose name the provider put in
 * `movieData` rather than `info`. Both are common.
 *
 * The original is explicit that only a *resolvable name* matters: image, plot and cast are
 * best-effort and the UI degrades gracefully without them.
 *
 * These mirror the local functions in `MovieInfoContent`; keeping them in step is the point, since
 * a stricter predicate silently downgrades detail screens rather than failing anything.
 */
class MovieDetailVisibilityTest {

    private fun resolveMovieName(info: Info?, data: MovieData?): String? =
        info?.name?.takeIf { it.isNotBlank() } ?: data?.name?.takeIf { it.isNotBlank() }

    private fun canShowFullScreen(info: Info?, data: MovieData?): Boolean =
        info != null && data != null && !resolveMovieName(info, data).isNullOrBlank()

    private fun info(name: String? = "A Movie", image: String? = "http://img/poster.jpg") =
        Info(name = name, movieImage = image, durationSecs = null)

    private fun data(name: String = "A Movie") = MovieData(
        streamId = 1,
        name = name,
        added = 0.0,
        categoryId = 1,
        containerExtension = "mp4",
        customSid = null,
        directSource = null,
    )

    @Test
    fun `full metadata shows the full screen`() {
        assertTrue(canShowFullScreen(info(), data()))
    }

    @Test
    fun `a missing poster still shows the full screen - the regression`() {
        // This is the case the port got wrong: usable metadata, no art.
        assertTrue(canShowFullScreen(info(image = null), data()))
        assertTrue(canShowFullScreen(info(image = ""), data()))
    }

    @Test
    fun `a name only in movieData still shows the full screen`() {
        // Providers frequently leave info.name blank and populate movieData.name.
        assertTrue(canShowFullScreen(info(name = null), data(name = "From MovieData")))
        assertTrue(canShowFullScreen(info(name = ""), data(name = "From MovieData")))
    }

    @Test
    fun `no resolvable name falls back to the limited screen`() {
        assertFalse(canShowFullScreen(info(name = null), data(name = "")))
        assertFalse(canShowFullScreen(info(name = "  "), data(name = "  ")))
    }

    @Test
    fun `a missing metadata block falls back to the limited screen`() {
        assertFalse(canShowFullScreen(null, data()))
        assertFalse(canShowFullScreen(info(), null))
        assertFalse(canShowFullScreen(null, null))
    }

    @Test
    fun `resolveMovieName prefers info then falls back to data`() {
        assertEquals("From Info", resolveMovieName(info(name = "From Info"), data(name = "From Data")))
        assertEquals("From Data", resolveMovieName(info(name = ""), data(name = "From Data")))
        assertNull(resolveMovieName(info(name = " "), data(name = " ")))
    }
}
