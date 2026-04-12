package pt.hitv.feature.player.platform

/**
 * Platform-specific player launchers.
 * Android: Launches Activities via Intent (supports PiP).
 * iOS: Presents native player view controllers.
 */

expect fun launchChannelPlayer(
    url: String,
    name: String,
    titleEpg: String? = null,
    descEpg: String? = null,
    logoUrl: String? = null,
    licenseKey: String? = null,
    categoryTitle: String? = null,
    categoryId: Int = -1
)

expect fun launchMoviePlayer(
    movieUrl: String,
    movieTitle: String,
    streamId: Int = 0,
    startPositionMs: Long = 0L
)

expect fun launchSeriesPlayer(
    seriesId: String,
    seasonNumber: Int,
    episodeIndex: Int
)
