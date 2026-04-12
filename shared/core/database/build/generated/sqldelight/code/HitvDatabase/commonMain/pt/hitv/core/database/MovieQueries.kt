package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String
import kotlin.collections.Collection
import pt.hitv.core.database.movie.SelectRecentlyViewed
import pt.hitv.core.database.movie.SelectRecentlyViewedPaged

public class MovieQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectAllPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Movie> = selectAllPaged(userId, value_, value__) { movieId, name, streamId, streamIcon,
      rating, added, categoryCreatorId, containerExtension, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
    categoryCreatorId: String,
    `value`: Long,
    value_: Long,
    mapper: (
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryPagedQuery(userId, categoryCreatorId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectByCategoryPaged(
    userId: Long,
    categoryCreatorId: String,
    value_: Long,
    value__: Long,
  ): Query<Movie> = selectByCategoryPaged(userId, categoryCreatorId, value_, value__) { movieId,
      name, streamId, streamIcon, rating, added, categoryCreatorId_, containerExtension, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId_,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectFavoritesPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Movie> = selectFavoritesPaged(userId, value_, value__) { movieId, name, streamId,
      streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectRecentlyViewedPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<SelectRecentlyViewedPaged> = selectRecentlyViewedPaged(userId, value_, value__) {
      movieId, name, streamId, streamIcon, rating, added, categoryCreatorId, containerExtension,
      isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    SelectRecentlyViewedPaged(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
    movieId: Long,
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectRecentlyViewed(userId: Long): Query<SelectRecentlyViewed> =
      selectRecentlyViewed(userId) { movieId, name, streamId, streamIcon, rating, added,
      categoryCreatorId, containerExtension, isFavorite, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    SelectRecentlyViewed(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectLastAddedPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Movie> = selectLastAddedPaged(userId, value_, value__) { movieId, name, streamId,
      streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectLastAdded(userId: Long, value_: Long): Query<Movie> = selectLastAdded(userId,
      value_) { movieId, name, streamId, streamIcon, rating, added, categoryCreatorId,
      containerExtension, isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen,
      contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectContinueWatching(userId: Long, value_: Long): Query<Movie> =
      selectContinueWatching(userId, value_) { movieId, name, streamId, streamIcon, rating, added,
      categoryCreatorId, containerExtension, isFavorite, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
    Movie_fts: String,
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SearchFtsQuery(Movie_fts, userId, value, value_) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun searchFts(
    Movie_fts: String,
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Movie> = searchFts(Movie_fts, userId, value_, value__) { movieId, name, streamId,
      streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun searchByName(
    userId: Long,
    value_: String?,
    value__: Long,
  ): Query<Movie> = searchByName(userId, value_, value__) { movieId, name, streamId, streamIcon,
      rating, added, categoryCreatorId, containerExtension, isFavorite, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
    movieId: Long,
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectFavorites(userId: Long): Query<Movie> = selectFavorites(userId) { movieId, name,
      streamId, streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
      isFavorite,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun selectFavoriteStatus(streamId: String, userId: Long): Query<Long> =
      SelectFavoriteStatusQuery(streamId, userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectByCategorySorted(
    userId: Long,
    `value`: String,
    categoryCreatorId: String,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategorySortedQuery(userId, value, categoryCreatorId, value_, value__,
      value___, value____, value_____, value______, value_______, value________, value_________,
      value__________, value___________, value____________, value_____________,
      value______________) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectByCategorySorted(
    userId: Long,
    value_: String,
    categoryCreatorId: String,
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
  ): Query<Movie> = selectByCategorySorted(userId, value_, categoryCreatorId, value__, value___,
      value____, value_____, value______, value_______, value________, value_________,
      value__________, value___________, value____________, value_____________, value______________,
      value_______________) { movieId, name, streamId, streamIcon, rating, added,
      categoryCreatorId_, containerExtension, isFavorite, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId_,
      containerExtension,
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

  public fun countByCategoryAndUserId(userId: Long, categoryCreatorId: String): Query<Long> =
      CountByCategoryAndUserIdQuery(userId, categoryCreatorId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countRecentMovies(userId: Long, lastSeen: Long): Query<Long> =
      CountRecentMoviesQuery(userId, lastSeen) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> findExistingMovie(
    userId: Long,
    streamId: String,
    categoryCreatorId: String,
    mapper: (
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = FindExistingMovieQuery(userId, streamId, categoryCreatorId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun findExistingMovie(
    userId: Long,
    streamId: String,
    categoryCreatorId: String,
  ): Query<Movie> = findExistingMovie(userId, streamId, categoryCreatorId) { movieId, name,
      streamId_, streamIcon, rating, added, categoryCreatorId_, containerExtension, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId_,
      streamIcon,
      rating,
      added,
      categoryCreatorId_,
      containerExtension,
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
    movieId: Long,
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectAllForSync(userId: Long): Query<Movie> = selectAllForSync(userId) { movieId,
      name, streamId, streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
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
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectAllLimited(userId: Long, value_: Long): Query<Movie> = selectAllLimited(userId,
      value_) { movieId, name, streamId, streamIcon, rating, added, categoryCreatorId,
      containerExtension, isFavorite, userId_, lastViewedTimestamp, lastUpdated, lastSeen,
      contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId,
      containerExtension,
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
    categoryCreatorId: String,
    `value`: Long,
    mapper: (
      movieId: Long,
      name: String,
      streamId: String,
      streamIcon: String,
      rating: String,
      added: String,
      categoryCreatorId: String,
      containerExtension: String,
      isFavorite: Long,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryLimitedQuery(userId, categoryCreatorId, value) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4)!!,
      cursor.getString(5)!!,
      cursor.getString(6)!!,
      cursor.getString(7)!!,
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getLong(12)!!,
      cursor.getString(13),
      cursor.getLong(14)!!
    )
  }

  public fun selectByCategoryLimited(
    userId: Long,
    categoryCreatorId: String,
    value_: Long,
  ): Query<Movie> = selectByCategoryLimited(userId, categoryCreatorId, value_) { movieId, name,
      streamId, streamIcon, rating, added, categoryCreatorId_, containerExtension, isFavorite,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Movie(
      movieId,
      name,
      streamId,
      streamIcon,
      rating,
      added,
      categoryCreatorId_,
      containerExtension,
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
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(407_656_907, """
        |INSERT OR REPLACE INTO Movie(name, streamId, streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 14) {
          bindString(0, name)
          bindString(1, streamId)
          bindString(2, streamIcon)
          bindString(3, rating)
          bindString(4, added)
          bindString(5, categoryCreatorId)
          bindString(6, containerExtension)
          bindLong(7, isFavorite)
          bindLong(8, userId)
          bindLong(9, lastViewedTimestamp)
          bindLong(10, lastUpdated)
          bindLong(11, lastSeen)
          bindString(12, contentHash)
          bindLong(13, syncVersion)
        }
    notifyQueries(407_656_907) { emit ->
      emit("Movie")
    }
  }

  public fun insertOrIgnore(
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
    isFavorite: Long,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(-1_905_289_253, """
        |INSERT OR IGNORE INTO Movie(name, streamId, streamIcon, rating, added, categoryCreatorId, containerExtension, isFavorite, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 14) {
          bindString(0, name)
          bindString(1, streamId)
          bindString(2, streamIcon)
          bindString(3, rating)
          bindString(4, added)
          bindString(5, categoryCreatorId)
          bindString(6, containerExtension)
          bindLong(7, isFavorite)
          bindLong(8, userId)
          bindLong(9, lastViewedTimestamp)
          bindLong(10, lastUpdated)
          bindLong(11, lastSeen)
          bindString(12, contentHash)
          bindLong(13, syncVersion)
        }
    notifyQueries(-1_905_289_253) { emit ->
      emit("Movie")
    }
  }

  public fun updateFavorite(
    isFavorite: Long,
    streamId: String,
    userId: Long,
  ) {
    driver.execute(-1_770_519_054,
        """UPDATE Movie SET isFavorite = ? WHERE streamId = ? AND userId = ?""", 3) {
          bindLong(0, isFavorite)
          bindString(1, streamId)
          bindLong(2, userId)
        }
    notifyQueries(-1_770_519_054) { emit ->
      emit("Movie")
    }
  }

  public fun updateLastViewedTimestamp(
    lastViewedTimestamp: Long,
    streamId: String,
    userId: Long,
  ) {
    driver.execute(-1_761_487_898,
        """UPDATE Movie SET lastViewedTimestamp = ? WHERE streamId = ? AND userId = ?""", 3) {
          bindLong(0, lastViewedTimestamp)
          bindString(1, streamId)
          bindLong(2, userId)
        }
    notifyQueries(-1_761_487_898) { emit ->
      emit("Movie")
    }
  }

  public fun updateById(
    name: String,
    streamId: String,
    streamIcon: String,
    rating: String,
    added: String,
    categoryCreatorId: String,
    containerExtension: String,
    isFavorite: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
    movieId: Long,
  ) {
    driver.execute(-1_274_268_888, """
        |UPDATE Movie SET name = ?, streamId = ?, streamIcon = ?, rating = ?, added = ?, categoryCreatorId = ?, containerExtension = ?, isFavorite = ?, lastViewedTimestamp = ?, lastUpdated = ?, lastSeen = ?, contentHash = ?, syncVersion = ?
        |WHERE movieId = ?
        """.trimMargin(), 14) {
          bindString(0, name)
          bindString(1, streamId)
          bindString(2, streamIcon)
          bindString(3, rating)
          bindString(4, added)
          bindString(5, categoryCreatorId)
          bindString(6, containerExtension)
          bindLong(7, isFavorite)
          bindLong(8, lastViewedTimestamp)
          bindLong(9, lastUpdated)
          bindLong(10, lastSeen)
          bindString(11, contentHash)
          bindLong(12, syncVersion)
          bindLong(13, movieId)
        }
    notifyQueries(-1_274_268_888) { emit ->
      emit("Movie")
    }
  }

  public fun markAsSeen(
    lastSeen: Long,
    userId: Long,
    movieId: Collection<Long>,
  ) {
    val movieIdIndexes = createArguments(count = movieId.size)
    driver.execute(null,
        """UPDATE Movie SET lastSeen = ? WHERE userId = ? AND movieId IN $movieIdIndexes""", 2 +
        movieId.size) {
          bindLong(0, lastSeen)
          bindLong(1, userId)
          movieId.forEachIndexed { index, movieId_ ->
            bindLong(index + 2, movieId_)
          }
        }
    notifyQueries(-892_173_849) { emit ->
      emit("Movie")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(103_796_917, """DELETE FROM Movie WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(103_796_917) { emit ->
      emit("Movie")
    }
  }

  public fun deleteStale(userId: Long, lastSeen: Long) {
    driver.execute(-1_380_432_031, """DELETE FROM Movie WHERE userId = ? AND lastSeen < ?""", 2) {
          bindLong(0, userId)
          bindLong(1, lastSeen)
        }
    notifyQueries(-1_380_432_031) { emit ->
      emit("Movie")
    }
  }

  private inner class SelectAllPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-955_336_675,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? GROUP BY streamId ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Movie.sq:selectAllPaged"
  }

  private inner class SelectByCategoryPagedQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_324_029_801,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND categoryCreatorId = ? GROUP BY streamId ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 4) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
      bindLong(2, value)
      bindLong(3, value_)
    }

    override fun toString(): String = "Movie.sq:selectByCategoryPaged"
  }

  private inner class SelectFavoritesPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(820_166_247,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND isFavorite = 1 GROUP BY streamId ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Movie.sq:selectFavoritesPaged"
  }

  private inner class SelectRecentlyViewedPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(582_824_096,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 GROUP BY streamId ORDER BY lastViewedTimestamp DESC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Movie.sq:selectRecentlyViewedPaged"
  }

  private inner class SelectRecentlyViewedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_591_350_293,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 GROUP BY streamId ORDER BY lastViewedTimestamp DESC LIMIT 10""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Movie.sq:selectRecentlyViewed"
  }

  private inner class SelectLastAddedPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(241_677_460,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? GROUP BY streamId ORDER BY CAST(added AS INTEGER) DESC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Movie.sq:selectLastAddedPaged"
  }

  private inner class SelectLastAddedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_325_275_231,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? GROUP BY streamId ORDER BY CAST(added AS INTEGER) DESC LIMIT ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "Movie.sq:selectLastAdded"
  }

  private inner class SelectContinueWatchingQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", "MovieInfo", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", "MovieInfo", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_081_615_107, """
    |SELECT m.movieId, m.name, m.streamId, m.streamIcon, m.rating, m.added, m.categoryCreatorId, m.containerExtension, m.isFavorite, m.userId, m.lastViewedTimestamp, m.lastUpdated, m.lastSeen, m.contentHash, m.syncVersion FROM Movie m
    |INNER JOIN MovieInfo mi ON m.streamId = CAST(mi.streamIdCreator AS TEXT) AND m.userId = mi.userId
    |WHERE m.userId = ? AND mi.playbackPosition > 0
    |GROUP BY m.streamId
    |ORDER BY m.lastViewedTimestamp DESC
    |LIMIT ?
    """.trimMargin(), mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "Movie.sq:selectContinueWatching"
  }

  private inner class SearchFtsQuery<out T : Any>(
    public val Movie_fts: String,
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", "Movie_fts", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", "Movie_fts", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_439_947_248, """
    |SELECT m.movieId, m.name, m.streamId, m.streamIcon, m.rating, m.added, m.categoryCreatorId, m.containerExtension, m.isFavorite, m.userId, m.lastViewedTimestamp, m.lastUpdated, m.lastSeen, m.contentHash, m.syncVersion FROM Movie m
    |INNER JOIN Movie_fts fts ON m.movieId = fts.docid
    |WHERE Movie_fts MATCH ? AND m.userId = ?
    |GROUP BY m.streamId
    |ORDER BY m.name ASC
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 4) {
      bindString(0, Movie_fts)
      bindLong(1, userId)
      bindLong(2, value)
      bindLong(3, value_)
    }

    override fun toString(): String = "Movie.sq:searchFts"
  }

  private inner class SearchByNameQuery<out T : Any>(
    public val userId: Long,
    public val `value`: String?,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(553_982_711,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND LOWER(name) LIKE LOWER(?) GROUP BY streamId ORDER BY name ASC LIMIT ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Movie.sq:searchByName"
  }

  private inner class SelectFavoritesQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-800_668_178,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE isFavorite = 1 AND userId = ? GROUP BY streamId""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Movie.sq:selectFavorites"
  }

  private inner class SelectFavoriteStatusQuery<out T : Any>(
    public val streamId: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-62_706_825,
        """SELECT isFavorite FROM Movie WHERE streamId = ? AND userId = ? LIMIT 1""", mapper, 2) {
      bindString(0, streamId)
      bindLong(1, userId)
    }

    override fun toString(): String = "Movie.sq:selectFavoriteStatus"
  }

  private inner class SelectByCategorySortedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: String,
    public val categoryCreatorId: String,
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
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_003_908_123, """
    |SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie
    |WHERE userId = ? AND (? = '' OR categoryCreatorId = ?)
    |GROUP BY streamId
    |ORDER BY
    |    CASE WHEN ? = 'name' AND ? = 1 THEN name END ASC,
    |    CASE WHEN ? = 'name' AND ? = 0 THEN name END DESC,
    |    CASE WHEN ? = 'rating' AND ? = 1 THEN rating END ASC,
    |    CASE WHEN ? = 'rating' AND ? = 0 THEN rating END DESC,
    |    CASE WHEN ? = 'added' AND ? = 1 THEN CAST(added AS INTEGER) END ASC,
    |    CASE WHEN ? = 'added' AND ? = 0 THEN CAST(added AS INTEGER) END DESC,
    |    CAST(added AS INTEGER) DESC
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 17) {
      bindLong(0, userId)
      bindString(1, value)
      bindString(2, categoryCreatorId)
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

    override fun toString(): String = "Movie.sq:selectByCategorySorted"
  }

  private inner class CountByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_001_863_777,
        """SELECT COUNT(DISTINCT streamId) FROM Movie WHERE userId = ?""", mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Movie.sq:countByUserId"
  }

  private inner class CountByCategoryAndUserIdQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_814_020_442,
        """SELECT COUNT(DISTINCT streamId) FROM Movie WHERE userId = ? AND categoryCreatorId = ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
    }

    override fun toString(): String = "Movie.sq:countByCategoryAndUserId"
  }

  private inner class CountRecentMoviesQuery<out T : Any>(
    public val userId: Long,
    public val lastSeen: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_929_249_952,
        """SELECT COUNT(*) FROM Movie WHERE userId = ? AND lastSeen >= ?""", mapper, 2) {
      bindLong(0, userId)
      bindLong(1, lastSeen)
    }

    override fun toString(): String = "Movie.sq:countRecentMovies"
  }

  private inner class FindExistingMovieQuery<out T : Any>(
    public val userId: Long,
    public val streamId: String,
    public val categoryCreatorId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_544_813_471,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND streamId = ? AND categoryCreatorId = ? LIMIT 1""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, streamId)
      bindString(2, categoryCreatorId)
    }

    override fun toString(): String = "Movie.sq:findExistingMovie"
  }

  private inner class SelectAllForSyncQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_169_808_748,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Movie.sq:selectAllForSync"
  }

  private inner class SelectAllLimitedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_024_121_346,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? GROUP BY streamId ORDER BY name ASC LIMIT ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "Movie.sq:selectAllLimited"
  }

  private inner class SelectByCategoryLimitedQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Movie", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Movie", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-102_654_468,
        """SELECT Movie.movieId, Movie.name, Movie.streamId, Movie.streamIcon, Movie.rating, Movie.added, Movie.categoryCreatorId, Movie.containerExtension, Movie.isFavorite, Movie.userId, Movie.lastViewedTimestamp, Movie.lastUpdated, Movie.lastSeen, Movie.contentHash, Movie.syncVersion FROM Movie WHERE userId = ? AND categoryCreatorId = ? GROUP BY streamId ORDER BY name ASC LIMIT ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
      bindLong(2, value)
    }

    override fun toString(): String = "Movie.sq:selectByCategoryLimited"
  }
}
