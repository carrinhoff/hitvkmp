package pt.hitv.core.data.mapper

import pt.hitv.core.model.Category
import pt.hitv.core.model.LiveStream
import pt.hitv.core.model.LoginResponse
import pt.hitv.core.model.Movie
import pt.hitv.core.model.ServerInfo
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.UserInfo
import pt.hitv.core.model.cast.Cast
import pt.hitv.core.model.cast.CastResponse
import pt.hitv.core.model.cast.Crew
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import pt.hitv.core.model.movieInfo.MovieInfoResponse
import pt.hitv.core.model.seriesInfo.Episode
import pt.hitv.core.model.seriesInfo.EpisodeInfo
import pt.hitv.core.model.seriesInfo.Season
import pt.hitv.core.model.seriesInfo.SeriesInfo
import pt.hitv.core.model.seriesInfo.SeriesInfoResponse
import pt.hitv.core.network.model.NetworkCategory
import pt.hitv.core.network.model.NetworkLiveStream
import pt.hitv.core.network.model.NetworkLoginResponse
import pt.hitv.core.network.model.NetworkMovie
import pt.hitv.core.network.model.NetworkServerInfo
import pt.hitv.core.network.model.NetworkTvShow
import pt.hitv.core.network.model.NetworkUserInfo
import pt.hitv.core.network.model.cast.NetworkCast
import pt.hitv.core.network.model.cast.NetworkCastResponse
import pt.hitv.core.network.model.cast.NetworkCrew
import pt.hitv.core.network.model.movieInfo.NetworkInfo
import pt.hitv.core.network.model.movieInfo.NetworkMovieData
import pt.hitv.core.network.model.movieInfo.NetworkMovieInfoResponse
import pt.hitv.core.network.model.seriesInfo.NetworkEpisode
import pt.hitv.core.network.model.seriesInfo.NetworkEpisodeInfo
import pt.hitv.core.network.model.seriesInfo.NetworkSeason
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfo
import pt.hitv.core.network.model.seriesInfo.NetworkSeriesInfoResponse

// Network -> Domain mapper functions

// NetworkMovie -> Movie
fun NetworkMovie.asExternalModel() = Movie(
    movieId = movieId,
    name = name ?: "",
    streamId = streamId ?: "",
    streamIcon = streamIcon,
    rating = rating,
    added = added ?: "",
    categoryId = categoryId,
    containerExtension = containerExtension ?: "",
    customSid = customSid,
    directSource = directSource,
    num = num,
    streamType = streamType,
    rating5based = rating5based
)

// NetworkTvShow -> TvShow
fun NetworkTvShow.asExternalModel() = TvShow(
    num = num,
    name = name,
    seriesId = seriesId,
    cover = cover,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = lastModified,
    rating = rating,
    rating5based = rating5based,
    backdropPath = backdropPath?.filterNotNull(),
    youtubeTrailer = youtubeTrailer,
    episodeRunTime = episodeRunTime,
    categoryId = categoryId
)

// NetworkLiveStream -> LiveStream
fun NetworkLiveStream.asExternalModel() = LiveStream(
    num = num,
    name = name ?: "",
    streamType = streamType ?: "",
    streamId = streamId,
    streamIcon = streamIcon ?: "",
    epgChannelId = epgChannelId ?: "",
    added = added,
    categoryId = categoryId,
    customSid = customSid ?: "",
    tvArchive = tvArchive,
    directSource = directSource ?: "",
    tvArchiveDuration = tvArchiveDuration
)

// NetworkCategory -> Category
fun NetworkCategory.asExternalModel() = Category(
    categoryId = categoryId,
    categoryName = categoryName ?: ""
)

// NetworkLoginResponse -> LoginResponse
fun NetworkLoginResponse.asExternalModel() = LoginResponse(
    userInfo = userInfo?.asExternalModel(),
    serverInfo = serverInfo?.asExternalModel()
        ?: ServerInfo(null, null, null, null, null, null, null, null)
)

// NetworkUserInfo -> UserInfo
fun NetworkUserInfo.asExternalModel() = UserInfo(
    username = username ?: "",
    password = password ?: "",
    message = message ?: "",
    auth = auth?.toIntOrNull() ?: 0,
    status = status ?: "",
    expDate = expDate,
    isTrial = isTrial?.toIntOrNull() ?: 0,
    activeCons = activeCons?.toIntOrNull() ?: 0,
    createdAt = createdAt?.toIntOrNull() ?: 0,
    maxConnections = maxConnections?.toIntOrNull() ?: 0,
    allowedOutputFormats = allowedOutputFormats ?: emptyList()
)

