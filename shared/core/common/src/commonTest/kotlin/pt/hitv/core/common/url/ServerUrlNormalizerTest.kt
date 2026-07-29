package pt.hitv.core.common.url

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Ported from `ServerUrlNormalizerTest` in the original, which is the spec for this utility.
 *
 * The port had no normalizer at all — `AccountManagerRepositoryImpl` carried a partial
 * reimplementation that trimmed the ends of the string and appended a trailing slash. Everything
 * below that concerns a missing scheme, interior whitespace, repeated slashes or a pasted
 * `player_api.php` endpoint was simply absent, and all of it lands on the login screen.
 *
 * Note the test names avoid commas and parentheses: this is `commonTest`, so it compiles for
 * Kotlin/Native too, which rejects both in a backtick identifier.
 */
class ServerUrlNormalizerTest {

    // ---- Xtream base -----------------------------------------------------------------------

    @Test
    fun `xtream base with scheme and port gets single trailing slash`() {
        assertEquals("http://example.com:8080/", ServerUrlNormalizer.normalize("http://example.com:8080"))
    }

    @Test
    fun `typical xtream host with port 80 is preserved with single trailing slash`() {
        assertEquals("http://line.provider.tv:80/", ServerUrlNormalizer.normalize("http://line.provider.tv:80"))
        assertEquals("http://line.provider.tv:80/", ServerUrlNormalizer.normalize("http://line.provider.tv:80/"))
    }

    @Test
    fun `xtream base without scheme gets http prepended`() {
        // The case that made a scheme-less host fail every request.
        assertEquals("http://example.com:8080/", ServerUrlNormalizer.normalize("example.com:8080"))
    }

    @Test
    fun `https scheme is preserved`() {
        assertEquals("https://secure.tv/", ServerUrlNormalizer.normalize("https://secure.tv"))
    }

    @Test
    fun `multiple trailing slashes collapse to one`() {
        assertEquals("http://example.com/", ServerUrlNormalizer.normalize("http://example.com///"))
    }

    @Test
    fun `pasted player_api endpoint is stripped back to base`() {
        assertEquals(
            "http://example.com:8080/",
            ServerUrlNormalizer.normalize("http://example.com:8080/player_api.php?username=u&password=p"),
        )
    }

    @Test
    fun `interior whitespace is stripped - the crash input`() {
        assertEquals("http://hitv.com/", ServerUrlNormalizer.normalize("hi tv.com"))
    }

    @Test
    fun `leading and trailing whitespace is stripped`() {
        assertEquals("http://example.com/", ServerUrlNormalizer.normalize("  http://example.com  "))
    }

    @Test
    fun `blank input returns empty`() {
        assertEquals("", ServerUrlNormalizer.normalize(""))
        assertEquals("", ServerUrlNormalizer.normalize("   "))
    }

    @Test
    fun `no-host degenerate input returns empty`() {
        assertEquals("", ServerUrlNormalizer.normalize("/"))
        assertEquals("", ServerUrlNormalizer.normalize("http://"))
    }

    // ---- M3U shapes reaching the Xtream entry point -----------------------------------------

    @Test
    fun `m3u get_php url keeps query and gets no trailing slash`() {
        val url = "http://example.com/get.php?username=u&password=p&type=m3u_plus"
        assertEquals(url, ServerUrlNormalizer.normalize(url))
    }

    @Test
    fun `m3u8 playlist url is preserved`() {
        assertEquals("http://example.com/list.m3u8", ServerUrlNormalizer.normalize("http://example.com/list.m3u8"))
    }

    @Test
    fun `m3u playlist url is preserved`() {
        assertEquals("http://example.com/list.m3u", ServerUrlNormalizer.normalize("http://example.com/list.m3u"))
    }

    @Test
    fun `m3u url without scheme gets http but keeps query`() {
        assertEquals(
            "http://example.com/get.php?username=u",
            ServerUrlNormalizer.normalize("example.com/get.php?username=u"),
        )
    }

    // ---- playlist entry point ---------------------------------------------------------------

    @Test
    fun `playlist extension-less url is NOT given a trailing slash`() {
        // Appending a slash here can 404 a perfectly valid playlist URL.
        assertEquals("http://example.com/playlist", ServerUrlNormalizer.normalizePlaylistUrl("http://example.com/playlist"))
    }

    @Test
    fun `playlist get_php url preserved verbatim`() {
        val url = "http://example.com/get.php?username=u&password=p&type=m3u_plus"
        assertEquals(url, ServerUrlNormalizer.normalizePlaylistUrl(url))
    }

    @Test
    fun `playlist m3u8 url preserved verbatim`() {
        val url = "https://cdn.example.com/a/b/list.m3u8?token=abc"
        assertEquals(url, ServerUrlNormalizer.normalizePlaylistUrl(url))
    }

    @Test
    fun `playlist url without scheme gets http prepended only`() {
        assertEquals(
            "http://example.com/get.php?u=1",
            ServerUrlNormalizer.normalizePlaylistUrl("example.com/get.php?u=1"),
        )
    }

    @Test
    fun `playlist url whitespace stripped and blank returns empty`() {
        assertEquals("http://example.com/x.m3u", ServerUrlNormalizer.normalizePlaylistUrl("  http://example.com/x.m3u "))
        assertEquals("", ServerUrlNormalizer.normalizePlaylistUrl("   "))
    }

    @Test
    fun `a playlist trailing slash the server expects is preserved`() {
        assertEquals("http://example.com/playlist/", ServerUrlNormalizer.normalizePlaylistUrl("http://example.com/playlist/"))
    }

    // ---- matrices and idempotence -----------------------------------------------------------

    @Test
    fun `xtream host matrix normalizes to scheme host port slash`() {
        val expected = "http://example.com:8080/"
        listOf(
            "http://example.com:8080",
            "http://example.com:8080/",
            "http://example.com:8080//",
            " http://example.com:8080 ",
            "example.com:8080",
            "http://example.com:8080/player_api.php",
            "http://example.com:8080/player_api.php?username=u&password=p",
        ).forEach { input ->
            assertEquals(expected, ServerUrlNormalizer.normalize(input), "input: \"$input\"")
        }
    }

    @Test
    fun `m3u playlist matrix preserves path and query`() {
        val expected = "http://example.com/get.php?username=u&password=p"
        listOf(
            "http://example.com/get.php?username=u&password=p",
            " http://example.com/get.php?username=u&password=p ",
            "example.com/get.php?username=u&password=p",
        ).forEach { input ->
            assertEquals(expected, ServerUrlNormalizer.normalizePlaylistUrl(input), "input: \"$input\"")
        }
    }

    @Test
    fun `normalize is idempotent`() {
        // Credentials get re-saved on every login, so a normalizer that drifted on reapplication
        // would corrupt a working host over time.
        listOf(
            "http://example.com:8080",
            "example.com",
            "http://example.com/get.php?u=1",
            "http://example.com/list.m3u8",
        ).forEach { input ->
            val once = ServerUrlNormalizer.normalize(input)
            assertEquals(once, ServerUrlNormalizer.normalize(once), "input: \"$input\"")
        }
    }

    @Test
    fun `normalizePlaylistUrl is idempotent`() {
        listOf(
            "http://example.com/get.php?u=1",
            "example.com/playlist",
            "https://cdn.example.com/list.m3u8",
        ).forEach { input ->
            val once = ServerUrlNormalizer.normalizePlaylistUrl(input)
            assertEquals(once, ServerUrlNormalizer.normalizePlaylistUrl(once), "input: \"$input\"")
        }
    }
}
