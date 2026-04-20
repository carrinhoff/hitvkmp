package pt.hitv.epg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import pt.hitv.epg.domain.EPGChannel
import pt.hitv.epg.domain.EPGEvent
import java.io.FilterInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Android actual: streams the XMLTV response through `XmlPullParser` so the
 * 80 MB feed never lives fully in memory. Matches the original project's
 * `XmltvParser` + `XmlSanitizingInputStream` pipeline exactly — uses
 * HttpURLConnection rather than Ktor to avoid `HttpClientCall.save()` buffering.
 */
actual object EpgStreamingLoader {

    actual suspend fun fetchAndParse(
        baseUrl: String,
        username: String,
        password: String,
        onProgress: suspend (processed: Int, stage: String) -> Unit,
    ): EpgDomainData = withContext(Dispatchers.IO) {
        val url = URL("${baseUrl}xmltv.php?username=$username&password=$password")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 120_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw RuntimeException("EPG HTTP $code")
            }
            val raw = conn.inputStream
            parseStream(raw, onProgress)
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun parseStream(
        rawStream: InputStream,
        onProgress: suspend (processed: Int, stage: String) -> Unit,
    ): EpgDomainData {
        val sanitized = XmlSanitizingInputStream(rawStream)
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
        val parser = factory.newPullParser()
        parser.setInput(sanitized, "UTF-8")

        val channels = ArrayList<EPGChannel>(4096)
        val programmes = HashMap<String, MutableList<EPGEvent>>(4096)
        var programmeIdCounter = 0

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "channel" -> {
                            try {
                                parseChannel(parser)?.let {
                                    channels += it
                                    if (channels.size % 500 == 0) onProgress(channels.size, "channels")
                                }
                            } catch (_: Exception) {
                                skipToEndTag(parser, "channel")
                            }
                        }
                        "programme" -> {
                            try {
                                parseProgramme(parser) { startMs, stopMs, chId, title, desc, icon ->
                                    val event = EPGEvent(
                                        id = "epg_${programmeIdCounter++}",
                                        start = startMs,
                                        end = stopMs,
                                        title = title,
                                        description = desc,
                                        imageURL = icon,
                                    )
                                    programmes.getOrPut(chId) { ArrayList(32) } += event
                                    if (programmeIdCounter % 1000 == 0) {
                                        onProgress(programmeIdCounter, "programmes")
                                    }
                                }
                            } catch (_: Exception) {
                                skipToEndTag(parser, "programme")
                            }
                        }
                    }
                }
                try {
                    eventType = parser.next()
                } catch (_: Exception) {
                    try { eventType = parser.next() } catch (_: Exception) { break }
                }
            }
        } catch (_: Exception) { /* fall through with whatever we parsed */ }

        onProgress(channels.size, "channels")
        onProgress(programmeIdCounter, "programmes")
        return EpgDomainData(channels = channels, programmes = programmes)
    }

    private fun parseChannel(parser: XmlPullParser): EPGChannel? {
        val id = parser.getAttributeValue(null, "id") ?: return null
        var displayName = ""
        var icon = ""
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "display-name" -> displayName = parser.nextText().orEmpty()
                    "icon" -> icon = parser.getAttributeValue(null, "src").orEmpty()
                    else -> skipToEndTag(parser, parser.name)
                }
                XmlPullParser.END_TAG -> if (parser.name == "channel") depth = 0
                XmlPullParser.END_DOCUMENT -> break
            }
        }
        return EPGChannel(channelID = id, name = displayName, imageURL = icon)
    }

    private inline fun parseProgramme(
        parser: XmlPullParser,
        emit: (startMs: Long, stopMs: Long, channel: String, title: String, desc: String, icon: String) -> Unit,
    ) {
        val startStr = parser.getAttributeValue(null, "start")
        val stopStr = parser.getAttributeValue(null, "stop")
        val channelAttr = parser.getAttributeValue(null, "channel")

        val startMs = parseXmltvDate(startStr)
        val stopMs = parseXmltvDate(stopStr)

        var title = ""
        var desc = ""
        var icon = ""

        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "title" -> title = runCatching { parser.nextText().orEmpty() }.getOrDefault("")
                    "desc" -> desc = runCatching { parser.nextText().orEmpty() }.getOrDefault("")
                    "icon" -> icon = parser.getAttributeValue(null, "src").orEmpty()
                    else -> skipToEndTag(parser, parser.name)
                }
                XmlPullParser.END_TAG -> if (parser.name == "programme") depth = 0
                XmlPullParser.END_DOCUMENT -> break
            }
        }

        if (startMs != null && stopMs != null && !channelAttr.isNullOrBlank()) {
            emit(startMs, stopMs, channelAttr, title, desc, icon)
        }
    }

    private fun skipToEndTag(parser: XmlPullParser, name: String) {
        var depth = 1
        while (depth > 0) {
            try {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> depth++
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.END_DOCUMENT -> return
                }
            } catch (_: Exception) { return }
        }
    }

    private val dateFormats by lazy {
        listOf("yyyyMMddHHmmss Z", "yyyyMMddHHmmss", "yyyyMMddHHmm Z", "yyyyMMddHHmm").map { pattern ->
            SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
    }

    private fun parseXmltvDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        for (f in dateFormats) {
            try { return f.parse(trimmed)?.time } catch (_: Exception) { }
        }
        return null
    }
}

/**
 * Strips invalid XML control characters byte-by-byte without buffering.
 * Ported verbatim from the original project's XmlSanitizingInputStream.
 */
private class XmlSanitizingInputStream(source: InputStream) : FilterInputStream(source) {
    override fun read(): Int {
        while (true) {
            val b = `in`.read()
            if (b == -1) return -1
            if (isValid(b)) return b
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val tmp = ByteArray(len)
        var writePos = off
        while (writePos == off) {
            val n = `in`.read(tmp, 0, len)
            if (n == -1) return if (writePos > off) writePos - off else -1
            for (i in 0 until n) {
                if (isValid(tmp[i].toInt() and 0xFF)) {
                    b[writePos++] = tmp[i]
                }
            }
        }
        return writePos - off
    }

    private fun isValid(b: Int) = b == 0x09 || b == 0x0A || b == 0x0D || b >= 0x20
}
