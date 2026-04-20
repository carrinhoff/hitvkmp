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
import pt.hitv.core.database.tvShow.SelectRecentlyViewed
import pt.hitv.core.database.tvShow.SelectRecentlyViewedPaged
import pt.hitv.core.database.channel.SelectRecentlyViewed as ChannelSelectRecentlyViewed
import pt.hitv.core.database.channel.SelectRecentlyViewedPaged as ChannelSelectRecentlyViewedPaged
import pt.hitv.core.database.movie.SelectRecentlyViewed as MovieSelectRecentlyViewed
import pt.hitv.core.database.movie.SelectRecentlyViewedPaged as MovieSelectRecentlyViewedPaged
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
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp,
    id = channelId.toString(),
    tvArchive = tvArchive.toInt(),
    tvArchiveDuration = tvArchiveDuration.toInt(),
    catchupType = catchupType,
    catchupSource = catchupSource,
)

fun ChannelSelectRecentlyViewed.toChannel() = Channel(
    name = name,
    streamIcon = streamIcon,
    streamUrl = streamUrl,
    epgChannelId = epgChannelId,
    categoryId = categoryCreatorId,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp,
    id = channelId.toString(),
    tvArchive = tvArchive.toInt(),
    tvArchiveDuration = tvArchiveDuration.toInt(),
    catchupType = catchupType,
    catchupSource = catchupSource,
)

fun ChannelSelectRecentlyViewedPaged.toChannel() = Channel(
    name = name,
    streamIcon = streamIcon,
    streamUrl = streamUrl,
    epgChannelId = epgChannelId,
    categoryId = categoryCreatorId,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp,
    id = channelId.toString(),
    tvArchive = tvArchive.toInt(),
    tvArchiveDuration = tvArchiveDuration.toInt(),
    catchupType = catchupType,
    catchupSource = catchupSource,
)

// ==================== Movie Mappers ====================

fun DbMovie.toMovie() = Movie(
    movieId = movieId,
    name = name,
    streamId = streamId,
    streamIcon = streamIcon,
    rating = rating,
    added = added,
    categoryId = categoryCreatorId,
    containerExtension = containerExtension,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

fun MovieSelectRecentlyViewed.toMovie() = Movie(
    movieId = movieId,
    name = name,
    streamId = streamId,
    streamIcon = streamIcon,
    rating = rating,
    added = added,
    categoryId = categoryCreatorId,
    containerExtension = containerExtension,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

fun MovieSelectRecentlyViewedPaged.toMovie() = Movie(
    movieId = movieId,
    name = name,
    streamId = streamId,
    streamIcon = streamIcon,
    rating = rating,
    added = added,
    categoryId = categoryCreatorId,
    containerExtension = containerExtension,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

// ==================== TvShow Mappers ====================

fun DbTvShow.toTvShow() = TvShow(
    num = num?.toInt(),
    name = name,
    seriesId = series_id.toInt(),
    cover = cover,
    plot = plot,
    cast = cast_,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path?.split(",")?.map { it.trim() },
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

fun SelectRecentlyViewed.toTvShow() = TvShow(
    num = num?.toInt(),
    name = name,
    seriesId = series_id.toInt(),
    cover = cover,
    plot = plot,
    cast = cast_,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path?.split(",")?.map { it.trim() },
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

fun SelectRecentlyViewedPaged.toTvShow() = TvShow(
    num = num?.toInt(),
    name = name,
    seriesId = series_id.toInt(),
    cover = cover,
    plot = plot,
    cast = cast_,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path?.split(",")?.map { it.trim() },
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id,
    isFavorite = isFavorite != 0L,
    lastViewedTimestamp = lastViewedTimestamp
)

// ==================== Category Mappers ====================

fun DbCategory.toCategory() = Category(
    categoryId = categoryId.toInt(),
    categoryName = categoryName
)

fun DbCategoryVod.toCategory() = Category(
    categoryId = categoryId.toInt(),
    categoryName = categoryName
)

fun DbCategoryTvShow.toCategory() = Category(
    categoryId = categoryId.toInt(),
    categoryName = categoryName
)

// ==================== ParentalControl Mappers ====================

fun DbParentalControl.toParentalControl() = ParentalControl(
    id = id,
    categoryId = categoryId.toInt(),
    categoryName = categoryName,
    userId = userId.toInt(),
    isProtected = isProtected != 0L,
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
