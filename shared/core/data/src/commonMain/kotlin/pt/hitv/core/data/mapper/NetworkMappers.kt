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
    streamId = stream_id ?: "",
    streamIcon = stream_icon,
    rating = rating,
    added = added ?: "",
    categoryId = category_id,
    containerExtension = container_extension ?: "",
    customSid = custom_sid,
    directSource = direct_source,
    num = num,
    streamType = stream_type,
    rating5based = rating_5based
)

// NetworkTvShow -> TvShow
fun NetworkTvShow.asExternalModel() = TvShow(
    num = num,
    name = name,
    seriesId = series_id,
    cover = cover,
    plot = plot,
    cast = cast,
    director = director,
    genre = genre,
    releaseDate = releaseDate,
    lastModified = last_modified,
    rating = rating,
    rating5based = rating_5based,
    backdropPath = backdrop_path,
    youtubeTrailer = youtube_trailer,
    episodeRunTime = episode_run_time,
    categoryId = category_id
)

// NetworkLiveStream -> LiveStream
fun NetworkLiveStream.asExternalModel() = LiveStream(
    num = num,
    name = name ?: "",
    streamType = stream_type ?: "",
    streamId = stream_id,
    streamIcon = stream_icon ?: "",
    epgChannelId = epg_channel_id ?: "",
    added = added,
    categoryId = category_id,
    customSid = custom_sid ?: "",
    tvArchive = tv_archive,
    directSource = direct_source ?: "",
    tvArchiveDuration = tv_archive_duration
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
    expDate = exp_date,
    isTrial = is_trial?.toIntOrNull() ?: 0,
    activeCons = active_cons?.toIntOrNull() ?: 0,
    createdAt = created_at?.toIntOrNull() ?: 0,
    maxConnections = max_connections?.toIntOrNull() ?: 0,
    allowedOutputFormats = allowed_output_formats ?: emptyList()
)

// NetworkServerInfo -> ServerInfo
fun NetworkServerInfo.asExternalModel() = ServerInfo(
    url = url,
    port = port,
    httpsPort = https_port,
    serverProtocol = server_protocol,
    rtmpPort = rtmp_port,
    timezone = timezone,
    timestampNow = timestamp_now,
    timeNow = time_now
)

// NetworkMovieInfoResponse -> MovieInfoResponse
fun NetworkMovieInfoResponse.asExternalModel() = MovieInfoResponse(
    info = info?.asExternalModel() ?: Info(durationSecs = null),
    movieData = movie_data?.asExternalModel()
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
    categoryId = category_id,
    containerExtension = container_extension ?: "",
    customSid = custom_sid,
    directSource = direct_source
)

// NetworkInfo -> Info
fun NetworkInfo.asExternalModel() = Info(
    kinopoiskUrl = kinopoisk_url,
    tmdbId = tmdb_id,
    name = name,
    oName = o_name,
    coverBig = cover_big,
    movieImage = movie_image,
    releasedate = releasedate,
    episodeRunTime = episode_run_time,
    youtubeTrailer = youtube_trailer,
    director = director,
    actors = actors,
    cast = cast,
    description = description,
    plot = plot,
    age = age,
    mpaaRating = mpaa_rating,
    ratingCountKinopoisk = rating_count_kinopoisk,
    country = country,
    genre = genre,
    backdropPath = backdrop_path?.toList(),
    durationSecs = duration_secs,
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
    tmdbId = tmdb_id,
    releasedate = releasedate,
    plot = plot,
    durationSecs = duration_secs,
    duration = duration,
    movieImage = movie_image,
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
    knownForDepartment = known_for_department ?: "",
    name = name ?: "",
    originalName = original_name ?: "",
    popularity = popularity,
    profilePath = profile_path ?: "",
    castId = cast_id,
    character = character ?: "",
    creditId = credit_id ?: "",
    order = order
)

// NetworkCrew -> Crew
fun NetworkCrew.asExternalModel() = Crew(
    adult = adult,
    gender = gender,
    id = id,
    knownForDepartment = known_for_department ?: "",
    name = name ?: "",
    originalName = original_name ?: "",
    popularity = popularity,
    profilePath = profile_path ?: "",
    creditId = credit_id ?: "",
    department = department ?: "",
    job = job ?: ""
)
