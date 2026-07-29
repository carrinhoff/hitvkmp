@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.epg

import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSInputStream
import platform.Foundation.NSOutputStream
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSXMLParser
import platform.Foundation.NSXMLParserDelegateProtocol
import platform.Foundation.downloadTaskWithRequest
import platform.UIKit.UIApplication
import platform.UIKit.UIBackgroundTaskInvalid
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS actual: streams the XMLTV feed to disk, then parses it with `NSXMLParser` straight off the
 * file — so the feed is never resident in memory as a whole.
 *
 * ## What this replaces, and why it mattered
 *
 * The previous implementation used `dataTaskWithRequest`, which buffers the entire body in
 * `NSData`, then copied that into a `ByteArray`, then decoded that into a Kotlin `String`. All
 * three are alive at the same moment, and a Kotlin string is UTF-16, so an 80 MB feed peaked
 * somewhere around 320 MB. The foreground app may survive that on a recent device; a `BGTask` is
 * granted far less and gets jetsam-killed. That is why background EPG sync was unreliable on iOS
 * while Android — which streams through `XmlPullParser` and never materialises the document — was
 * fine.
 *
 * Now:
 *  - `downloadTaskWithRequest` writes the response to a temporary file as it arrives, so download
 *    memory is a fixed buffer regardless of feed size. URLSession still decompresses
 *    `Content-Encoding: gzip` transparently, exactly as the data task did.
 *  - `NSXMLParser(contentsOfURL:)` reads that file incrementally and emits SAX events.
 *  - [XmltvSaxAssembler] turns those into [EpgDomainData], applying the allowlist and time window
 *    as it goes, so a rejected programme never allocates its title or description.
 *
 * Peak memory now tracks the *retained* EPG rather than the size of the feed.
 *
 * All the assembly logic lives in `commonMain` and is covered by `XmltvSaxAssemblerTest`, including
 * parity with [EpgParser] — the regex parser Android's path and every existing test still use. This
 * file is deliberately a thin adapter: element name, attributes, text, in and out.
 */
