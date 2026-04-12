package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Double
import kotlin.Long
import kotlin.String

public class SeriesInfoQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectSeriesInfo(
    seriesId: String,
    userId: Long,
    mapper: (
      seriesId: String,
      name: String?,
      cover: String?,
      plot: String?,
      cast_: String?,
      director: String?,
      genre: String?,
      releaseDate: String?,
      last_modified: String?,
      rating: String?,
      rating_5based: Double?,
      backdrop_path: String?,
      youtube_trailer: String?,
      episode_run_time: String?,
      category_id: String?,
      userId: Long,
    ) -> T,
  ): Query<T> = SelectSeriesInfoQuery(seriesId, userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getDouble(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getLong(15)!!
    )
  }

  public fun selectSeriesInfo(seriesId: String, userId: Long): Query<SeriesInfo> =
      selectSeriesInfo(seriesId, userId) { seriesId_, name, cover, plot, cast_, director, genre,
      releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer,
      episode_run_time, category_id, userId_ ->
    SeriesInfo(
      seriesId_,
      name,
      cover,
      plot,
      cast_,
      director,
      genre,
      releaseDate,
      last_modified,
      rating,
      rating_5based,
      backdrop_path,
      youtube_trailer,
      episode_run_time,
      category_id,
      userId_
    )
  }

  public fun <T : Any> selectSeasonsWithEpisodes(
    series_id: String,
    userId: Long,
    mapper: (
      seasonId: String,
      airDate: String?,
      episodeCount: Long?,
      name: String?,
      overview: String?,
      cover: String?,
      coverBig: String?,
      seriesId: String,
      episodeId: String,
      episodeNum: Long?,
      containerExtension: String?,
      added: String?,
      season: Long?,
      title: String?,
      tmdbId: Double?,
      releaseDate: String?,
      plot: String?,
      durationSecs: Double?,
      duration: String?,
      bitrate: Double?,
      rating: Double?,
      movieImage: String?,
      seasonNumber: Long,
      playbackPosition: Long,
    ) -> T,
  ): Query<T> = SelectSeasonsWithEpisodesQuery(series_id, userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getLong(2),
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7)!!,
      cursor.getString(8)!!,
      cursor.getLong(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getLong(12),
      cursor.getString(13),
      cursor.getDouble(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getDouble(17),
      cursor.getString(18),
      cursor.getDouble(19),
      cursor.getDouble(20),
      cursor.getString(21),
      cursor.getLong(22)!!,
      cursor.getLong(23)!!
    )
  }

  public fun selectSeasonsWithEpisodes(series_id: String, userId: Long):
      Query<SelectSeasonsWithEpisodes> = selectSeasonsWithEpisodes(series_id, userId) { seasonId,
      airDate, episodeCount, name, overview, cover, coverBig, seriesId, episodeId, episodeNum,
      containerExtension, added, season, title, tmdbId, releaseDate, plot, durationSecs, duration,
      bitrate, rating, movieImage, seasonNumber, playbackPosition ->
    SelectSeasonsWithEpisodes(
      seasonId,
      airDate,
      episodeCount,
      name,
      overview,
      cover,
      coverBig,
      seriesId,
      episodeId,
      episodeNum,
      containerExtension,
      added,
      season,
      title,
      tmdbId,
      releaseDate,
      plot,
      durationSecs,
      duration,
      bitrate,
      rating,
      movieImage,
      seasonNumber,
      playbackPosition
    )
  }

  public fun insertSeriesInfo(
    seriesId: String,
    name: String?,
    cover: String?,
    plot: String?,
    cast_: String?,
    director: String?,
    genre: String?,
    releaseDate: String?,
    last_modified: String?,
    rating: String?,
    rating_5based: Double?,
    backdrop_path: String?,
    youtube_trailer: String?,
    episode_run_time: String?,
    category_id: String?,
    userId: Long,
  ) {
    driver.execute(1_578_517_346, """
        |INSERT OR IGNORE INTO SeriesInfo(seriesId, name, cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, userId)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 16) {
          bindString(0, seriesId)
          bindString(1, name)
          bindString(2, cover)
          bindString(3, plot)
          bindString(4, cast_)
          bindString(5, director)
          bindString(6, genre)
          bindString(7, releaseDate)
          bindString(8, last_modified)
          bindString(9, rating)
          bindDouble(10, rating_5based)
          bindString(11, backdrop_path)
          bindString(12, youtube_trailer)
          bindString(13, episode_run_time)
          bindString(14, category_id)
          bindLong(15, userId)
        }
    notifyQueries(1_578_517_346) { emit ->
      emit("SeriesInfo")
    }
  }

  public fun insertSeason(
    season_id: String,
    air_date: String?,
    episode_count: Long?,
    name: String?,
    overview: String?,
    season_number: Long,
    cover: String?,
    cover_big: String?,
    series_id: String,
    userId: Long,
  ) {
    driver.execute(1_842_364_672, """
        |INSERT OR IGNORE INTO Season(season_id, air_date, episode_count, name, overview, season_number, cover, cover_big, series_id, userId)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, season_id)
          bindString(1, air_date)
          bindLong(2, episode_count)
          bindString(3, name)
          bindString(4, overview)
          bindLong(5, season_number)
          bindString(6, cover)
          bindString(7, cover_big)
          bindString(8, series_id)
          bindLong(9, userId)
        }
    notifyQueries(1_842_364_672) { emit ->
      emit("Season")
    }
  }

  public fun insertEpisode(
    episode_id: String,
    episode_num: Long?,
    title: String?,
    container_extension: String?,
    custom_sid: String?,
    added: String?,
    season: Long?,
    direct_source: String?,
    seasonCreatorId: String,
    userId: Long,
  ) {
    driver.execute(2_060_888_958, """
        |INSERT OR IGNORE INTO Episode(episode_id, episode_num, title, container_extension, custom_sid, added, season, direct_source, seasonCreatorId, userId)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 10) {
          bindString(0, episode_id)
          bindLong(1, episode_num)
          bindString(2, title)
          bindString(3, container_extension)
          bindString(4, custom_sid)
          bindString(5, added)
          bindLong(6, season)
          bindString(7, direct_source)
          bindString(8, seasonCreatorId)
          bindLong(9, userId)
        }
    notifyQueries(2_060_888_958) { emit ->
      emit("Episode")
    }
  }

  public fun insertEpisodeInfo(
    episodeCreatorId: String,
    tmdb_id: Double?,
    release_date: String?,
    plot: String?,
    duration_secs: Double?,
    duration: String?,
    movie_image: String?,
    bitrate: Double?,
    rating: Double?,
    season: String?,
    userId: Long,
    playbackPosition: Long,
  ) {
    driver.execute(-1_868_851_892, """
        |INSERT OR IGNORE INTO EpisodeInfo(episodeCreatorId, tmdb_id, release_date, plot, duration_secs, duration, movie_image, bitrate, rating, season, userId, playbackPosition)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 12) {
          bindString(0, episodeCreatorId)
          bindDouble(1, tmdb_id)
          bindString(2, release_date)
          bindString(3, plot)
          bindDouble(4, duration_secs)
          bindString(5, duration)
          bindString(6, movie_image)
          bindDouble(7, bitrate)
          bindDouble(8, rating)
          bindString(9, season)
          bindLong(10, userId)
          bindLong(11, playbackPosition)
        }
    notifyQueries(-1_868_851_892) { emit ->
      emit("EpisodeInfo")
    }
  }

  public fun updateEpisodePlaybackPosition(
    playbackPosition: Long,
    episodeCreatorId: String,
    userId: Long,
  ) {
    driver.execute(-1_586_138_126,
        """UPDATE EpisodeInfo SET playbackPosition = ? WHERE episodeCreatorId = ? AND userId = ?""",
        3) {
          bindLong(0, playbackPosition)
          bindString(1, episodeCreatorId)
          bindLong(2, userId)
        }
    notifyQueries(-1_586_138_126) { emit ->
      emit("EpisodeInfo")
    }
  }

  public fun updateEpisodeDuration(
    duration_secs: Double?,
    episodeCreatorId: String,
    userId: Long,
  ) {
    driver.execute(851_224_354,
        """UPDATE EpisodeInfo SET duration_secs = ? WHERE episodeCreatorId = ? AND userId = ?""", 3)
        {
          bindDouble(0, duration_secs)
          bindString(1, episodeCreatorId)
          bindLong(2, userId)
        }
    notifyQueries(851_224_354) { emit ->
      emit("EpisodeInfo")
    }
  }

  public fun deleteSeriesInfoByUserId(userId: Long) {
    driver.execute(-2_137_056_911, """DELETE FROM SeriesInfo WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-2_137_056_911) { emit ->
      emit("SeriesInfo")
    }
  }

  public fun deleteSeasonsByUserId(userId: Long) {
    driver.execute(596_124_894, """DELETE FROM Season WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(596_124_894) { emit ->
      emit("Season")
    }
  }

  public fun deleteEpisodesByUserId(userId: Long) {
    driver.execute(59_935_812, """DELETE FROM Episode WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(59_935_812) { emit ->
      emit("Episode")
    }
  }

  public fun deleteEpisodesInfoByUserId(userId: Long) {
    driver.execute(-1_358_885_998, """DELETE FROM EpisodeInfo WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-1_358_885_998) { emit ->
      emit("EpisodeInfo")
    }
  }

  private inner class SelectSeriesInfoQuery<out T : Any>(
    public val seriesId: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("SeriesInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("SeriesInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_687_748_069,
        """SELECT SeriesInfo.seriesId, SeriesInfo.name, SeriesInfo.cover, SeriesInfo.plot, SeriesInfo.cast_, SeriesInfo.director, SeriesInfo.genre, SeriesInfo.releaseDate, SeriesInfo.last_modified, SeriesInfo.rating, SeriesInfo.rating_5based, SeriesInfo.backdrop_path, SeriesInfo.youtube_trailer, SeriesInfo.episode_run_time, SeriesInfo.category_id, SeriesInfo.userId FROM SeriesInfo WHERE seriesId = ? AND userId = ?""",
        mapper, 2) {
      bindString(0, seriesId)
      bindLong(1, userId)
    }

    override fun toString(): String = "SeriesInfo.sq:selectSeriesInfo"
  }

  private inner class SelectSeasonsWithEpisodesQuery<out T : Any>(
    public val series_id: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Season", "Episode", "EpisodeInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Season", "Episode", "EpisodeInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-48_105_298, """
    |SELECT
    |    season.season_id AS seasonId,
    |    season.air_date AS airDate,
    |    season.episode_count AS episodeCount,
    |    season.name,
    |    season.overview,
    |    season.cover,
    |    season.cover_big AS coverBig,
    |    season.series_id AS seriesId,
    |    episode.episode_id AS episodeId,
    |    episode.episode_num AS episodeNum,
    |    episode.container_extension AS containerExtension,
    |    episode.added,
    |    episode.season,
    |    episode.title,
    |    episodeInfo.tmdb_id AS tmdbId,
    |    episodeInfo.release_date AS releaseDate,
    |    episodeInfo.plot,
    |    episodeInfo.duration_secs AS durationSecs,
    |    episodeInfo.duration,
    |    episodeInfo.bitrate,
    |    episodeInfo.rating,
    |    episodeInfo.movie_image AS movieImage,
    |    season.season_number AS seasonNumber,
    |    episodeInfo.playbackPosition
    |FROM Season AS season
    |INNER JOIN Episode AS episode ON season.season_id = episode.seasonCreatorId
    |INNER JOIN EpisodeInfo AS episodeInfo ON episode.episode_id = episodeInfo.episodeCreatorId
    |WHERE season.series_id = ? AND season.userId = ?
    |ORDER BY episode.season ASC, episode.episode_num ASC
    """.trimMargin(), mapper, 2) {
      bindString(0, series_id)
      bindLong(1, userId)
    }

    override fun toString(): String = "SeriesInfo.sq:selectSeasonsWithEpisodes"
  }
}
