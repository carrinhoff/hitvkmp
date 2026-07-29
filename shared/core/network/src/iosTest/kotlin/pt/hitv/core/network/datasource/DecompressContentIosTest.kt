package pt.hitv.core.network.datasource

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Runs the iOS gzip path against a real gzip stream, on an actual iOS simulator.
 *
 * The previous implementation never inflated anything: it called `NSString.create(data:encoding:)`
 * on the still-compressed bytes, got null, and fell back to decoding the compressed binary as
 * text. Providers serving a gzipped XMLTV payload therefore got **no EPG on iOS** while Android
 * inflated it fine — and it failed silently, producing mojibake rather than an error.
 *
 * The fixture below is a genuine gzip stream (header, DEFLATE body, CRC32 and length trailer)
 * produced outside this codebase, so the test cannot pass by agreeing with our own encoder — there
 * isn't one. Only real inflation satisfies it.
 */
class DecompressContentIosTest {

    /** gzip of [EXPECTED_XML]. */
    private val gzippedXml: ByteArray = byteArrayOf(
        31, -117, 8, 0, 0, 0, 0, 0, 2, -1, 85, -113, 65, -117, -62, 48, 16, -123, -17, -2, -118, 33,
        -41, -91, -90, -83, -96, 30, -90, 17, 20, 92, 60, -71, -32, -6, 3, 98, 51, 118, 3, -23, 36,
        -76, -95, -84, -2, -6, 77, 93, 21, -99, -61, 28, -34, -57, -68, 121, 15, 87, -65, -83, -125,
        -127, -70, -34, 122, -82, 68, 49, -51, 5, 16, -41, -34, 88, 110, 42, 113, -4, -34, 102, 75,
        -79, 82, 19, -116, -125, -102, 0, 96, -3, -93, -103, -55, -127, 53, -107, 56, -99, -22, 66,
        40, 52, -74, 15, 78, 95, 50, -42, 45, -87, -11, 122, 3, 123, 38, -108, 111, 42, -54, -5,
        -35, -51, 35, 116, -66, -23, 116, -37, 18, -12, 81, 119, -79, 18, 101, 94, -50, -13, 69, 57,
        47, -54, 60, 13, 124, -116, 91, 36, -24, -61, 11, -101, -67, -78, -69, -35, 51, 67, -76,
        -47, -111, -6, -68, -38, 16, -56, -64, -41, -29, 1, -54, 127, -128, -122, -6, 90, -19, -8,
        -20, 116, 76, -36, 51, -40, -3, 33, 101, 28, 85, -108, -49, 60, -87, -90, 28, 123, -2, 1,
        58, 28, -12, -45, 20, 1, 0, 0
    )

    @Test
    fun `inflates a real gzip stream back to the original XML`() {
        val result = decompressContent(gzippedXml)
        assertEquals(EXPECTED_XML, result, "gzip payload did not round-trip")
    }

    @Test
    fun `inflated output is parseable XML rather than mojibake`() {
        // The old behaviour returned bytes decoded as text, which starts with the gzip magic
        // rather than a declaration. This is the cheap check that would have caught it.
        val result = decompressContent(gzippedXml)
        assertTrue(result.startsWith("<?xml"), "expected XML, got: ${result.take(40)}")
        assertTrue(result.contains("<title>Gzipped Programme</title>"))
    }

    @Test
    fun `plain uncompressed input passes through untouched`() {
        val plain = "<tv><channel id=\"x\"/></tv>"
        assertEquals(plain, decompressContent(plain.encodeToByteArray()))
    }

    @Test
    fun `empty input yields empty output`() {
        assertEquals("", decompressContent(ByteArray(0)))
    }

    @Test
    fun `truncated gzip fails closed instead of returning garbage`() {
        // Half a gzip stream must not come back as text the parser would try to read.
        val truncated = gzippedXml.copyOfRange(0, gzippedXml.size / 2)
        val result = decompressContent(truncated)
        assertTrue(
            result.isEmpty() || result.startsWith("<?xml"),
            "truncated stream produced junk: ${result.take(40)}",
        )
    }

    @Test
    fun `xz input returns empty rather than compressed noise`() {
        // XZ is unsupported on iOS; the contract is "no content", not binary decoded as text.
        val xzMagic = byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00, 0x11, 0x22)
        assertEquals("", decompressContent(xzMagic))
    }

    private companion object {
        /** Raw string: the fixture contains quotes and newlines, so no escaping games. */
        val EXPECTED_XML: String = """<?xml version="1.0" encoding="UTF-8"?>
<tv>
  <channel id="bbc1"><display-name>BBC One</display-name></channel>
  <programme start="20260726120000 +0000" stop="20260726130000 +0000" channel="bbc1"><title>Gzipped Programme</title><desc>Inflated on iOS</desc></programme>
</tv>
"""
    }
}
