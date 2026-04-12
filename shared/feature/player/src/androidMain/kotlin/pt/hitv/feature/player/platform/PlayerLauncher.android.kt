package pt.hitv.feature.player.platform

import android.content.Intent
import pt.hitv.core.common.AndroidContextHolder

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
    val context = AndroidContextHolder.applicationContext
    val intent = Intent().apply {
        setClassName(context, "pt.hitv.android.player.ChannelPlayerActivity")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("url", url)
        putExtra("name", name)
        putExtra("titleEpg", titleEpg)
        putExtra("descEpg", descEpg)
        putExtra("imgEpg", logoUrl)
        putExtra("categoryTitle", categoryTitle)
        putExtra("categoryId", categoryId)
        putExtra("licenseKey", licenseKey)
    }
    context.startActivity(intent)
}

actual fun launchMoviePlayer(
    movieUrl: String,
    movieTitle: String,
    streamId: Int,
    startPositionMs: Long
) {
    val context = AndroidContextHolder.applicationContext
    val intent = Intent().apply {
        setClassName(context, "pt.hitv.android.player.MoviePlayerActivity")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("url", movieUrl)
        putExtra("title", movieTitle)
        putExtra("streamId", streamId)
        putExtra("startPositionMs", startPositionMs)
    }
    context.startActivity(intent)
}

actual fun launchSeriesPlayer(
    seriesId: String,
    seasonNumber: Int,
    episodeIndex: Int
) {
    val context = AndroidContextHolder.applicationContext
    val intent = Intent().apply {
        setClassName(context, "pt.hitv.android.player.SeriesPlayerActivity")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("seriesId", seriesId)
        putExtra("seasonNumber", seasonNumber)
        putExtra("episodeIndex", episodeIndex)
    }
    context.startActivity(intent)
}