// NetworkServerInfo -> ServerInfo
fun NetworkServerInfo.asExternalModel() = ServerInfo(
    url = url,
    port = port,
    httpsPort = httpsPort,
    serverProtocol = serverProtocol,
    rtmpPort = rtmpPort,
    timezone = timezone,
    timestampNow = timestampNow,
    timeNow = timeNow
)

// NetworkMovieInfoResponse -> MovieInfoResponse
fun NetworkMovieInfoResponse.asExternalModel() = MovieInfoResponse(
    info = info?.asExternalModel() ?: Info(durationSecs = null),
    movieData = movieData?.asExternalModel()
        ?: MovieData(
            streamId = 0,
            name = "",
            added = 0.0,
            categoryId = 0,
            containerExtension = "",
            customSid = null,
            directSource = null
        )
)

// NetworkMovieData -> MovieData
fun NetworkMovieData.asExternalModel() = MovieData(
    streamId = streamId,
    name = name ?: "",
    added = added,
    categoryId = categoryId,
    containerExtension = containerExtension ?: "",
    customSid = customSid,
    directSource = directSource
)

// NetworkInfo -> Info
fun NetworkInfo.asExternalModel() = Info(
    kinopoiskUrl = kinopoiskUrl,
    tmdbId = tmdbId,
    name = name,
    oName = oName,
    coverBig = coverBig,
    movieImage = movieImage,
    releasedate = releasedate,
    episodeRunTime = episodeRunTime,
    youtubeTrailer = youtubeTrailer,
    director = director,
    actors = actors,
    cast = cast,
    description = description,
    plot = plot,
    age = age,
    mpaaRating = mpaaRating,
    ratingCountKinopoisk = ratingCountKinopoisk,
    country = country,
    genre = genre,
    backdropPath = backdropPath,
    durationSecs = durationSecs,
    duration = duration,
    bitrate = bitrate,
    rating = rating
)

// NetworkSeriesInfoResponse -> SeriesInfoResponse
fun NetworkSeriesInfoResponse.asExternalModel() = SeriesInfoResponse(
    seasons = seasons?.map { it.asExternalModel() },
    info = info?.asExternalModel(),
    episodes = episodes?.mapValues { (_, episodes) -> episodes.map { it.asExternalModel() } }
)

// NetworkSeriesInfo -> SeriesInfo
fun NetworkSeriesInfo.asExternalModel() = SeriesInfo(
    name = name,
    cover = cover,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = lastModified,
    rating = rating,
    rating5based = rating5based,
    backdropPath = backdropPath?.toList() ?: emptyList(),
    youtubeTrailer = youtubeTrailer,
    episodeRunTime = episodeRunTime,
    categoryId = categoryId
)

// NetworkSeason -> Season
fun NetworkSeason.asExternalModel() = Season(
    airDate = airDate,
    episodeCount = episodeCount,
    id = id ?: "",
    name = name,
    overview = overview,
    seasonNumber = seasonNumber,
    cover = cover,
    coverBig = coverBig
)

// NetworkEpisode -> Episode
fun NetworkEpisode.asExternalModel() = Episode(
    id = id ?: "",
    episodeNum = episodeNum,
    title = title,
    containerExtension = containerExtension,
    info = info?.asExternalModel() ?: EpisodeInfo(tmdbId = null),
    customSid = customSid,
    added = added,
    season = season,
    directSource = directSource
)

// NetworkEpisodeInfo -> EpisodeInfo
fun NetworkEpisodeInfo.asExternalModel() = EpisodeInfo(
    tmdbId = tmdbId,
    releasedate = releasedate,
    plot = plot,
    durationSecs = durationSecs,
    duration = duration,
    movieImage = movieImage,
    bitrate = bitrate,
    rating = rating,
    season = season
)

// NetworkCastResponse -> CastResponse
fun NetworkCastResponse.asExternalModel() = CastResponse(
    id = id,
    cast = cast?.map { it.asExternalModel() } ?: emptyList(),
    crew = crew?.map { it.asExternalModel() } ?: emptyList()
)

// NetworkCast -> Cast
fun NetworkCast.asExternalModel() = Cast(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = knownForDepartment ?: "",
    name = name ?: "",
    originalName = originalName ?: "",
    popularity = popularity,
    profilePath = profilePath ?: "",
    castId = castId,
    character = character ?: "",
    creditId = creditId ?: "",
    order = order
)

// NetworkCrew -> Crew
fun NetworkCrew.asExternalModel() = Crew(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = knownForDepartment ?: "",
    name = name ?: "",
    originalName = originalName ?: "",
    popularity = popularity,
    profilePath = profilePath ?: "",
    creditId = creditId ?: "",
    department = department ?: "",
    job = job ?: ""
)
