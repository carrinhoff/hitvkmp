package pt.hitv.core.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText

/**
 * Ktor-based service for downloading M3U playlists and EPG content.
 * Replaces the Retrofit M3uDownloadService interface.
 *
 * Downloads raw content from absolute URLs (bypasses host selection).
 */
class M3uDownloadService(private val client: HttpClient) {

    /**
     * Downloads M3U content from the given URL and returns it as a string.
     */
    suspend fun downloadM3uContent(url: String): String {
        return client.get(url).bodyAsText()
    }

    /**
     * Downloads EPG content from the given URL and returns it as raw bytes
     * (may be gzip or xz compressed).
     */
    suspend fun downloadEpgContentAsBytes(url: String): ByteArray {
        return client.get(url).bodyAsBytes()
    }

    /**
     * Downloads EPG content from the given URL and returns the full response
     * for header inspection.
     */
    suspend fun downloadEpgContent(url: String): HttpResponse {
        return client.get(url)
    }
}
