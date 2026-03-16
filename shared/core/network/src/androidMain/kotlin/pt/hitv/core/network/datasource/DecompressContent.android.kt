package pt.hitv.core.network.datasource

import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream

/**
 * Android/JVM implementation of content decompression.
 * Handles gzip, xz, and plain text content.
 */
actual fun decompressContent(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    val isGzip = bytes.size >= 2 &&
        bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    val isXz = bytes.size >= 6 &&
        bytes[0] == 0xFD.toByte() && bytes[1] == 0x37.toByte() &&
        bytes[2] == 0x7A.toByte() && bytes[3] == 0x58.toByte() &&
        bytes[4] == 0x5A.toByte() && bytes[5] == 0x00.toByte()

    return when {
        isXz -> {
            XZInputStream(ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
        isGzip -> {
            GZIPInputStream(ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
        else -> bytes.decodeToString()
    }
}
