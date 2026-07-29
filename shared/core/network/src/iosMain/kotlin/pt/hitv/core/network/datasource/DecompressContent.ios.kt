package pt.hitv.core.network.datasource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.zlib.Z_BUF_ERROR
import platform.zlib.Z_NO_FLUSH
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2
import platform.zlib.z_stream

/**
 * iOS implementation of content decompression.
 *
 * ## What this used to do
 *
 * The previous implementation looked like it decompressed but did not: `decompressGzip()` copied
 * the bytes and then called `NSString.create(data:encoding:)` on the **still-gzipped** data. That
 * returns null for compressed input, so the code fell through to `bytes.decodeToString()` and
 * handed the EPG parser mojibake produced from binary. Providers that serve a gzipped `xmltv`
 * payload therefore got **no EPG at all on iOS**, while Android inflated it correctly — a silent
 * platform divergence in the feature this port most needed to match.
 *
 * ## What it does now
 *
 * Real inflation via zlib, which ships with the platform and has Kotlin/Native bindings.
 * `inflateInit2(strm, 47)` is the standard incantation: 47 = 15 window bits + 32, where the +32
 * tells zlib to auto-detect a gzip or zlib wrapper rather than expecting raw DEFLATE. Output is
 * grown in chunks because the decompressed size is not known up front.
 *
 * XZ is still unsupported — it is not part of the platform and would need a third-party library.
 * That is unchanged from before, and is now surfaced honestly rather than silently returning
 * garbage: [decompressContent] returns an empty string for XZ, which the callers already treat as
 * "no content" rather than trying to parse it.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun decompressContent(bytes: ByteArray): String {
    if (bytes.isEmpty()) return ""

    val isGzip = bytes.size >= 2 &&
        bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    val isXz = bytes.size >= 6 &&
        bytes[0] == 0xFD.toByte() && bytes[1] == 0x37.toByte() &&
        bytes[2] == 0x7A.toByte() && bytes[3] == 0x58.toByte() &&
        bytes[4] == 0x5A.toByte() && bytes[5] == 0x00.toByte()

    return when {
        isGzip -> inflateGzip(bytes)?.decodeToString() ?: ""
        // Returning "" rather than decoding compressed bytes as text: callers check for blank and
        // report "invalid or non-XML content" instead of feeding the parser noise.
        isXz -> ""
        else -> bytes.decodeToString()
    }
}

/** Chunk size for the output buffer. XMLTV feeds inflate to tens of MB, so grow generously. */
private const val INFLATE_CHUNK = 64 * 1024

/**
 * Inflates gzip-wrapped DEFLATE using zlib. Returns null if the stream is malformed.
 */
@OptIn(ExperimentalForeignApi::class)
private fun inflateGzip(input: ByteArray): ByteArray? = memScoped {
    val strm = alloc<z_stream>()
    // 15 window bits | 32 => auto-detect gzip/zlib header.
    if (inflateInit2(strm.ptr, 47) != Z_OK) return null

    val out = ArrayList<Byte>(input.size * 4)
    val buffer = ByteArray(INFLATE_CHUNK)

    try {
        input.usePinned { pinnedIn ->
            strm.next_in = pinnedIn.addressOf(0).reinterpret()
            strm.avail_in = input.size.toUInt()

            // Explicit flag rather than a labelled return: two nested `usePinned` lambdas make
            // `return@usePinned` bind to whichever is nearest, which is easy to misread and easier
            // to break with an innocent edit.
            var finished = false
            while (!finished) {
                var status = Z_OK
                var produced = 0

                buffer.usePinned { pinnedOut ->
                    strm.next_out = pinnedOut.addressOf(0).reinterpret()
                    strm.avail_out = buffer.size.toUInt()
                    status = inflate(strm.ptr, Z_NO_FLUSH)
                    produced = buffer.size - strm.avail_out.toInt()
                }

                for (i in 0 until produced) out.add(buffer[i])

                finished = when (status) {
                    Z_STREAM_END -> true
                    // Z_BUF_ERROR only means "needs more room"; the next iteration supplies it.
                    // With no input left and nothing produced, the stream is truncated.
                    Z_OK, Z_BUF_ERROR -> produced == 0 && strm.avail_in == 0u
                    else -> true
                }
            }
        }
    } finally {
        inflateEnd(strm.ptr)
    }

    return if (out.isEmpty()) null else out.toByteArray()
}
