package pt.hitv.epg

/**
 * Streaming EPG loader — fetches the XMLTV feed and parses it incrementally,
 * never holding the full XML in memory. Mirrors the original Android project's
 * HttpURLConnection + XmlPullParser + InputStream pipeline; avoids Ktor's
 * save()-based body buffering that OOMs on ~80 MB feeds.
 *
 * Android actual uses XmlPullParser on a raw HttpURLConnection InputStream.
 * iOS actual (TODO) should use NSXMLParser.
 */
expect object EpgStreamingLoader {
    /**
     * Downloads and parses the XMLTV feed from [baseUrl]?username=…&password=…
     * Returns the parsed [EpgDomainData] for DB insertion.
     *
     * [onProgress] is called periodically with (processedItems, stage) where
     * stage ∈ {"channels", "programmes"}. Useful for UI progress updates.
     */
    suspend fun fetchAndParse(
        baseUrl: String,
        username: String,
        password: String,
        onProgress: suspend (processed: Int, stage: String) -> Unit = { _, _ -> },
    ): EpgDomainData
}
