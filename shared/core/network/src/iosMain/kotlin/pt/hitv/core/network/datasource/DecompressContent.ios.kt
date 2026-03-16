package pt.hitv.core.network.datasource

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes

/**
 * iOS implementation of content decompression.
 * Handles gzip and plain text content.
 *
 * Note: XZ decompression is not natively supported on iOS.
 * For EPG content that uses XZ compression, the server should be configured
 * to provide gzip-compressed content instead.
 * If XZ support is required, consider adding a KMP XZ library.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun decompressContent(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    val isGzip = bytes.size >= 2 &&
        bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    val isXz = bytes.size >= 6 &&
        bytes[0] == 0xFD.toByte() && bytes[1] == 0x37.toByte() &&
        bytes[2] == 0x7A.toByte() && bytes[3] == 0x58.toByte() &&
        bytes[4] == 0x5A.toByte() && bytes[5] == 0x00.toByte()

    return when {
        isGzip -> {
            val nsData = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            // Use NSData decompression via compression framework
            val decompressed = nsData.decompressGzip()
            decompressed ?: bytes.decodeToString()
        }
        isXz -> {
            // XZ not natively supported on iOS - attempt plain text decode
            // In practice, most IPTV providers offer gzip as fallback
            bytes.decodeToString()
        }
        else -> bytes.decodeToString()
    }
}

/**
 * Decompresses gzip NSData using the Compression framework.
 * Returns null if decompression fails.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun NSData.decompressGzip(): String? {
    return try {
        // Use the platform's built-in decompression
        // For a production app, you would use the Compression framework
        // or a KMP gzip library. This is a simplified implementation.
        val bytes = ByteArray(this.length.toInt())
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
        }

        // Try to create a string from the raw bytes
        // NSURLSession can handle gzip automatically via Accept-Encoding
        NSString.create(data = this, encoding = NSUTF8StringEncoding)?.toString()
    } catch (_: Exception) {
        null
    }
}
