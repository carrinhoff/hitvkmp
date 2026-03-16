package pt.hitv.core.testing

import kotlinx.datetime.Clock
import pt.hitv.core.model.Category
import pt.hitv.core.model.Channel
import pt.hitv.core.model.ChannelEpgInfo
import pt.hitv.core.model.LoginResponse
import pt.hitv.core.model.Movie
import pt.hitv.core.model.ServerInfo
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.UserCredentials
import pt.hitv.core.model.UserInfo

/**
 * Factory object for creating test data models.
 *
 * Provides convenient factory methods to create model objects for unit tests
 * with sensible defaults while allowing customization of specific fields.
 *
 * Usage:
 * ```
 * val movie = TestDataFactory.createMovie(name = "Test Movie")
 * val channel = TestDataFactory.createChannel(id = "123", name = "ESPN")
 * val categories = TestDataFactory.createCategories(5) // Creates 5 categories
 * ```
 */
object TestDataFactory {

    // ==================== Category ====================

    fun createCategory(
        id: Int = 1,
        name: String = "Category $id"
    ): Category = Category(
        categoryId = id,
        categoryName = name
    )

    fun createCategories(
        count: Int,
        namePrefix: String = "Category"
    ): List<Category> = (1..count).map { id ->
        createCategory(id = id, name = "$namePrefix $id")
    }

    // ==================== Channel ====================

    fun createChannel(
        id: String = "1",
        name: String = "Channel $id",
        streamIcon: String = "http://icon.example.com/$id.png",
        streamUrl: String = "http://stream.example.com/$id",
        categoryId: String = "1",
        isFavorite: Boolean = false,
        epgChannelId: String = "epg_$id",
        lastViewedTimestamp: Long = 0L,
        licenseKey: String? = null,
        userId: Int = 1
    ): Channel = Channel(
        name = name,
        streamIcon = streamIcon,
        streamUrl = streamUrl,
        categoryId = categoryId,
        isFavorite = isFavorite,
        epgChannelId = epgChannelId,
        lastViewedTimestamp = lastViewedTimestamp,
        id = id,
        licenseKey = licenseKey,
        userId = userId
    )

    fun createChannels(
        count: Int,
        categoryId: String = "1",
        namePrefix: String = "Channel"
    ): List<Channel> = (1..count).map { id ->
        createChannel(
            id = id.toString(),
            name = "$namePrefix $id",
            categoryId = categoryId
        )
    }

    // ==================== Movie ====================

    fun createMovie(
        movieId: Long = 1L,
        name: String = "Movie $movieId",
        streamId: String = movieId.toString(),
        streamIcon: String? = "http://poster.example.com/$movieId.jpg",
        rating: String? = "7.5",
        added: String = "2023-01-01",
        categoryId: String? = "1",
        containerExtension: String = "mp4",
        isFavorite: Boolean = false,
        customSid: String? = null,
        directSource: String? = null,
        num: String? = null,
        streamType: String? = "movie",
        rating5based: Double? = 3.75,
        lastViewedTimestamp: Long = 0L
    ): Movie = Movie(
        movieId = movieId,
        name = name,
        streamId = streamId,
        streamIcon = streamIcon,
        rating = rating,
        added = added,
        categoryId = categoryId,
        containerExtension = containerExtension,
        isFavorite = isFavorite,
        customSid = customSid,
        directSource = directSource,
        num = num,
        streamType = streamType,
        rating5based = rating5based,
        lastViewedTimestamp = lastViewedTimestamp
    )

    fun createMovies(
        count: Int,
        categoryId: String = "1",
        namePrefix: String = "Movie"
    ): List<Movie> = (1..count).map { id ->
        createMovie(
            movieId = id.toLong(),
            name = "$namePrefix $id",
            categoryId = categoryId
        )
    }

    // ==================== TvShow ====================

    fun createTvShow(
        seriesId: Int = 1,
        name: String = "TV Show $seriesId",
        cover: String? = "http://cover.example.com/$seriesId.jpg",
        plot: String? = "A great TV show about testing.",
        cast: String? = "Test Actor, Mock Actress",
        director: String? = "Test Director",
        genre: String? = "Drama",
        releaseDate: String? = "2023",
        lastModified: String? = null,
        rating: String? = "8.5",
        rating5based: Double? = 4.25,
        backdropPath: List<String>? = null,
        youtubeTrailer: String? = null,
        episodeRunTime: String? = "45",
        categoryId: String? = "1",
        isFavorite: Boolean = false,
        lastViewedTimestamp: Long = 0L,
        num: Int? = 1
    ): TvShow = TvShow(
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
        backdropPath = backdropPath,
        youtubeTrailer = youtubeTrailer,
        episodeRunTime = episodeRunTime,
        categoryId = categoryId,
        isFavorite = isFavorite,
        lastViewedTimestamp = lastViewedTimestamp
    )

    fun createTvShows(
        count: Int,
        categoryId: String = "1",
        namePrefix: String = "TV Show"
    ): List<TvShow> = (1..count).map { id ->
        createTvShow(
            seriesId = id,
            name = "$namePrefix $id",
            categoryId = categoryId,
            num = id
        )
    }

    // ==================== UserCredentials ====================

    fun createUserCredentials(
        userId: Int = 1,
        username: String = "testuser",
        password: String = "testpass",
        hostname: String = "http://test.server.com:8080/",
        expirationDate: String? = "1234567890",
        epgUrl: String? = null,
        allowedOutputFormats: List<String>? = listOf("m3u8", "ts"),
        channelPreviewEnabled: Boolean = true
    ): UserCredentials = UserCredentials(
        userId = userId,
        username = username,
        password = password,
        hostname = hostname,
        expirationDate = expirationDate,
        epgUrl = epgUrl,
        allowedOutputFormats = allowedOutputFormats,
        channelPreviewEnabled = channelPreviewEnabled
    )

    // ==================== LoginResponse ====================

    fun createLoginResponse(
        username: String = "testuser",
        password: String = "testpass",
        status: String = "Active",
        expDate: String? = "1234567890",
        maxConnections: Int = 2,
        serverUrl: String = "test.server.com",
        port: String? = "8080",
        protocol: String = "http"
    ): LoginResponse {
        val userInfo = UserInfo(
            username = username,
            password = password,
            message = "OK",
            auth = 1,
            status = status,
            expDate = expDate,
            isTrial = 0,
            activeCons = 1,
            createdAt = 1000000,
            maxConnections = maxConnections,
            allowedOutputFormats = listOf("m3u8", "ts")
        )
        val serverInfo = ServerInfo(
            url = serverUrl,
            port = port,
            httpsPort = null,
            serverProtocol = protocol,
            rtmpPort = null,
            timezone = "UTC",
            timestampNow = null,
            timeNow = null
        )
        return LoginResponse(
            userInfo = userInfo,
            serverInfo = serverInfo
        )
    }

    // ==================== ChannelEpgInfo ====================

    fun createChannelEpgInfo(
        channelId: String? = "epg_1",
        channelName: String? = "Test Channel",
        programmeTitle: String? = "Test Programme",
        programmeDescription: String? = "A great test programme.",
        startTime: Long? = Clock.System.now().toEpochMilliseconds() - 1800000,
        endTime: Long? = Clock.System.now().toEpochMilliseconds() + 1800000,
        logo: String? = "http://logo.example.com/channel.png"
    ): ChannelEpgInfo = ChannelEpgInfo(
        channelId = channelId,
        channelName = channelName,
        programmeTitle = programmeTitle,
        programmeDescription = programmeDescription,
        startTime = startTime,
        endTime = endTime,
        logo = logo
    )
}
