package pt.hitv.feature.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pins which VOD containers iOS is told it cannot open.
 *
 * Xtream serves a title in whatever container the provider stored it in, and the URL says so:
 * `…/movie/user/pass/12345.mkv`. ExoPlayer has extractors for Matroska, AVI and FLV, so the
 * original — Android-only — plays them without anyone thinking about it. AVFoundation does not.
 *
 * That part cannot be fixed from the client; the file really is an MKV. What this changes is the
 * failure mode: instead of a black screen followed by a 25-second watchdog timeout saying "the
 * stream may be unavailable" — misleading, because the stream is fine and merely undecodable —
 * the user gets an immediate message naming the format.
 */
class AvFoundationSupportTest {

    private val base = "http://host:8080/movie/user/pass/12345"

    // ---- container extraction ---------------------------------------------------------------

    @Test
    fun `reads the container from the url`() {
        assertEquals("mkv", AvFoundationSupport.containerOf("$base.mkv"))
        assertEquals("mp4", AvFoundationSupport.containerOf("$base.mp4"))
    }

    @Test
    fun `container matching is case-insensitive`() {
        assertEquals("mkv", AvFoundationSupport.containerOf("$base.MKV"))
        assertTrue(AvFoundationSupport.isUnsupportedByAvFoundation("$base.MKV"))
    }

    @Test
    fun `a token on the url does not hide the container`() {
        // Providers routinely append tokens; the container must still be seen.
        assertEquals("mkv", AvFoundationSupport.containerOf("$base.mkv?token=abc"))
        assertTrue(AvFoundationSupport.isUnsupportedByAvFoundation("$base.mkv?token=abc"))
    }

    @Test
    fun `a url with no extension has no container`() {
        assertNull(AvFoundationSupport.containerOf(base))
        assertNull(AvFoundationSupport.containerOf("$base?token=abc"))
    }

    @Test
    fun `a dot in a path segment is not mistaken for an extension`() {
        assertNull(AvFoundationSupport.containerOf("http://host/my.folder/12345"))
    }

    @Test
    fun `a trailing dot yields no container`() {
        assertNull(AvFoundationSupport.containerOf("$base."))
    }

    // ---- the deny-list ------------------------------------------------------------------------

    @Test
    fun `containers ios cannot decode are rejected`() {
        listOf("mkv", "avi", "flv", "wmv", "divx", "vob", "rmvb", "mpg", "mpeg", "webm", "m2ts", "asf")
            .forEach { ext ->
                assertTrue(
                    AvFoundationSupport.isUnsupportedByAvFoundation("$base.$ext"),
                    "expected .$ext to be rejected",
                )
            }
    }

    @Test
    fun `a direct ts url is rejected but an m3u8 manifest is not`() {
        // HLS is delivered as .ts segments behind an .m3u8 manifest, and AVFoundation plays that
        // happily. A raw .ts file played progressively is a different thing and does not work.
        assertTrue(AvFoundationSupport.isUnsupportedByAvFoundation("$base.ts"))
        assertFalse(AvFoundationSupport.isUnsupportedByAvFoundation("$base.m3u8"))
    }

    @Test
    fun `containers ios can play are allowed`() {
        listOf("mp4", "m4v", "mov", "m3u8").forEach { ext ->
            assertFalse(
                AvFoundationSupport.isUnsupportedByAvFoundation("$base.$ext"),
                "expected .$ext to be allowed",
            )
        }
    }

    @Test
    fun `an unknown container is allowed through`() {
        // Deny-list, not allow-list: guessing wrong here only reproduces the old behaviour, whereas
        // an over-eager allow-list would block titles that play fine.
        assertFalse(AvFoundationSupport.isUnsupportedByAvFoundation("$base.xyz"))
        assertFalse(AvFoundationSupport.isUnsupportedByAvFoundation(base))
    }

    // ---- ClearKey DRM ---------------------------------------------------------------------------

    @Test
    fun `a channel with a licence key is reported as undecryptable`() {
        // ExoPlayer supports ClearKey and LiveMediaSourceFactory wires a DrmSessionManager for it.
        // AVFoundation implements FairPlay only, so the licenceKey reaching the iOS player is
        // accepted and never used, and the stream simply fails.
        val message = AvFoundationSupport.drmUnsupportedMessage("abc123:def456")
        assertNotNull(message)
        assertTrue(message.contains("DRM"), "message should say why: $message")
    }

    @Test
    fun `an unprotected channel produces no drm message`() {
        // Callers treat null as "nothing special about this stream".
        assertNull(AvFoundationSupport.drmUnsupportedMessage(null))
        assertNull(AvFoundationSupport.drmUnsupportedMessage(""))
        assertNull(AvFoundationSupport.drmUnsupportedMessage("   "))
    }

    // ---- the message ---------------------------------------------------------------------------

    @Test
    fun `the message names the actual format`() {
        val message = AvFoundationSupport.unsupportedContainerMessage("$base.mkv")
        assertNotNull(message)
        assertTrue(message.contains(".mkv"), "message should name the container: $message")
    }

    @Test
    fun `no message for a playable or unknown container`() {
        // Callers use null as "go ahead and load it".
        assertNull(AvFoundationSupport.unsupportedContainerMessage("$base.mp4"))
        assertNull(AvFoundationSupport.unsupportedContainerMessage("$base.m3u8"))
        assertNull(AvFoundationSupport.unsupportedContainerMessage(base))
    }
}
