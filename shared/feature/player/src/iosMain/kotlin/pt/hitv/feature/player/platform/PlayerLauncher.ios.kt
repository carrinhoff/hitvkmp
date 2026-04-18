@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pt.hitv.feature.player.platform

import pt.hitv.feature.player.presentChannelPlayer
import pt.hitv.feature.player.presentMoviePlayer
import pt.hitv.feature.player.presentSeriesPlayer

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
    // Mounts shared `ChannelPlayerScreen` over an AVPlayer surface with full overlay
    // parity (sidebar, EPG, sleep timer, aspect cycle, retry). VM bridging + URL
    // normalization happen inside `presentChannelPlayer`.
    presentChannelPlayer(
        url = url,
        name = name,
        titleEpg = titleEpg,
        descEpg = descEpg,
        logoUrl = logoUrl,
        licenseKey = licenseKey,
        categoryTitle = categoryTitle,
        categoryId = categoryId
    )
}

actual fun launchMoviePlayer(
    movieUrl: String,
    movieTitle: String,
    streamId: Int,
    startPositionMs: Long
) {
    presentMoviePlayer(
        movieUrl = movieUrl,
        movieTitle = movieTitle,
        streamId = streamId,
        startPositionMs = startPositionMs
    )
}

actual fun launchSeriesPlayer(
    seriesId: String,
    seasonNumber: Int,
    episodeIndex: Int
) {
    presentSeriesPlayer(
        seriesId = seriesId,
        seasonNumber = seasonNumber,
        episodeIndex = episodeIndex
    )
}
