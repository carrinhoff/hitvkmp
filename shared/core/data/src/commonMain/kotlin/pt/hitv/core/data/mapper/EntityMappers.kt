package pt.hitv.core.data.mapper

import pt.hitv.core.database.Channel as DbChannel
import pt.hitv.core.database.Movie as DbMovie
import pt.hitv.core.database.TvShow as DbTvShow
import pt.hitv.core.database.Category as DbCategory
import pt.hitv.core.database.CategoryVod as DbCategoryVod
import pt.hitv.core.database.CategoryTvShow as DbCategoryTvShow
import pt.hitv.core.database.ParentalControl as DbParentalControl
import pt.hitv.core.database.UserCredentials as DbUserCredentials
import pt.hitv.core.database.CustomGroup as DbCustomGroup
import pt.hitv.core.database.SeriesInfo as DbSeriesInfo
import pt.hitv.core.model.Channel
import pt.hitv.core.model.Movie
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.Category
import pt.hitv.core.model.ParentalControl
import pt.hitv.core.model.UserCredentials
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.model.seriesInfo.SeriesInfo

// ==================== Channel Mappers ====================

fun DbChannel.toChannel() = Channel(
    name = name,
    streamIcon = streamIcon,
    streamUrl = streamUrl,
    epgChannelId = epgChannelId,
    categoryId = categoryCreatorId,
    isFavorite = isFavorite,
    lastViewedTimestamp = lastViewedTimestamp,
    channelId = channelId
)

// ==================== Movie Mappers ====================

fun DbMovie.toMovie() = Movie(
    movieId = movieId.toInt(),
    name = name,
    streamId = streamId,
    streamIcon = streamIcon,
    rating = rating,
    added = added,
    categoryId = categoryCreatorId,
    containerExtension = containerExtension,
    isFavorite = isFavorite,
    lastViewedTimestamp = lastViewedTimestamp
)

// ==================== TvShow Mappers ====================

fun DbTvShow.toTvShow() = TvShow(
    num = num,
    name = name,
    seriesId = series_id,
    cover = cover,
    plot = plot,
    cast = cast_,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path,
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id,
    isFavorite = isFavorite,
    lastViewedTimestamp = lastViewedTimestamp
)

// ==================== Category Mappers ====================

fun DbCategory.toCategory() = Category(
    categoryId = categoryId,
    categoryName = categoryName
)

fun DbCategoryVod.toCategory() = Category(
    categoryId = categoryId,
    categoryName = categoryName
)

fun DbCategoryTvShow.toCategory() = Category(
    categoryId = categoryId,
    categoryName = categoryName
)

// ==================== ParentalControl Mappers ====================

fun DbParentalControl.toParentalControl() = ParentalControl(
    id = id.toInt(),
    categoryId = categoryId,
    categoryName = categoryName,
    userId = userId,
    isProtected = isProtected,
    createdAt = createdAt
)

// ==================== SeriesInfo Mappers ====================

fun DbSeriesInfo.toSeriesInfo() = SeriesInfo(
    name = name,
    cover = cover,
    plot = plot,
    cast = cast_,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path?.split(",")?.map { it.trim() } ?: emptyList(),
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id
)
