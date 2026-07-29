package pt.hitv.feature.player

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins both normalizers, and in particular why iOS live playback needs its own.
 *
 * [MediaUrlNormalizer.normalize] appends the `output` preference, which is
 * `allowedOutputFormats.firstOrNull() ?: ""`. That is the original's behaviour and it is fine on
 * Android, where ExoPlayer plays raw MPEG-TS over HTTP. AVFoundation does not: it handles HLS and
 * progressive MP4/MOV, so the two most common values of that preference — empty, and `"ts"` —
 * both produce a URL an `AVPlayer` cannot decode.
 *
 * The practical shape of the bug: an M3U account (no Xtream `user_info`, so `output` is empty) or a
 * provider listing `ts` first would play live TV on Android and show a channel that never starts on
 * iOS, with no error the user could act on.
 */
class MediaUrlNormalizerTest {

    private val extensionless = "http://host:8080/live/user/pass/12345"

    // ---- the general normalizer (unchanged behaviour) ---------------------------------------

    @Test
    fun `appends the output format when the url has no extension`() {
        assertEquals("$extensionless.m3u8", MediaUrlNormalizer.normalize(extensionless, "m3u8"))
        assertEquals("$extensionless.ts", MediaUrlNormalizer.normalize(extensionless, "ts"))
    }

    @Test
    fun `leaves a url that already has a known extension alone`() {
        listOf(".m3u8", ".mpd", ".ism", ".isml", ".ts", ".mp4", ".webm").forEach { ext ->
            val url = "$extensionless$ext"
            assertEquals(url, MediaUrlNormalizer.normalize(url, "m3u8"), "for $ext")
        }
    }

    @Test
    fun `leaves the url untouched when no output format is set`() {
        // This is the case that breaks iOS: extension-less, so the server serves raw TS.
        assertEquals(extensionless, MediaUrlNormalizer.normalize(extensionless, null))
        assertEquals(extensionless, MediaUrlNormalizer.normalize(extensionless, ""))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals(extensionless, MediaUrlNormalizer.normalize("  $extensionless  ", null))
    }

    @Test
    fun `extension matching is case-insensitive`() {
        val upper = "$extensionless.M3U8"
        assertEquals(upper, MediaUrlNormalizer.normalize(upper, "ts"))
    }

    // ---- the AVFoundation live variant --------------------------------------------------------

    @Test
    fun `live on ios asks for hls when there is no extension`() {
        assertEquals("$extensionless.m3u8", MediaUrlNormalizer.normalizeLiveForAvPlayer(extensionless))
    }

    @Test
    fun `live on ios does not hand avplayer a raw ts stream - the regression`() {
        // With the general normalizer and an `output` of "ts", iOS received "....ts" and failed to
        // decode. The live variant ignores the preference for exactly this reason.
        val viaGeneral = MediaUrlNormalizer.normalize(extensionless, "ts")
        assertEquals("$extensionless.ts", viaGeneral)

        val viaLive = MediaUrlNormalizer.normalizeLiveForAvPlayer(extensionless)
        assertEquals("$extensionless.m3u8", viaLive)
    }

    @Test
    fun `an m3u account with no output preference still gets hls`() {
        // M3U accounts have no Xtream user_info, so `output` is always empty for them.
        assertEquals(extensionless, MediaUrlNormalizer.normalize(extensionless, ""))
        assertEquals("$extensionless.m3u8", MediaUrlNormalizer.normalizeLiveForAvPlayer(extensionless))
    }

    @Test
    fun `a url that already declares its container is respected`() {
        // Providers that hand out a complete URL must not have anything appended — including the
        // ones already serving HLS, and the direct-file ones.
        //
        // `.ts` is deliberately absent: it is rewritten to `.m3u8`, since AVFoundation cannot play
        // a raw transport stream. See `live on ios rewrites a raw ts stream to hls`.
        listOf("$extensionless.m3u8", "http://host/path/movie.mp4").forEach { url ->
            assertEquals(url, MediaUrlNormalizer.normalizeLiveForAvPlayer(url), "for $url")
        }
    }

    @Test
    fun `whitespace is trimmed before the extension check`() {
        // Otherwise a trailing space defeats endsWith and ".m3u8" gets appended to a URL that
        // already has it.
        assertEquals("$extensionless.m3u8", MediaUrlNormalizer.normalizeLiveForAvPlayer(" $extensionless.m3u8 "))
    }

    @Test
    fun `live on ios rewrites a raw ts stream to hls`() {
        // CatchUpUrlBuilder emits Flussonic timeshift as ".../timeshift_abs-{utc}.ts" — the
        // documented Flussonic convention, fine on Android, undecodable by AVFoundation. Flussonic
        // serves the same recording as HLS at the .m3u8 variant.
        assertEquals(
            "http://host/ch1/timeshift_abs-1700000000.m3u8",
            MediaUrlNormalizer.normalizeLiveForAvPlayer("http://host/ch1/timeshift_abs-1700000000.ts"),
        )
    }

    @Test
    fun `the ts rewrite preserves the query string`() {
        assertEquals(
            "http://host/ch1/timeshift_abs-1700000000.m3u8?token=abc",
            MediaUrlNormalizer.normalizeLiveForAvPlayer("http://host/ch1/timeshift_abs-1700000000.ts?token=abc"),
        )
    }

    @Test
    fun `the ts rewrite leaves other containers alone`() {
        listOf("$extensionless.m3u8", "http://host/path/movie.mp4").forEach { url ->
            assertEquals(url, MediaUrlNormalizer.normalizeLiveForAvPlayer(url), "for $url")
        }
    }

    @Test
    fun `an extension behind a query string is still recognised - the corruption bug`() {
        // Providers routinely append tokens. Checking endsWith against the whole URL missed the
        // extension and produced "….m3u8?token=abc.ts", which cannot resolve. The original guards
        // this with an extra !url.contains(".m3u8") check that this port had dropped.
        val tokenised = "$extensionless.m3u8?token=abc"
        assertEquals(tokenised, MediaUrlNormalizer.normalize(tokenised, "ts"))
        assertEquals(tokenised, MediaUrlNormalizer.normalizeLiveForAvPlayer(tokenised))
    }

    @Test
    fun `an extension is inserted before the query rather than after it`() {
        val tokenless = "$extensionless?token=abc"
        assertEquals("$extensionless.ts?token=abc", MediaUrlNormalizer.normalize(tokenless, "ts"))
        assertEquals("$extensionless.m3u8?token=abc", MediaUrlNormalizer.normalizeLiveForAvPlayer(tokenless))
    }

    @Test
    fun `a fragment is treated like a query`() {
        val fragment = "$extensionless.m3u8#start"
        assertEquals(fragment, MediaUrlNormalizer.normalize(fragment, "ts"))
    }
}
