package pt.hitv.epg

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.stringWithContentsOfURL

/**
 * iOS actual: reads the XMLTV feed directly from NSURL (no Ktor client, so
 * no HttpClientCall.save() double-buffering) and delegates to the existing
 * regex-based [EpgParser]. The content is still loaded as a single String —
 * acceptable on iOS where process heap headroom is much larger than Android's
 * 256 MB default. TODO: swap to NSXMLParser for full streaming.
 */
@OptIn(ExperimentalForeignApi::class)
actual object EpgStreamingLoader {

    actual suspend fun fetchAndParse(
        baseUrl: String,
        username: String,
        password: String,
        onProgress: suspend (processed: Int, stage: String) -> Unit,
    ): EpgDomainData = withContext(Dispatchers.Default) {
        val urlString = "${baseUrl}xmltv.php?username=$username&password=$password"
        val url = NSURL.URLWithString(urlString)
            ?: throw IllegalArgumentException("Invalid EPG URL: $urlString")

        val xml = NSString.stringWithContentsOfURL(url, NSUTF8StringEncoding, null) as? String
            ?: throw RuntimeException("Empty EPG response")

        onProgress(0, "channels")
        EpgParser.parse(xml).also {
            onProgress(it.channels.size, "channels")
            onProgress(it.programmes.values.sumOf { list -> list.size }, "programmes")
        }
    }
}
