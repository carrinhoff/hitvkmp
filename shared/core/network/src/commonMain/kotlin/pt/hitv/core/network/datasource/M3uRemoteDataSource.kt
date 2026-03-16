package pt.hitv.core.network.datasource

import pt.hitv.core.common.Resources
import pt.hitv.core.network.api.M3uDownloadService

/**
 * Decompresses raw bytes that may be gzip, xz, or plain text encoded.
 * Platform-specific implementations handle the actual decompression.
 */
expect fun decompressContent(bytes: ByteArray): String

/**
 * Remote data source for M3U playlist and EPG content downloads.
 * Replaces the Hilt-injected M3uRemoteDataSource.
 *
 * Handles gzip and xz decompression of EPG content via platform-specific [decompressContent].
 */
class M3uRemoteDataSource(
    private val m3uDownloadService: M3uDownloadService
) : BaseRemoteDataSource() {

    suspend fun fetchEpgFromUrl(url: String): Resources<String> {
        val bytesResult = getResult(endpoint = "download_epg") {
            m3uDownloadService.downloadEpgContentAsBytes(url)
        }

        return when (bytesResult) {
            is Resources.Success -> {
                try {
                    val bytes = bytesResult.data
                    val content = decompressContent(bytes)

                    if (content.isNotBlank()) {
                        Resources.Success(content)
                    } else {
                        Resources.Error("EPG content is empty")
                    }
                } catch (e: Exception) {
                    Resources.Error("Failed to read/decompress EPG content: ${e.message}")
                }
            }
            is Resources.Error -> Resources.Error(bytesResult.message)
            is Resources.Loading -> Resources.Loading()
        }
    }

    suspend fun fetchM3uContent(url: String): Resources<String> {
        val contentResult = getResult(endpoint = "download_m3u") {
            m3uDownloadService.downloadM3uContent(url)
        }

        return when (contentResult) {
            is Resources.Success -> {
                try {
                    val content = contentResult.data

                    if (content.isBlank()) {
                        return Resources.Error("M3U content is empty")
                    }

                    val trimmed = content.trimStart()
                    if (trimmed.startsWith("<!doctype", ignoreCase = true) ||
                        trimmed.startsWith("<html", ignoreCase = true)
                    ) {
                        return Resources.Error("URL returned a web page instead of an M3U playlist")
                    }

                    if (!trimmed.startsWith("#EXTM3U") && !content.contains("#EXTINF:")) {
                        return Resources.Error("URL did not return valid M3U content")
                    }

                    Resources.Success(content)
                } catch (e: Exception) {
                    Resources.Error("Failed to read M3U content: ${e.message}")
                }
            }
            is Resources.Error -> Resources.Error(contentResult.message)
            is Resources.Loading -> Resources.Loading()
        }
    }
}
