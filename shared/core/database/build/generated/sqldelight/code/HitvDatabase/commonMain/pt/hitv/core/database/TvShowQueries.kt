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
import kotlin.collections.Collection
import pt.hitv.core.database.tvShow.SelectRecentlyViewed
import pt.hitv.core.database.tvShow.SelectRecentlyViewedPaged

public class TvShowQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectAllPagedQuery(userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectAllPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<TvShow> = selectAllPaged(userId, value_, value__) { tvShowId, num, name, series_id,
      cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based,
      backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectByCategoryPaged(
    userId: Long,
    category_id: String,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryPagedQuery(userId, category_id, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectByCategoryPaged(
    userId: Long,
    category_id: String,
    value_: Long,
    value__: Long,
  ): Query<TvShow> = selectByCategoryPaged(userId, category_id, value_, value__) { tvShowId, num,
      name, series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id_, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      category_id_,
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectFavoritesPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectFavoritesPagedQuery(userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectFavoritesPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<TvShow> = selectFavoritesPaged(userId, value_, value__) { tvShowId, num, name, series_id,
      cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based,
      backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectRecentlyViewedPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectRecentlyViewedPagedQuery(userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectRecentlyViewedPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<SelectRecentlyViewedPaged> = selectRecentlyViewedPaged(userId, value_, value__) {
      tvShowId, num, name, series_id, cover, plot, cast_, director, genre, releaseDate,
      last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time,
      category_id, isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash,
      syncVersion ->
    SelectRecentlyViewedPaged(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectRecentlyViewed(userId: Long, mapper: (
    tvShowId: Long,
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) -> T): Query<T> = SelectRecentlyViewedQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectRecentlyViewed(userId: Long): Query<SelectRecentlyViewed> =
      selectRecentlyViewed(userId) { tvShowId, num, name, series_id, cover, plot, cast_, director,
      genre, releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer,
      episode_run_time, category_id, isFavorite, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    SelectRecentlyViewed(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectLastAddedPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectLastAddedPagedQuery(userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectLastAddedPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<TvShow> = selectLastAddedPaged(userId, value_, value__) { tvShowId, num, name, series_id,
      cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based,
      backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectLastAdded(
    userId: Long,
    `value`: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectLastAddedQuery(userId, value) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectLastAdded(userId: Long, value_: Long): Query<TvShow> = selectLastAdded(userId,
      value_) { tvShowId, num, name, series_id, cover, plot, cast_, director, genre, releaseDate,
      last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time,
      category_id, isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash,
      syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectContinueWatching(
    userId: Long,
    `value`: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectContinueWatchingQuery(userId, value) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectContinueWatching(userId: Long, value_: Long): Query<TvShow> =
      selectContinueWatching(userId, value_) { tvShowId, num, name, series_id, cover, plot, cast_,
      director, genre, releaseDate, last_modified, rating, rating_5based, backdrop_path,
      youtube_trailer, episode_run_time, category_id, isFavorite, userId_, lastViewedTimestamp,
      lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> searchFts(
    TvShow_fts: String,
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SearchFtsQuery(TvShow_fts, userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun searchFts(
    TvShow_fts: String,
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<TvShow> = searchFts(TvShow_fts, userId, value_, value__) { tvShowId, num, name,
      series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> searchByName(
    userId: Long,
    `value`: String?,
    value_: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SearchByNameQuery(userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun searchByName(
    userId: Long,
    value_: String?,
    value__: Long,
  ): Query<TvShow> = searchByName(userId, value_, value__) { tvShowId, num, name, series_id, cover,
      plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based,
      backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectFavorites(userId: Long, mapper: (
    tvShowId: Long,
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) -> T): Query<T> = SelectFavoritesQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectFavorites(userId: Long): Query<TvShow> = selectFavorites(userId) { tvShowId, num,
      name, series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun selectFavoriteStatus(series_id: Long, userId: Long): Query<Long> =
      SelectFavoriteStatusQuery(series_id, userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectByCategorySorted(
    userId: Long,
    `value`: String,
    category_id: String,
    value_: String,
    value__: Long,
    value___: String,
    value____: Long,
    value_____: String,
    value______: Long,
    value_______: String,
    value________: Long,
    value_________: String,
    value__________: Long,
    value___________: String,
    value____________: Long,
    value_____________: Long,
    value______________: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategorySortedQuery(userId, value, category_id, value_, value__, value___,
      value____, value_____, value______, value_______, value________, value_________,
      value__________, value___________, value____________, value_____________,
      value______________) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectByCategorySorted(
    userId: Long,
    value_: String,
    category_id: String,
    value__: String,
    value___: Long,
    value____: String,
    value_____: Long,
    value______: String,
    value_______: Long,
    value________: String,
    value_________: Long,
    value__________: String,
    value___________: Long,
    value____________: String,
    value_____________: Long,
    value______________: Long,
    value_______________: Long,
  ): Query<TvShow> = selectByCategorySorted(userId, value_, category_id, value__, value___,
      value____, value_____, value______, value_______, value________, value_________,
      value__________, value___________, value____________, value_____________, value______________,
      value_______________) { tvShowId, num, name, series_id, cover, plot, cast_, director, genre,
      releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer,
      episode_run_time, category_id_, isFavorite, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      category_id_,
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun countByUserId(userId: Long): Query<Long> = CountByUserIdQuery(userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countByCategoryAndUserId(userId: Long, category_id: String): Query<Long> =
      CountByCategoryAndUserIdQuery(userId, category_id) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countSearchTvShows(userId: Long, name: String): Query<Long> =
      CountSearchTvShowsQuery(userId, name) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countFavorites(userId: Long): Query<Long> = CountFavoritesQuery(userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countRecentlyViewed(userId: Long): Query<Long> = CountRecentlyViewedQuery(userId) {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun countRecentTvShows(userId: Long, lastSeen: Long): Query<Long> =
      CountRecentTvShowsQuery(userId, lastSeen) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> findExistingTvShow(
    userId: Long,
    series_id: Long,
    category_id: String,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = FindExistingTvShowQuery(userId, series_id, category_id) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun findExistingTvShow(
    userId: Long,
    series_id: Long,
    category_id: String,
  ): Query<TvShow> = findExistingTvShow(userId, series_id, category_id) { tvShowId, num, name,
      series_id_, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id_, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id_,
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
      category_id_,
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectAllForSync(userId: Long, mapper: (
    tvShowId: Long,
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) -> T): Query<T> = SelectAllForSyncQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectAllForSync(userId: Long): Query<TvShow> = selectAllForSync(userId) { tvShowId,
      num, name, series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectAllLimited(
    userId: Long,
    `value`: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectAllLimitedQuery(userId, value) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectAllLimited(userId: Long, value_: Long): Query<TvShow> = selectAllLimited(userId,
      value_) { tvShowId, num, name, series_id, cover, plot, cast_, director, genre, releaseDate,
      last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time,
      category_id, isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash,
      syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectByCategoryLimited(
    userId: Long,
    category_id: String,
    `value`: Long,
    mapper: (
      tvShowId: Long,
      num: Long?,
      name: String?,
      series_id: Long,
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
      category_id: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryLimitedQuery(userId, category_id, value) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getString(7),
      cursor.getString(8),
      cursor.getString(9),
      cursor.getString(10),
      cursor.getString(11),
      cursor.getDouble(12),
      cursor.getString(13),
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16)!!,
      cursor.getLong(17)!!,
      cursor.getLong(18)!!,
      cursor.getLong(19)!!,
      cursor.getLong(20)!!,
      cursor.getLong(21)!!,
      cursor.getString(22),
      cursor.getLong(23)!!
    )
  }

  public fun selectByCategoryLimited(
    userId: Long,
    category_id: String,
    value_: Long,
  ): Query<TvShow> = selectByCategoryLimited(userId, category_id, value_) { tvShowId, num, name,
      series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating,
      rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id_, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    TvShow(
      tvShowId,
      num,
      name,
      series_id,
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
      category_id_,
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun insertOrReplace(
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(-1_344_060_294, """
        |INSERT OR REPLACE INTO TvShow(num, name, series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 23) {
          bindLong(0, num)
          bindString(1, name)
          bindLong(2, series_id)
          bindString(3, cover)
          bindString(4, plot)
          bindString(5, cast_)
          bindString(6, director)
          bindString(7, genre)
          bindString(8, releaseDate)
          bindString(9, last_modified)
          bindString(10, rating)
          bindDouble(11, rating_5based)
          bindString(12, backdrop_path)
          bindString(13, youtube_trailer)
          bindString(14, episode_run_time)
          bindString(15, category_id)
          bindLong(16, isFavorite)
          bindLong(17, userId)
          bindLong(18, lastViewedTimestamp)
          bindLong(19, lastUpdated)
          bindLong(20, lastSeen)
          bindString(21, contentHash)
          bindLong(22, syncVersion)
        }
    notifyQueries(-1_344_060_294) { emit ->
      emit("TvShow")
    }
  }

  public fun insertOrIgnore(
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(1_778_981_708, """
        |INSERT OR IGNORE INTO TvShow(num, name, series_id, cover, plot, cast_, director, genre, releaseDate, last_modified, rating, rating_5based, backdrop_path, youtube_trailer, episode_run_time, category_id, isFavorite, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 23) {
          bindLong(0, num)
          bindString(1, name)
          bindLong(2, series_id)
          bindString(3, cover)
          bindString(4, plot)
          bindString(5, cast_)
          bindString(6, director)
          bindString(7, genre)
          bindString(8, releaseDate)
          bindString(9, last_modified)
          bindString(10, rating)
          bindDouble(11, rating_5based)
          bindString(12, backdrop_path)
          bindString(13, youtube_trailer)
          bindString(14, episode_run_time)
          bindString(15, category_id)
          bindLong(16, isFavorite)
          bindLong(17, userId)
          bindLong(18, lastViewedTimestamp)
          bindLong(19, lastUpdated)
          bindLong(20, lastSeen)
          bindString(21, contentHash)
          bindLong(22, syncVersion)
        }
    notifyQueries(1_778_981_708) { emit ->
      emit("TvShow")
    }
  }

  public fun updateFavoriteStatus(
    isFavorite: Long,
    series_id: Long,
    userId: Long,
  ) {
    driver.execute(541_224_757,
        """UPDATE TvShow SET isFavorite = ? WHERE series_id = ? AND userId = ?""", 3) {
          bindLong(0, isFavorite)
          bindLong(1, series_id)
          bindLong(2, userId)
        }
    notifyQueries(541_224_757) { emit ->
      emit("TvShow")
    }
  }

  public fun updateLastViewedTimestamp(
    lastViewedTimestamp: Long,
    series_id: Long,
    userId: Long,
  ) {
    driver.execute(-19_763_243,
        """UPDATE TvShow SET lastViewedTimestamp = ? WHERE series_id = ? AND userId = ?""", 3) {
          bindLong(0, lastViewedTimestamp)
          bindLong(1, series_id)
          bindLong(2, userId)
        }
    notifyQueries(-19_763_243) { emit ->
      emit("TvShow")
    }
  }

  public fun updateById(
    num: Long?,
    name: String?,
    series_id: Long,
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
    category_id: String,
    isFavorite: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
    tvShowId: Long,
  ) {
    driver.execute(1_921_718_041, """
        |UPDATE TvShow SET num = ?, name = ?, series_id = ?, cover = ?, plot = ?, cast_ = ?, director = ?, genre = ?, releaseDate = ?, last_modified = ?, rating = ?, rating_5based = ?, backdrop_path = ?, youtube_trailer = ?, episode_run_time = ?, category_id = ?, isFavorite = ?, lastViewedTimestamp = ?, lastUpdated = ?, lastSeen = ?, contentHash = ?, syncVersion = ?
        |WHERE tvShowId = ?
        """.trimMargin(), 23) {
          bindLong(0, num)
          bindString(1, name)
          bindLong(2, series_id)
          bindString(3, cover)
          bindString(4, plot)
          bindString(5, cast_)
          bindString(6, director)
          bindString(7, genre)
          bindString(8, releaseDate)
          bindString(9, last_modified)
          bindString(10, rating)
          bindDouble(11, rating_5based)
          bindString(12, backdrop_path)
          bindString(13, youtube_trailer)
          bindString(14, episode_run_time)
          bindString(15, category_id)
          bindLong(16, isFavorite)
          bindLong(17, lastViewedTimestamp)
          bindLong(18, lastUpdated)
          bindLong(19, lastSeen)
          bindString(20, contentHash)
          bindLong(21, syncVersion)
          bindLong(22, tvShowId)
        }
    notifyQueries(1_921_718_041) { emit ->
      emit("TvShow")
    }
  }

  public fun markAsSeen(
    lastSeen: Long,
    userId: Long,
    tvShowId: Collection<Long>,
  ) {
    val tvShowIdIndexes = createArguments(count = tvShowId.size)
    driver.execute(null,
        """UPDATE TvShow SET lastSeen = ? WHERE userId = ? AND tvShowId IN $tvShowIdIndexes""", 2 +
        tvShowId.size) {
          bindLong(0, lastSeen)
          bindLong(1, userId)
          tvShowId.forEachIndexed { index, tvShowId_ ->
            bindLong(index + 2, tvShowId_)
          }
        }
    notifyQueries(-1_991_154_216) { emit ->
      emit("TvShow")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(-506_899_418, """DELETE FROM TvShow WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-506_899_418) { emit ->
      emit("TvShow")
    }
  }

  public fun deleteStale(userId: Long, lastSeen: Long) {
    driver.execute(-1_089_085_040, """DELETE FROM TvShow WHERE userId = ? AND lastSeen < ?""", 2) {
          bindLong(0, userId)
          bindLong(1, lastSeen)
        }
    notifyQueries(-1_089_085_040) { emit ->
      emit("TvShow")
    }
  }

  private inner class SelectAllPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_566_033_010,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? GROUP BY series_id ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "TvShow.sq:selectAllPaged"
  }

  private inner class SelectByCategoryPagedQuery<out T : Any>(
    public val userId: Long,
    public val category_id: String,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_538_708_486,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND category_id = ? GROUP BY series_id ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 4) {
      bindLong(0, userId)
      bindString(1, category_id)
      bindLong(2, value)
      bindLong(3, value_)
    }

    override fun toString(): String = "TvShow.sq:selectByCategoryPaged"
  }

  private inner class SelectFavoritesPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_605_249_304,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND isFavorite = 1 GROUP BY series_id ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "TvShow.sq:selectFavoritesPaged"
  }

  private inner class SelectRecentlyViewedPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_970_418_545,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 GROUP BY series_id ORDER BY lastViewedTimestamp DESC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "TvShow.sq:selectRecentlyViewedPaged"
  }

  private inner class SelectRecentlyViewedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_918_533_946,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 ORDER BY lastViewedTimestamp DESC LIMIT 10""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:selectRecentlyViewed"
  }

  private inner class SelectLastAddedPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_026_760_517,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? GROUP BY series_id ORDER BY CAST(last_modified AS INTEGER) DESC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "TvShow.sq:selectLastAddedPaged"
  }

  private inner class SelectLastAddedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_217_974_864,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? GROUP BY series_id ORDER BY CAST(last_modified AS INTEGER) DESC LIMIT ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "TvShow.sq:selectLastAdded"
  }

  private inner class SelectContinueWatchingQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", "Season", "Episode", "EpisodeInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", "Season", "Episode", "EpisodeInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-367_811_212, """
    |SELECT DISTINCT t.tvShowId, t.num, t.name, t.series_id, t.cover, t.plot, t.cast_, t.director, t.genre, t.releaseDate, t.last_modified, t.rating, t.rating_5based, t.backdrop_path, t.youtube_trailer, t.episode_run_time, t.category_id, t.isFavorite, t.userId, t.lastViewedTimestamp, t.lastUpdated, t.lastSeen, t.contentHash, t.syncVersion FROM TvShow t
    |INNER JOIN Season s ON t.series_id = CAST(s.series_id AS INTEGER) AND t.userId = s.userId
    |INNER JOIN Episode e ON s.season_id = e.seasonCreatorId AND s.userId = e.userId
    |INNER JOIN EpisodeInfo ei ON e.episode_id = ei.episodeCreatorId AND e.userId = ei.userId
    |WHERE t.userId = ? AND ei.playbackPosition > 0
    |ORDER BY t.lastViewedTimestamp DESC
    |LIMIT ?
    """.trimMargin(), mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "TvShow.sq:selectContinueWatching"
  }

  private inner class SearchFtsQuery<out T : Any>(
    public val TvShow_fts: String,
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", "TvShow_fts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", "TvShow_fts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_295_548_415, """
    |SELECT t.tvShowId, t.num, t.name, t.series_id, t.cover, t.plot, t.cast_, t.director, t.genre, t.releaseDate, t.last_modified, t.rating, t.rating_5based, t.backdrop_path, t.youtube_trailer, t.episode_run_time, t.category_id, t.isFavorite, t.userId, t.lastViewedTimestamp, t.lastUpdated, t.lastSeen, t.contentHash, t.syncVersion FROM TvShow t
    |INNER JOIN TvShow_fts fts ON t.tvShowId = fts.docid
    |WHERE TvShow_fts MATCH ? AND t.userId = ?
    |GROUP BY t.series_id
    |ORDER BY t.name ASC
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 4) {
      bindString(0, TvShow_fts)
      bindLong(1, userId)
      bindLong(2, value)
      bindLong(3, value_)
    }

    override fun toString(): String = "TvShow.sq:searchFts"
  }

  private inner class SearchByNameQuery<out T : Any>(
    public val userId: Long,
    public val `value`: String?,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(995_804_840,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND LOWER(name) LIKE LOWER(?) GROUP BY series_id ORDER BY name ASC LIMIT ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "TvShow.sq:searchByName"
  }

  private inner class SelectFavoritesQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_742_581_917,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE isFavorite = 1 AND userId = ? GROUP BY series_id""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:selectFavorites"
  }

  private inner class SelectFavoriteStatusQuery<out T : Any>(
    public val series_id: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(722_376_232,
        """SELECT isFavorite FROM TvShow WHERE series_id = ? AND userId = ? LIMIT 1""", mapper, 2) {
      bindLong(0, series_id)
      bindLong(1, userId)
    }

    override fun toString(): String = "TvShow.sq:selectFavoriteStatus"
  }

  private inner class SelectByCategorySortedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: String,
    public val category_id: String,
    public val value_: String,
    public val value__: Long,
    public val value___: String,
    public val value____: Long,
    public val value_____: String,
    public val value______: Long,
    public val value_______: String,
    public val value________: Long,
    public val value_________: String,
    public val value__________: Long,
    public val value___________: String,
    public val value____________: Long,
    public val value_____________: Long,
    public val value______________: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(554_481_804, """
    |SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow
    |WHERE userId = ? AND (? = '' OR category_id = ?)
    |GROUP BY series_id
    |ORDER BY
    |    CASE WHEN ? = 'name' AND ? = 1 THEN name END ASC,
    |    CASE WHEN ? = 'name' AND ? = 0 THEN name END DESC,
    |    CASE WHEN ? = 'rating' AND ? = 1 THEN rating END ASC,
    |    CASE WHEN ? = 'rating' AND ? = 0 THEN rating END DESC,
    |    CASE WHEN ? = 'added' AND ? = 1 THEN CAST(last_modified AS INTEGER) END ASC,
    |    CASE WHEN ? = 'added' AND ? = 0 THEN CAST(last_modified AS INTEGER) END DESC,
    |    CAST(last_modified AS INTEGER) DESC
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 17) {
      bindLong(0, userId)
      bindString(1, value)
      bindString(2, category_id)
      bindString(3, value_)
      bindLong(4, value__)
      bindString(5, value___)
      bindLong(6, value____)
      bindString(7, value_____)
      bindLong(8, value______)
      bindString(9, value_______)
      bindLong(10, value________)
      bindString(11, value_________)
      bindLong(12, value__________)
      bindString(13, value___________)
      bindLong(14, value____________)
      bindLong(15, value_____________)
      bindLong(16, value______________)
    }

    override fun toString(): String = "TvShow.sq:selectByCategorySorted"
  }

  private inner class CountByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-190_279_666,
        """SELECT COUNT(DISTINCT series_id) FROM TvShow WHERE userId = ?""", mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:countByUserId"
  }

  private inner class CountByCategoryAndUserIdQuery<out T : Any>(
    public val userId: Long,
    public val category_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_151_658_199,
        """SELECT COUNT(DISTINCT series_id) FROM TvShow WHERE userId = ? AND category_id = ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindString(1, category_id)
    }

    override fun toString(): String = "TvShow.sq:countByCategoryAndUserId"
  }

  private inner class CountSearchTvShowsQuery<out T : Any>(
    public val userId: Long,
    public val name: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-77_563_653,
        """SELECT COUNT(DISTINCT series_id) FROM TvShow WHERE userId = ? AND name LIKE ?""", mapper,
        2) {
      bindLong(0, userId)
      bindString(1, name)
    }

    override fun toString(): String = "TvShow.sq:countSearchTvShows"
  }

  private inner class CountFavoritesQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_247_770,
        """SELECT COUNT(DISTINCT series_id) FROM TvShow WHERE userId = ? AND isFavorite = 1""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:countFavorites"
  }

  private inner class CountRecentlyViewedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-504_684_771,
        """SELECT COUNT(DISTINCT series_id) FROM TvShow WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:countRecentlyViewed"
  }

  private inner class CountRecentTvShowsQuery<out T : Any>(
    public val userId: Long,
    public val lastSeen: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(208_337_128,
        """SELECT COUNT(*) FROM TvShow WHERE userId = ? AND lastSeen >= ?""", mapper, 2) {
      bindLong(0, userId)
      bindLong(1, lastSeen)
    }

    override fun toString(): String = "TvShow.sq:countRecentTvShows"
  }

  private inner class FindExistingTvShowQuery<out T : Any>(
    public val userId: Long,
    public val series_id: Long,
    public val category_id: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-704_085_759,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND series_id = ? AND category_id = ? LIMIT 1""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, series_id)
      bindString(2, category_id)
    }

    override fun toString(): String = "TvShow.sq:findExistingTvShow"
  }

  private inner class SelectAllForSyncQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_593_816_931,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "TvShow.sq:selectAllForSync"
  }

  private inner class SelectAllLimitedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-739_504_333,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? GROUP BY series_id ORDER BY name ASC LIMIT ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "TvShow.sq:selectAllLimited"
  }

  private inner class SelectByCategoryLimitedQuery<out T : Any>(
    public val userId: Long,
    public val category_id: String,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("TvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("TvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-2_085_197_397,
        """SELECT TvShow.tvShowId, TvShow.num, TvShow.name, TvShow.series_id, TvShow.cover, TvShow.plot, TvShow.cast_, TvShow.director, TvShow.genre, TvShow.releaseDate, TvShow.last_modified, TvShow.rating, TvShow.rating_5based, TvShow.backdrop_path, TvShow.youtube_trailer, TvShow.episode_run_time, TvShow.category_id, TvShow.isFavorite, TvShow.userId, TvShow.lastViewedTimestamp, TvShow.lastUpdated, TvShow.lastSeen, TvShow.contentHash, TvShow.syncVersion FROM TvShow WHERE userId = ? AND category_id = ? GROUP BY series_id ORDER BY name ASC LIMIT ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, category_id)
      bindLong(2, value)
    }

    override fun toString(): String = "TvShow.sq:selectByCategoryLimited"
  }
}
