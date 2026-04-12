package pt.hitv.core.database.database

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Long
import kotlin.Unit
import kotlin.reflect.KClass
import pt.hitv.core.database.CategoryQueries
import pt.hitv.core.database.CategoryTvShowQueries
import pt.hitv.core.database.CategoryVodQueries
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.database.CustomGroupQueries
import pt.hitv.core.database.EpgChannelQueries
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.database.MovieInfoQueries
import pt.hitv.core.database.MovieQueries
import pt.hitv.core.database.ParentalControlQueries
import pt.hitv.core.database.ProgrammeQueries
import pt.hitv.core.database.SeriesInfoQueries
import pt.hitv.core.database.TvShowQueries
import pt.hitv.core.database.UserCredentialsQueries

internal val KClass<HitvDatabase>.schema: SqlSchema<QueryResult.Value<Unit>>
  get() = HitvDatabaseImpl.Schema

internal fun KClass<HitvDatabase>.newInstance(driver: SqlDriver): HitvDatabase =
    HitvDatabaseImpl(driver)

private class HitvDatabaseImpl(
  driver: SqlDriver,
) : TransacterImpl(driver), HitvDatabase {
  override val categoryQueries: CategoryQueries = CategoryQueries(driver)

  override val categoryTvShowQueries: CategoryTvShowQueries = CategoryTvShowQueries(driver)

  override val categoryVodQueries: CategoryVodQueries = CategoryVodQueries(driver)

  override val channelQueries: ChannelQueries = ChannelQueries(driver)

  override val customGroupQueries: CustomGroupQueries = CustomGroupQueries(driver)

  override val epgChannelQueries: EpgChannelQueries = EpgChannelQueries(driver)

  override val movieQueries: MovieQueries = MovieQueries(driver)

  override val movieInfoQueries: MovieInfoQueries = MovieInfoQueries(driver)

  override val parentalControlQueries: ParentalControlQueries = ParentalControlQueries(driver)

  override val programmeQueries: ProgrammeQueries = ProgrammeQueries(driver)

  override val seriesInfoQueries: SeriesInfoQueries = SeriesInfoQueries(driver)

  override val tvShowQueries: TvShowQueries = TvShowQueries(driver)

  override val userCredentialsQueries: UserCredentialsQueries = UserCredentialsQueries(driver)

  public object Schema : SqlSchema<QueryResult.Value<Unit>> {
    override val version: Long
      get() = 1

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
      driver.execute(null, """
          |CREATE TABLE Category (
          |    categoryLocalId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    categoryId INTEGER NOT NULL,
          |    categoryName TEXT NOT NULL,
          |    userId INTEGER NOT NULL,
          |    isPinned INTEGER NOT NULL DEFAULT 0,
          |    isHidden INTEGER NOT NULL DEFAULT 0,
          |    isDefault INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE CategoryTvShow (
          |    categoryTvShowLocalId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    categoryId INTEGER NOT NULL,
          |    categoryName TEXT NOT NULL,
          |    userId INTEGER NOT NULL,
          |    isPinned INTEGER NOT NULL DEFAULT 0,
          |    isHidden INTEGER NOT NULL DEFAULT 0,
          |    isDefault INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE CategoryVod (
          |    categoryVodLocalId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    categoryId INTEGER NOT NULL,
          |    categoryName TEXT NOT NULL,
          |    userId INTEGER NOT NULL,
          |    isPinned INTEGER NOT NULL DEFAULT 0,
          |    isHidden INTEGER NOT NULL DEFAULT 0,
          |    isDefault INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Channel (
          |    channelId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    streamUrl TEXT NOT NULL,
          |    streamIcon TEXT NOT NULL,
          |    epgChannelId TEXT,
          |    categoryCreatorId TEXT NOT NULL,
          |    isFavorite INTEGER NOT NULL DEFAULT 0,
          |    licenseKey TEXT,
          |    userId INTEGER NOT NULL,
          |    lastViewedTimestamp INTEGER NOT NULL DEFAULT 0,
          |    lastUpdated INTEGER NOT NULL DEFAULT 0,
          |    lastSeen INTEGER NOT NULL DEFAULT 0,
          |    contentHash TEXT,
          |    syncVersion INTEGER NOT NULL DEFAULT 1
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE CustomGroup (
          |    groupId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    groupName TEXT NOT NULL,
          |    groupIcon TEXT,
          |    createdAt INTEGER NOT NULL DEFAULT 0,
          |    updatedAt INTEGER NOT NULL DEFAULT 0,
          |    sortOrder INTEGER NOT NULL DEFAULT 0,
          |    isPinned INTEGER NOT NULL DEFAULT 0,
          |    isHidden INTEGER NOT NULL DEFAULT 0,
          |    isDefault INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE CustomGroupChannel (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    groupId INTEGER NOT NULL REFERENCES CustomGroup(groupId) ON DELETE CASCADE,
          |    channelId INTEGER NOT NULL REFERENCES Channel(channelId) ON DELETE CASCADE,
          |    channelUserId INTEGER NOT NULL DEFAULT 0,
          |    position INTEGER NOT NULL DEFAULT 0,
          |    addedAt INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE EpgChannel (
          |    channel_id TEXT NOT NULL,
          |    display_name TEXT,
          |    logo TEXT,
          |    userId INTEGER NOT NULL,
          |    PRIMARY KEY (channel_id, userId)
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Movie (
          |    movieId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    name TEXT NOT NULL,
          |    streamId TEXT NOT NULL,
          |    streamIcon TEXT NOT NULL,
          |    rating TEXT NOT NULL,
          |    added TEXT NOT NULL,
          |    categoryCreatorId TEXT NOT NULL,
          |    containerExtension TEXT NOT NULL,
          |    isFavorite INTEGER NOT NULL DEFAULT 0,
          |    userId INTEGER NOT NULL,
          |    lastViewedTimestamp INTEGER NOT NULL DEFAULT 0,
          |    lastUpdated INTEGER NOT NULL DEFAULT 0,
          |    lastSeen INTEGER NOT NULL DEFAULT 0,
          |    contentHash TEXT,
          |    syncVersion INTEGER NOT NULL DEFAULT 1
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE MovieData (
          |    streamId INTEGER PRIMARY KEY NOT NULL,
          |    name TEXT NOT NULL,
          |    added REAL NOT NULL,
          |    category_id INTEGER NOT NULL,
          |    container_extension TEXT NOT NULL,
          |    custom_sid TEXT,
          |    direct_source TEXT,
          |    userId INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE MovieInfo (
          |    movieInfoId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    streamIdCreator INTEGER NOT NULL,
          |    kinopoisk_url TEXT,
          |    tmdb_id TEXT,
          |    name TEXT,
          |    o_name TEXT,
          |    cover_big TEXT,
          |    movie_image TEXT,
          |    releasedate TEXT,
          |    episode_run_time TEXT,
          |    youtube_trailer TEXT,
          |    director TEXT,
          |    actors TEXT,
          |    cast_ TEXT,
          |    description TEXT,
          |    plot TEXT,
          |    age TEXT,
          |    mpaa_rating TEXT,
          |    rating_count_kinopoisk TEXT,
          |    country TEXT,
          |    genre TEXT,
          |    backdrop_path TEXT,
          |    duration_secs TEXT,
          |    duration TEXT,
          |    bitrate TEXT,
          |    rating TEXT,
          |    userId INTEGER NOT NULL DEFAULT 0,
          |    playbackPosition INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE ParentalControl (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    categoryId INTEGER NOT NULL,
          |    categoryName TEXT NOT NULL,
          |    userId INTEGER NOT NULL,
          |    isProtected INTEGER NOT NULL DEFAULT 1,
          |    createdAt INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Programme (
          |    id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    channel_name TEXT,
          |    start_time INTEGER NOT NULL,
          |    end_time INTEGER NOT NULL,
          |    userId INTEGER NOT NULL,
          |    imageUrl TEXT,
          |    FOREIGN KEY (channel_name, userId) REFERENCES EpgChannel(channel_id, userId) ON DELETE CASCADE
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Title (
          |    title_id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    title TEXT,
          |    programme_id INTEGER REFERENCES Programme(id) ON DELETE CASCADE,
          |    userId INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Description (
          |    desc_id INTEGER PRIMARY KEY AUTOINCREMENT,
          |    desc TEXT,
          |    programme_id INTEGER REFERENCES Programme(id) ON DELETE CASCADE,
          |    userId INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE SeriesInfo (
          |    seriesId TEXT PRIMARY KEY NOT NULL,
          |    name TEXT,
          |    cover TEXT,
          |    plot TEXT,
          |    cast_ TEXT,
          |    director TEXT,
          |    genre TEXT,
          |    releaseDate TEXT,
          |    last_modified TEXT,
          |    rating TEXT,
          |    rating_5based REAL,
          |    backdrop_path TEXT,
          |    youtube_trailer TEXT,
          |    episode_run_time TEXT,
          |    category_id TEXT,
          |    userId INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Season (
          |    season_id TEXT PRIMARY KEY NOT NULL,
          |    air_date TEXT,
          |    episode_count INTEGER,
          |    name TEXT,
          |    overview TEXT,
          |    season_number INTEGER NOT NULL,
          |    cover TEXT,
          |    cover_big TEXT,
          |    series_id TEXT NOT NULL,
          |    userId INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE Episode (
          |    episode_id TEXT PRIMARY KEY NOT NULL,
          |    episode_num INTEGER,
          |    title TEXT,
          |    container_extension TEXT,
          |    custom_sid TEXT,
          |    added TEXT,
          |    season INTEGER,
          |    direct_source TEXT,
          |    seasonCreatorId TEXT NOT NULL,
          |    userId INTEGER NOT NULL
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE EpisodeInfo (
          |    episodeCreatorId TEXT PRIMARY KEY NOT NULL,
          |    tmdb_id REAL,
          |    release_date TEXT,
          |    plot TEXT,
          |    duration_secs REAL,
          |    duration TEXT,
          |    movie_image TEXT,
          |    bitrate REAL,
          |    rating REAL,
          |    season TEXT,
          |    userId INTEGER NOT NULL,
          |    playbackPosition INTEGER NOT NULL DEFAULT 0
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE TvShow (
          |    tvShowId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    num INTEGER,
          |    name TEXT,
          |    series_id INTEGER NOT NULL,
          |    cover TEXT,
          |    plot TEXT,
          |    cast_ TEXT,
          |    director TEXT,
          |    genre TEXT,
          |    releaseDate TEXT,
          |    last_modified TEXT,
          |    rating TEXT,
          |    rating_5based REAL,
          |    backdrop_path TEXT,
          |    youtube_trailer TEXT,
          |    episode_run_time TEXT,
          |    category_id TEXT NOT NULL,
          |    isFavorite INTEGER NOT NULL DEFAULT 0,
          |    userId INTEGER NOT NULL,
          |    lastViewedTimestamp INTEGER NOT NULL DEFAULT 0,
          |    lastUpdated INTEGER NOT NULL DEFAULT 0,
          |    lastSeen INTEGER NOT NULL DEFAULT 0,
          |    contentHash TEXT,
          |    syncVersion INTEGER NOT NULL DEFAULT 1
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE TABLE UserCredentials (
          |    userId INTEGER PRIMARY KEY AUTOINCREMENT,
          |    username TEXT NOT NULL,
          |    encryptedPassword TEXT NOT NULL,
          |    hostname TEXT NOT NULL,
          |    expirationDate TEXT,
          |    epgUrl TEXT,
          |    allowedOutputFormats TEXT,
          |    channelPreviewEnabled INTEGER NOT NULL DEFAULT 1
          |)
          """.trimMargin(), 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX category_unique ON Category(categoryId, categoryName, userId)", 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX category_tvshow_unique ON CategoryTvShow(categoryId, categoryName, userId)",
          0)
      driver.execute(null,
          "CREATE UNIQUE INDEX category_vod_unique ON CategoryVod(categoryId, categoryName, userId)",
          0)
      driver.execute(null,
          "CREATE UNIQUE INDEX channel_unique ON Channel(name, streamIcon, categoryCreatorId, userId)",
          0)
      driver.execute(null,
          "CREATE UNIQUE INDEX custom_group_channel_unique ON CustomGroupChannel(groupId, channelId)",
          0)
      driver.execute(null,
          "CREATE INDEX custom_group_channel_group_idx ON CustomGroupChannel(groupId)", 0)
      driver.execute(null,
          "CREATE INDEX custom_group_channel_channel_idx ON CustomGroupChannel(channelId)", 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX movie_stream_unique ON Movie(streamId, categoryCreatorId, userId)",
          0)
      driver.execute(null,
          "CREATE UNIQUE INDEX parental_control_unique ON ParentalControl(categoryId, userId)", 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX programme_unique ON Programme(channel_name, start_time, end_time)",
          0)
      driver.execute(null, "CREATE INDEX title_programme_idx ON Title(programme_id)", 0)
      driver.execute(null, "CREATE INDEX description_programme_idx ON Description(programme_id)", 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX tvshow_unique ON TvShow(series_id, category_id, userId)", 0)
      driver.execute(null,
          "CREATE UNIQUE INDEX credentials_unique ON UserCredentials(username, hostname)", 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE Movie_fts USING fts4(
          |    name,
          |    streamId,
          |    nameNormalized,
          |    tokenize=unicode61
          |)
          """.trimMargin(), 0)
      driver.execute(null, """
          |CREATE VIRTUAL TABLE TvShow_fts USING fts4(
          |    name,
          |    series_id,
          |    nameNormalized,
          |    tokenize=unicode61
          |)
          """.trimMargin(), 0)
      return QueryResult.Unit
    }

    override fun migrate(
      driver: SqlDriver,
      oldVersion: Long,
      newVersion: Long,
      vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> = QueryResult.Unit
  }
}