actual object EpgStreamingLoader {

    private const val TIMEOUT_SECONDS = 300.0  // 5 minutes — XMLTV feeds can be tens of MB

    actual suspend fun fetchAndParse(
        baseUrl: String,
        username: String,
        password: String,
        onProgress: suspend (processed: Int, stage: String) -> Unit,
        channelFilter: Set<String>?,
        minEndTimeMs: Long,
        maxStartTimeMs: Long,
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
        var localFile: NSURL? = null
        try {
            val raw = downloadToFile(url)
            // Strip XML-illegal control bytes before NSXMLParser sees them. Real XMLTV feeds
            // contain them — the original project ships an `XmlSanitizingInputStream` for exactly
            // this, and the Android actual pipes every response through it. NSXMLParser is strict
            // and aborts on the first one, so without this a feed Android handles fine would kill
            // EPG entirely on iOS.
            localFile = try {
                sanitizeXmlFile(raw)
            } finally {
                NSFileManager.defaultManager.removeItemAtURL(raw, null)
            }
            onProgress(0, "channels")

            val assembler = XmltvSaxAssembler(
                channelFilter = channelFilter,
                minEndTimeMs = minEndTimeMs,
                maxStartTimeMs = maxStartTimeMs,
            )
            val parsedCleanly = parseXmltvFile(localFile, assembler)
            val result = assembler.build()

            // Android skips a malformed element and carries on, returning whatever it read; iOS
            // used to throw the whole guide away. Match Android: partial data beats none. Failing
            // loudly is still right when NOTHING parsed, because returning an empty EpgDomainData
            // makes the sync clear the user's guide and look like the provider had no data.
            if (!parsedCleanly && result.channels.isEmpty() && result.programmes.isEmpty()) {
                throw RuntimeException("EPG XML parse failed before any usable data was read")
            }

            return result.also {
                onProgress(it.channels.size, "channels")
                onProgress(it.programmes.values.sumOf { list -> list.size }, "programmes")
            }
        } finally {
            localFile?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
            if (bgTaskId != UIBackgroundTaskInvalid) {
                app.endBackgroundTask(bgTaskId)
            }
        }
    }

    /**
     * Downloads the feed to a file we own and returns its URL.
     *
     * URLSession hands the completion handler a temporary file that it deletes as soon as the
     * handler returns, so the file is moved somewhere we control before parsing. The move is
     * within the same volume, so it is a rename rather than a copy.
     */
    private suspend fun downloadToFile(url: NSURL): NSURL =
        suspendCancellableCoroutine { cont ->
            val config = NSURLSessionConfiguration.defaultSessionConfiguration().apply {
                timeoutIntervalForRequest = TIMEOUT_SECONDS
                timeoutIntervalForResource = TIMEOUT_SECONDS
            }
            val session = NSURLSession.sessionWithConfiguration(config)
            val request = NSURLRequest.requestWithURL(url)

            val task = session.downloadTaskWithRequest(request) { tempUrl: NSURL?, _: NSURLResponse?, error: NSError? ->
                when {
                    error != null -> cont.resumeWithException(
                        RuntimeException("EPG fetch failed: ${error.localizedDescription}")
                    )

                    tempUrl == null -> cont.resumeWithException(
                        RuntimeException("EPG fetch returned no body")
                    )

                    else -> {
                        val fm = NSFileManager.defaultManager
                        val unique = NSProcessInfo.processInfo.globallyUniqueString
                        val destination = NSURL.fileURLWithPath(
                            NSTemporaryDirectory() + "hitv-epg-$unique.xml"
                        )
                        if (fm.moveItemAtURL(tempUrl, destination, null)) {
                            cont.resume(destination)
                        } else {
                            cont.resumeWithException(
                                RuntimeException("Could not move the downloaded EPG file into place")
                            )
                        }
                    }
                }
            }

            cont.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    /**
     * Runs `NSXMLParser` over [fileUrl], feeding every SAX event into [assembler].
     *
     * `internal` rather than private so `EpgStreamingLoaderIosTest` can drive the real parser and
     * the real delegate against a fixture on the simulator — the adapter is the one part of this
     * path that common tests cannot reach.
     */
    internal fun parseXmltvFile(fileUrl: NSURL, assembler: XmltvSaxAssembler): Boolean {
        val parser = NSXMLParser(contentsOfURL = fileUrl)
            ?: throw RuntimeException("Could not open the EPG file for parsing")

        // Held in a local for the duration of parse(): NSXMLParser's delegate property is weak, so
        // letting this go out of scope early would leave it with a dead delegate and silently
        // produce an empty EPG.
        val delegate = AssemblerDelegate(assembler)
        parser.delegate = delegate

        // Whatever the delegate already assembled stays in `assembler`; the caller decides
        // whether a partial read is usable.
        return parser.parse()
    }

    /**
     * Copies [source] to a fresh file, dropping bytes XML forbids.
     *
     * A verbatim port of the original's `XmlSanitizingInputStream` predicate — tab, LF, CR and
     * anything from 0x20 up are kept, every other control byte is discarded. Done as a streaming
     * copy with a fixed buffer so the feed still never has to be resident.
     */
    internal fun sanitizeXmlFile(source: NSURL): NSURL {
        val destination = NSURL.fileURLWithPath(
            NSTemporaryDirectory() + "hitv-epg-clean-${NSProcessInfo.processInfo.globallyUniqueString}.xml"
        )
        val input = NSInputStream(uRL = source)
            ?: throw RuntimeException("Could not open the downloaded EPG file")
        val output = NSOutputStream(uRL = destination, append = false)
            ?: throw RuntimeException("Could not open a file to sanitize the EPG into")

        input.open()
        output.open()
        try {
            memScoped {
                val capacity = 64 * 1024
                val inBuf = allocArray<UByteVar>(capacity)
                val outBuf = allocArray<UByteVar>(capacity)
                while (true) {
                    val read = input.read(inBuf, capacity.toULong()).toInt()
                    if (read <= 0) break

                    var kept = 0
                    for (i in 0 until read) {
                        val b = inBuf[i].toInt()
                        if (b == 0x09 || b == 0x0A || b == 0x0D || b >= 0x20) {
                            outBuf[kept++] = inBuf[i]
                        }
                    }

                    // Always write from index 0 and shift any unwritten tail forward, rather
                    // than doing pointer arithmetic on the buffer.
                    var pending = kept
                    while (pending > 0) {
                        val n = output.write(outBuf, pending.toULong()).toInt()
                        if (n <= 0) throw RuntimeException("Failed writing the sanitized EPG file")
                        if (n < pending) {
                            for (i in 0 until (pending - n)) outBuf[i] = outBuf[n + i]
                        }
                        pending -= n
                    }
                }
            }
        } finally {
            input.close()
            output.close()
        }
        return destination
    }

    /**
     * Forwards NSXMLParser's SAX callbacks into [XmltvSaxAssembler].
     *
     * NSXMLParser resolves entities itself, so the text handed to [XmltvSaxAssembler.characters] is
     * already decoded — `EpgParser.decodeXmlEntities` must not be applied on top of it. It also
     * splits text at buffer boundaries and around entities, which is why the assembler accumulates
     * rather than replaces (pinned by `XmltvSaxAssemblerTest`).
     */
    private class AssemblerDelegate(
        private val assembler: XmltvSaxAssembler,
    ) : NSObject(), NSXMLParserDelegateProtocol {

        override fun parser(
            parser: NSXMLParser,
            didStartElement: String,
            namespaceURI: String?,
            qualifiedName: String?,
            attributes: Map<Any?, *>,
        ) {
            // Short-lived per element, and only for elements the assembler acts on. Building it
            // unconditionally would allocate a map for every node in the document.
            val attrs = if (attributes.isEmpty()) {
                emptyMap()
            } else {
                buildMap(attributes.size) {
                    attributes.forEach { (key, value) ->
                        val k = key as? String ?: return@forEach
                        val v = value as? String ?: return@forEach
                        put(k, v)
                    }
                }
            }
            assembler.startElement(didStartElement, attrs)
        }

        override fun parser(parser: NSXMLParser, foundCharacters: String) {
            assembler.characters(foundCharacters)
        }

        override fun parser(
            parser: NSXMLParser,
            didEndElement: String,
            namespaceURI: String?,
            qualifiedName: String?,
        ) {
            assembler.endElement(didEndElement)
        }
    }
}
