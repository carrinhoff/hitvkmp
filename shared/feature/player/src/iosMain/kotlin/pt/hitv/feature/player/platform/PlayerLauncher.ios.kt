package pt.hitv.feature.player.platform

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

/**
 * iOS player launcher — presents AVPlayerViewController modally.
 * Uses .m3u8 extension for HLS compatibility.
 */
actual fun launchChannelPlayer(
    url: String,
    name: String,
    titleEpg: String?,
    descEpg: String?,
    logoUrl: String?,
    licenseKey: String?,
    categoryTitle: String?,
    categoryId: Int
) {
    val hlsUrl = normalizeUrlForIos(url)
    presentAVPlayer(hlsUrl)
}

actual fun launchMoviePlayer(
    movieUrl: String,
    movieTitle: String,
    streamId: Int,
    startPositionMs: Long
) {
    val hlsUrl = normalizeUrlForIos(movieUrl)
    presentAVPlayer(hlsUrl)
}

actual fun launchSeriesPlayer(
    seriesId: String,
    seasonNumber: Int,
    episodeIndex: Int
) {
    // Series player needs the URL built from seriesId — this would come from the caller
    // For now this is a no-op since the URL isn't passed directly
    println("iOS launchSeriesPlayer: series=$seriesId season=$seasonNumber episode=$episodeIndex")
}

/**
 * Normalizes URL for iOS: forces .m3u8 extension for HLS playback.
 */
private fun normalizeUrlForIos(url: String): String {
    val trimmed = url.trim()
    val knownExtensions = listOf(".m3u8", ".mpd", ".ts", ".mp4", ".webm")
    val hasKnownExtension = knownExtensions.any { trimmed.endsWith(it, ignoreCase = true) }

    return if (!hasKnownExtension) {
        "$trimmed.m3u8"
    } else if (trimmed.endsWith(".ts", ignoreCase = true) || trimmed.endsWith(".mp4", ignoreCase = true)) {
        // Replace non-HLS extensions with m3u8 for server-side transcoding
        trimmed.substringBeforeLast(".") + ".m3u8"
    } else {
        trimmed
    }
}

/**
 * Presents an AVPlayerViewController modally from the top-most view controller.
 */
private fun presentAVPlayer(urlString: String) {
    val nsUrl = NSURL.URLWithString(urlString) ?: return
    val player = AVPlayer(uRL = nsUrl)
    val playerViewController = AVPlayerViewController()
    playerViewController.player = player

    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var topVC = rootVC
    while (topVC.presentedViewController != null) {
        topVC = topVC.presentedViewController!!
    }

    topVC.presentViewController(playerViewController, animated = true) {
        player.play()
    }
}
