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

public class MovieInfoQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectMovieDataWithInfo(
    streamId: Long,
    userId: Long,
    mapper: (
      streamId: Long,
      name: String,
      added: Double,
      category_id: Long,
      container_extension: String,
      custom_sid: String?,
      direct_source: String?,
      userId: Long,
      movieInfoId: Long?,
      streamIdCreator: Long?,
      kinopoisk_url: String?,
      tmdb_id: String?,
      name_: String?,
      o_name: String?,
      cover_big: String?,
      movie_image: String?,
      releasedate: String?,
      episode_run_time: String?,
      youtube_trailer: String?,
      director: String?,
      actors: String?,
      cast_: String?,
      description: String?,
      plot: String?,
      age: String?,
      mpaa_rating: String?,
      rating_count_kinopoisk: String?,
      country: String?,
      genre: String?,
      backdrop_path: String?,
      duration_secs: String?,
      duration: String?,
      bitrate: String?,
      rating: String?,
      userId_: Long?,
      playbackPosition: Long?,
    ) -> T,
  ): Query<T> = SelectMovieDataWithInfoQuery(streamId, userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getDouble(2)!!,
      cursor.getLong(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5),
      cursor.getString(6),
      cursor.getLong(7)!!,
      cursor.getLong(8),
      cursor.getLong(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getString(17),
      cursor.getString(18),
      cursor.getString(19),
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getString(23),
      cursor.getString(24),
      cursor.getString(25),
      cursor.getString(26),
      cursor.getString(27),
      cursor.getString(28),
      cursor.getString(29),
      cursor.getString(30),
      cursor.getString(31),
      cursor.getString(32),
      cursor.getString(33),
      cursor.getLong(34),
      cursor.getLong(35)
    )
  }

  public fun selectMovieDataWithInfo(streamId: Long, userId: Long): Query<SelectMovieDataWithInfo> =
      selectMovieDataWithInfo(streamId, userId) { streamId_, name, added, category_id,
      container_extension, custom_sid, direct_source, userId_, movieInfoId, streamIdCreator,
      kinopoisk_url, tmdb_id, name_, o_name, cover_big, movie_image, releasedate, episode_run_time,
      youtube_trailer, director, actors, cast_, description, plot, age, mpaa_rating,
      rating_count_kinopoisk, country, genre, backdrop_path, duration_secs, duration, bitrate,
      rating, userId__, playbackPosition ->
    SelectMovieDataWithInfo(
      streamId_,
      name,
      added,
      category_id,
      container_extension,
      custom_sid,
      direct_source,
      userId_,
      movieInfoId,
      streamIdCreator,
      kinopoisk_url,
      tmdb_id,
      name_,
      o_name,
      cover_big,
      movie_image,
      releasedate,
      episode_run_time,
      youtube_trailer,
      director,
      actors,
      cast_,
      description,
      plot,
      age,
      mpaa_rating,
      rating_count_kinopoisk,
      country,
      genre,
      backdrop_path,
      duration_secs,
      duration,
      bitrate,
      rating,
      userId__,
      playbackPosition
    )
  }

  public fun <T : Any> selectMovieInfoByStreamId(
    streamIdCreator: Long,
    userId: Long,
    mapper: (
      movieInfoId: Long,
      streamIdCreator: Long,
      kinopoisk_url: String?,
      tmdb_id: String?,
      name: String?,
      o_name: String?,
      cover_big: String?,
      movie_image: String?,
      releasedate: String?,
      episode_run_time: String?,
      youtube_trailer: String?,
      director: String?,
      actors: String?,
      cast_: String?,
      description: String?,
      plot: String?,
      age: String?,
      mpaa_rating: String?,
      rating_count_kinopoisk: String?,
      country: String?,
      genre: String?,
      backdrop_path: String?,
      duration_secs: String?,
      duration: String?,
      bitrate: String?,
      rating: String?,
      userId: Long,
      playbackPosition: Long,
    ) -> T,
  ): Query<T> = SelectMovieInfoByStreamIdQuery(streamIdCreator, userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2),
      cursor.getString(3),
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getString(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getString(17),
      cursor.getString(18),
      cursor.getString(19),
      cursor.getString(20),
      cursor.getString(21),
      cursor.getString(22),
      cursor.getString(23),
      cursor.getString(24),
      cursor.getString(25),
      cursor.getLong(26)!!,
      cursor.getLong(27)!!
    )
  }

  public fun selectMovieInfoByStreamId(streamIdCreator: Long, userId: Long): Query<MovieInfo> =
      selectMovieInfoByStreamId(streamIdCreator, userId) { movieInfoId, streamIdCreator_,
      kinopoisk_url, tmdb_id, name, o_name, cover_big, movie_image, releasedate, episode_run_time,
      youtube_trailer, director, actors, cast_, description, plot, age, mpaa_rating,
      rating_count_kinopoisk, country, genre, backdrop_path, duration_secs, duration, bitrate,
      rating, userId_, playbackPosition ->
    MovieInfo(
      movieInfoId,
      streamIdCreator_,
      kinopoisk_url,
      tmdb_id,
      name,
      o_name,
      cover_big,
      movie_image,
      releasedate,
      episode_run_time,
      youtube_trailer,
      director,
      actors,
      cast_,
      description,
      plot,
      age,
      mpaa_rating,
      rating_count_kinopoisk,
      country,
      genre,
      backdrop_path,
      duration_secs,
      duration,
      bitrate,
      rating,
      userId_,
      playbackPosition
    )
  }

  public fun selectPlaybackPosition(streamIdCreator: Long, userId: Long): Query<Long> =
      SelectPlaybackPositionQuery(streamIdCreator, userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertMovieData(
    streamId: Long?,
    name: String,
    added: Double,
    category_id: Long,
    container_extension: String,
    custom_sid: String?,
    direct_source: String?,
    userId: Long,
  ) {
    driver.execute(2_128_440_678, """
        |INSERT OR REPLACE INTO MovieData(streamId, name, added, category_id, container_extension, custom_sid, direct_source, userId)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindLong(0, streamId)
          bindString(1, name)
          bindDouble(2, added)
          bindLong(3, category_id)
          bindString(4, container_extension)
          bindString(5, custom_sid)
          bindString(6, direct_source)
          bindLong(7, userId)
        }
    notifyQueries(2_128_440_678) { emit ->
      emit("MovieData")
    }
  }

  public fun insertMovieInfo(
    streamIdCreator: Long,
    kinopoisk_url: String?,
    tmdb_id: String?,
    name: String?,
    o_name: String?,
    cover_big: String?,
    movie_image: String?,
    releasedate: String?,
    episode_run_time: String?,
    youtube_trailer: String?,
    director: String?,
    actors: String?,
    cast_: String?,
    description: String?,
    plot: String?,
    age: String?,
    mpaa_rating: String?,
    rating_count_kinopoisk: String?,
    country: String?,
    genre: String?,
    backdrop_path: String?,
    duration_secs: String?,
    duration: String?,
    bitrate: String?,
    rating: String?,
    userId: Long,
    playbackPosition: Long,
  ) {
    driver.execute(2_128_601_706, """
        |INSERT OR REPLACE INTO MovieInfo(streamIdCreator, kinopoisk_url, tmdb_id, name, o_name, cover_big, movie_image, releasedate, episode_run_time, youtube_trailer, director, actors, cast_, description, plot, age, mpaa_rating, rating_count_kinopoisk, country, genre, backdrop_path, duration_secs, duration, bitrate, rating, userId, playbackPosition)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 27) {
          bindLong(0, streamIdCreator)
          bindString(1, kinopoisk_url)
          bindString(2, tmdb_id)
          bindString(3, name)
          bindString(4, o_name)
          bindString(5, cover_big)
          bindString(6, movie_image)
          bindString(7, releasedate)
          bindString(8, episode_run_time)
          bindString(9, youtube_trailer)
          bindString(10, director)
          bindString(11, actors)
          bindString(12, cast_)
          bindString(13, description)
          bindString(14, plot)
          bindString(15, age)
          bindString(16, mpaa_rating)
          bindString(17, rating_count_kinopoisk)
          bindString(18, country)
          bindString(19, genre)
          bindString(20, backdrop_path)
          bindString(21, duration_secs)
          bindString(22, duration)
          bindString(23, bitrate)
          bindString(24, rating)
          bindLong(25, userId)
          bindLong(26, playbackPosition)
        }
    notifyQueries(2_128_601_706) { emit ->
      emit("MovieInfo")
    }
  }

  public fun updatePlaybackPosition(
    playbackPosition: Long,
    streamIdCreator: Long,
    userId: Long,
  ) {
    driver.execute(1_964_157_576,
        """UPDATE MovieInfo SET playbackPosition = ? WHERE streamIdCreator = ? AND userId = ?""", 3)
        {
          bindLong(0, playbackPosition)
          bindLong(1, streamIdCreator)
          bindLong(2, userId)
        }
    notifyQueries(1_964_157_576) { emit ->
      emit("MovieInfo")
    }
  }

  public fun deleteMovieInfoByUserId(userId: Long) {
    driver.execute(-275_659_435, """DELETE FROM MovieInfo WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-275_659_435) { emit ->
      emit("MovieInfo")
    }
  }

  public fun deleteMovieDataByUserId(userId: Long) {
    driver.execute(2_042_795_089, """DELETE FROM MovieData WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(2_042_795_089) { emit ->
      emit("MovieData")
    }
  }

  private inner class SelectMovieDataWithInfoQuery<out T : Any>(
    public val streamId: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MovieData", "MovieInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MovieData", "MovieInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(439_991_863, """
    |SELECT md.streamId, md.name, md.added, md.category_id, md.container_extension, md.custom_sid, md.direct_source, md.userId, mi.movieInfoId, mi.streamIdCreator, mi.kinopoisk_url, mi.tmdb_id, mi.name, mi.o_name, mi.cover_big, mi.movie_image, mi.releasedate, mi.episode_run_time, mi.youtube_trailer, mi.director, mi.actors, mi.cast_, mi.description, mi.plot, mi.age, mi.mpaa_rating, mi.rating_count_kinopoisk, mi.country, mi.genre, mi.backdrop_path, mi.duration_secs, mi.duration, mi.bitrate, mi.rating, mi.userId, mi.playbackPosition
    |FROM MovieData md
    |LEFT JOIN MovieInfo mi ON md.streamId = mi.streamIdCreator AND md.userId = mi.userId
    |WHERE md.streamId = ? AND md.userId = ?
    """.trimMargin(), mapper, 2) {
      bindLong(0, streamId)
      bindLong(1, userId)
    }

    override fun toString(): String = "MovieInfo.sq:selectMovieDataWithInfo"
  }

  private inner class SelectMovieInfoByStreamIdQuery<out T : Any>(
    public val streamIdCreator: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MovieInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MovieInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_208_241_433,
        """SELECT MovieInfo.movieInfoId, MovieInfo.streamIdCreator, MovieInfo.kinopoisk_url, MovieInfo.tmdb_id, MovieInfo.name, MovieInfo.o_name, MovieInfo.cover_big, MovieInfo.movie_image, MovieInfo.releasedate, MovieInfo.episode_run_time, MovieInfo.youtube_trailer, MovieInfo.director, MovieInfo.actors, MovieInfo.cast_, MovieInfo.description, MovieInfo.plot, MovieInfo.age, MovieInfo.mpaa_rating, MovieInfo.rating_count_kinopoisk, MovieInfo.country, MovieInfo.genre, MovieInfo.backdrop_path, MovieInfo.duration_secs, MovieInfo.duration, MovieInfo.bitrate, MovieInfo.rating, MovieInfo.userId, MovieInfo.playbackPosition FROM MovieInfo WHERE streamIdCreator = ? AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindLong(0, streamIdCreator)
      bindLong(1, userId)
    }

    override fun toString(): String = "MovieInfo.sq:selectMovieInfoByStreamId"
  }

  private inner class SelectPlaybackPositionQuery<out T : Any>(
    public val streamIdCreator: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("MovieInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("MovieInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-42_934_085,
        """SELECT playbackPosition FROM MovieInfo WHERE streamIdCreator = ? AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindLong(0, streamIdCreator)
      bindLong(1, userId)
    }

    override fun toString(): String = "MovieInfo.sq:selectPlaybackPosition"
  }
}
