package pt.hitv.android.player

import android.net.Uri
import android.util.Base64
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds the right [MediaSource] for a live stream URL, with ClearKey DRM when a licence is
 * supplied.
 *
 * Ported from the original's `feature/player/.../util/MediaSourceFactory.kt`. The port previously
 * had neither piece, inline in `ChannelPlayerActivity`:
 *
 *  - **DASH went through `ProgressiveMediaSource`**, under a literal
 *    `// Would need DashMediaSource but skipping for now`. `ProgressiveMediaSource` cannot parse an
 *    MPD manifest, so every `.mpd` channel simply failed — and `media3-exoplayer-dash` was already
 *    a declared dependency, so nothing was blocking it.
 *  - **`licenseKey` was carried all the way from the Intent into the ViewModel and then ignored**,
 *    so DRM-protected channels could never play.
 *  - SmoothStreaming (`.ism` / `.isml`) had no branch at all and fell through to progressive.
 *
 * Android-only: AVPlayer has no DASH or ClearKey support, so on iOS these streams remain
 * unplayable regardless — see KMP_MIGRATION_AUDIT.md.
 */
@OptIn(UnstableApi::class)
object LiveMediaSourceFactory {

    /**
     * @param licenseKey optional ClearKey licence in `kidHex:keyHex` form, as the original expects.
     */
    fun create(
        url: String,
        dataSourceFactory: DataSource.Factory,
        licenseKey: String? = null,
    ): MediaSource {
        val mediaItem = MediaItem.fromUri(url)
        val path = Uri.parse(url).path.orEmpty()

        val isDash = path.endsWith(".mpd", ignoreCase = true)
        val isHls = path.endsWith(".m3u8", ignoreCase = true)
        val isSs = path.endsWith(".ism", ignoreCase = true) ||
            path.endsWith(".isml", ignoreCase = true)

        return when {
            isDash && !licenseKey.isNullOrBlank() ->
                createDashSourceWithDrm(dataSourceFactory, mediaItem, licenseKey)

            isDash ->
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            isHls ->
                HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)

            isSs ->
                SsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            // Progressive handles .ts, .mp4 and raw TS streams.
            else ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    /**
     * DASH + ClearKey. Builds a local licence response from the `kid:key` hex pair so no licence
     * server round-trip is needed, exactly as the original does.
     *
     * Falls back to plain DASH if the licence is malformed — a bad key should degrade to "stream
     * won't decrypt", not "app crashes".
     */
    private fun createDashSourceWithDrm(
        dataSourceFactory: DataSource.Factory,
        mediaItem: MediaItem,
        licenseKey: String,
    ): MediaSource = try {
        val parts = licenseKey.split(":")
        require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            "Invalid licenseKey format. Expected hex 'kid:key'."
        }

        val flags = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val kidBase64Url = Base64.encodeToString(hexStringToByteArray(parts[0]), flags)
        val keyBase64Url = Base64.encodeToString(hexStringToByteArray(parts[1]), flags)

        val licenseResponseJson = JSONObject()
            .put(
                "keys",
                JSONArray().put(
                    JSONObject()
                        .put("kty", "oct")
                        .put("k", keyBase64Url)
                        .put("kid", kidBase64Url)
                )
            )
            .put("type", "temporary")
            .toString()

        val drmSessionManager = DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .build(LocalMediaDrmCallback(licenseResponseJson.toByteArray(Charsets.UTF_8)))

        DashMediaSource.Factory(dataSourceFactory)
            .setDrmSessionManagerProvider { drmSessionManager }
            .createMediaSource(mediaItem)
    } catch (_: Exception) {
        DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    /** Hex pair string to bytes. Ported from the original's `hexStringToByteArray`. */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "")
        require(cleanHex.length % 2 == 0) {
            "Hex string must have an even number of characters (was ${cleanHex.length})"
        }
        val data = ByteArray(cleanHex.length / 2)
        for (i in cleanHex.indices step 2) {
            val hi = Character.digit(cleanHex[i], 16)
            val lo = Character.digit(cleanHex[i + 1], 16)
            require(hi >= 0 && lo >= 0) { "Invalid hex character in string: $cleanHex" }
            data[i / 2] = ((hi shl 4) + lo).toByte()
        }
        return data
    }
}
