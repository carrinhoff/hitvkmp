@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.epg

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.dataTaskWithRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS actual: fetches the XMLTV feed via async NSURLSession (replaces an earlier
 * synchronous `NSString.stringWithContentsOfURL` call). The synchronous version
 * blocked the calling coroutine and was killed the instant the app backgrounded —
 * users who locked the phone mid-sync got an "Empty EPG response" toast.
 *
 * Async URLSession data tasks survive short backgrounding and don't stall the
 * coroutine while the response is in flight. Combined with a foreground-side
 * `beginBackgroundTask` window (in `SyncBridge.kt`), this covers the common case
 * of "lock screen during a 10-second EPG fetch".
 *
 * For longer fetches across full app suspension, a true background URLSession
 * with file-based handoff is the next step — that requires Swift-side
 * URLSessionDelegate plumbing and an AppDelegate adapter, deferred for now.
 */
actual object EpgStreamingLoader {

    private const val TIMEOUT_SECONDS = 300.0  // 5 minutes — XMLTV feeds can be tens of MB

    actual suspend fun fetchAndParse(
        baseUrl: String,
        username: String,
        password: String,
        onProgress: suspend (processed: Int, stage: String) -> Unit,
    ): EpgDomainData {
        val urlString = "${baseUrl}xmltv.php?username=$username&password=$password"
        val url = NSURL.URLWithString(urlString)
            ?: throw IllegalArgumentException("Invalid EPG URL: $urlString")

        // beginBackgroundTask gives us up to ~30 s of grace if the user backgrounds
        // the app mid-fetch. No-op when called from a BGTask context (the BGTask has
        // its own timer); no-op when foreground stays foreground. Always paired with
        // endBackgroundTask in the finally to avoid the "task expired" warning.
        val app = UIApplication.sharedApplication
        val bgTaskId = app.beginBackgroundTaskWithExpirationHandler(null)
        try {
            val xml = downloadXml(url)
            onProgress(0, "channels")
            return EpgParser.parse(xml).also {
                onProgress(it.channels.size, "channels")
                onProgress(it.programmes.values.sumOf { list -> list.size }, "programmes")
            }
        } finally {
            if (bgTaskId != UIBackgroundTaskInvalid) {
                app.endBackgroundTask(bgTaskId)
            }
        }
    }

    /**
     * Downloads the XMLTV body via an async data task. The continuation resumes
     * once the system-level URLSession callback fires, regardless of whether the
     * app is backgrounded in the meantime (within the OS's tolerance window).
     */
    private suspend fun downloadXml(url: NSURL): String =
        suspendCancellableCoroutine { cont ->
            val config = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
                timeoutIntervalForRequest = TIMEOUT_SECONDS
                timeoutIntervalForResource = TIMEOUT_SECONDS
            }
            val session = NSURLSession.sessionWithConfiguration(config)
            val request = NSURLRequest.requestWithURL(url)

            val task = session.dataTaskWithRequest(request) { data: NSData?, _: NSURLResponse?, error: NSError? ->
                when {
                    error != null -> cont.resumeWithException(
                        RuntimeException("EPG fetch failed: ${error.localizedDescription}")
                    )
                    data == null || data.length.toInt() == 0 -> cont.resumeWithException(
                        RuntimeException("EPG fetch returned no body")
                    )
                    else -> cont.resume(data.toUtf8String())
                }
            }

            cont.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    /**
     * Convert NSData → UTF-8 String via memcpy into a pinned ByteArray. This
     * pattern is the canonical Kotlin/Native bridge for binary data — avoids
     * the cinterop quirks of NSString factory methods on large blobs.
     */
    private fun NSData.toUtf8String(): String {
        val length = this.length.toInt()
        if (length == 0) return ""
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, length.toULong())
        }
        return bytes.decodeToString()
    }
}
