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
import pt.hitv.core.database.channel.SelectRecentlyViewed
import pt.hitv.core.database.channel.SelectRecentlyViewedPaged

public class ChannelQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    channelId: Long,
    name: String,
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) -> T): Query<T> = SelectAllByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectAllByUserId(userId: Long): Query<Channel> = selectAllByUserId(userId) {
      channelId, name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectAllPaged(
    userId: Long,
    `value`: Long,
    value_: Long,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectAllPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Channel> = selectAllPaged(userId, value_, value__) { channelId, name, streamUrl,
      streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
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
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectByCategoryPaged(
    userId: Long,
    categoryCreatorId: String,
    value_: Long,
    value__: Long,
  ): Query<Channel> = selectByCategoryPaged(userId, categoryCreatorId, value_, value__) { channelId,
      name, streamUrl, streamIcon, epgChannelId, categoryCreatorId_, isFavorite, licenseKey,
      userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId_,
      isFavorite,
      licenseKey,
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
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectFavoritesPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<Channel> = selectFavoritesPaged(userId, value_, value__) { channelId, name, streamUrl,
      streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectRecentlyViewed(userId: Long, mapper: (
    channelId: Long,
    name: String,
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectRecentlyViewed(userId: Long): Query<SelectRecentlyViewed> =
      selectRecentlyViewed(userId) { channelId, name, streamUrl, streamIcon, epgChannelId,
      categoryCreatorId, isFavorite, licenseKey, userId_, lastViewedTimestamp, lastUpdated,
      lastSeen, contentHash, syncVersion ->
    SelectRecentlyViewed(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
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
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectRecentlyViewedPaged(
    userId: Long,
    value_: Long,
    value__: Long,
  ): Query<SelectRecentlyViewedPaged> = selectRecentlyViewedPaged(userId, value_, value__) {
      channelId, name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    SelectRecentlyViewedPaged(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
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
    value__: Long,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SearchByNameQuery(userId, value, value_, value__) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun searchByName(
    userId: Long,
    value_: String?,
    value__: Long,
    value___: Long,
  ): Query<Channel> = searchByName(userId, value_, value__, value___) { channelId, name, streamUrl,
      streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectByName(
    name: String,
    userId: Long,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByNameQuery(name, userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectByName(name: String, userId: Long): Query<Channel> = selectByName(name, userId) {
      channelId, name_, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name_,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectByEpgId(
    `value`: String?,
    userId: Long,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = SelectByEpgIdQuery(value, userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectByEpgId(value_: String?, userId: Long): Query<Channel> = selectByEpgId(value_,
      userId) { channelId, name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun selectFavoriteStatus(
    name: String,
    categoryCreatorId: String,
    userId: Long,
  ): Query<Long> = SelectFavoriteStatusQuery(name, categoryCreatorId, userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> findExistingChannel(
    userId: Long,
    name: String,
    categoryCreatorId: String,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
      userId: Long,
      lastViewedTimestamp: Long,
      lastUpdated: Long,
      lastSeen: Long,
      contentHash: String?,
      syncVersion: Long,
    ) -> T,
  ): Query<T> = FindExistingChannelQuery(userId, name, categoryCreatorId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun findExistingChannel(
    userId: Long,
    name: String,
    categoryCreatorId: String,
  ): Query<Channel> = findExistingChannel(userId, name, categoryCreatorId) { channelId, name_,
      streamUrl, streamIcon, epgChannelId, categoryCreatorId_, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name_,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId_,
      isFavorite,
      licenseKey,
      userId_,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectAllForSync(userId: Long, mapper: (
    channelId: Long,
    name: String,
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectAllForSync(userId: Long): Query<Channel> = selectAllForSync(userId) { channelId,
      name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
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

  public fun countRecentChannels(userId: Long, lastSeen: Long): Query<Long> =
      CountRecentChannelsQuery(userId, lastSeen) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAllLimited(
    userId: Long,
    `value`: Long,
    mapper: (
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectAllLimited(userId: Long, value_: Long): Query<Channel> = selectAllLimited(userId,
      value_) { channelId, name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId_, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
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
      channelId: Long,
      name: String,
      streamUrl: String,
      streamIcon: String,
      epgChannelId: String?,
      categoryCreatorId: String,
      isFavorite: Long,
      licenseKey: String?,
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
      cursor.getString(4),
      cursor.getString(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9)!!,
      cursor.getLong(10)!!,
      cursor.getLong(11)!!,
      cursor.getString(12),
      cursor.getLong(13)!!
    )
  }

  public fun selectByCategoryLimited(
    userId: Long,
    categoryCreatorId: String,
    value_: Long,
  ): Query<Channel> = selectByCategoryLimited(userId, categoryCreatorId, value_) { channelId, name,
      streamUrl, streamIcon, epgChannelId, categoryCreatorId_, isFavorite, licenseKey, userId_,
      lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId_,
      isFavorite,
      licenseKey,
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
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(-2_062_402_344, """
        |INSERT OR REPLACE INTO Channel(name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 13) {
          bindString(0, name)
          bindString(1, streamUrl)
          bindString(2, streamIcon)
          bindString(3, epgChannelId)
          bindString(4, categoryCreatorId)
          bindLong(5, isFavorite)
          bindString(6, licenseKey)
          bindLong(7, userId)
          bindLong(8, lastViewedTimestamp)
          bindLong(9, lastUpdated)
          bindLong(10, lastSeen)
          bindString(11, contentHash)
          bindLong(12, syncVersion)
        }
    notifyQueries(-2_062_402_344) { emit ->
      emit("Channel")
    }
  }

  public fun insertOrIgnore(
    name: String,
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
    userId: Long,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
  ) {
    driver.execute(-183_853_266, """
        |INSERT OR IGNORE INTO Channel(name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 13) {
          bindString(0, name)
          bindString(1, streamUrl)
          bindString(2, streamIcon)
          bindString(3, epgChannelId)
          bindString(4, categoryCreatorId)
          bindLong(5, isFavorite)
          bindString(6, licenseKey)
          bindLong(7, userId)
          bindLong(8, lastViewedTimestamp)
          bindLong(9, lastUpdated)
          bindLong(10, lastSeen)
          bindString(11, contentHash)
          bindLong(12, syncVersion)
        }
    notifyQueries(-183_853_266) { emit ->
      emit("Channel")
    }
  }

  public fun updateFavorite(
    isFavorite: Long,
    name: String,
    userId: Long,
    categoryCreatorId: String,
  ) {
    driver.execute(-49_083_067,
        """UPDATE Channel SET isFavorite = ? WHERE name = ? AND userId = ? AND categoryCreatorId = ?""",
        4) {
          bindLong(0, isFavorite)
          bindString(1, name)
          bindLong(2, userId)
          bindString(3, categoryCreatorId)
        }
    notifyQueries(-49_083_067) { emit ->
      emit("Channel")
    }
  }

  public fun updateLastViewedTimestamp(
    lastViewedTimestamp: Long,
    name: String,
    userId: Long,
    categoryCreatorId: String,
  ) {
    driver.execute(-341_505_357,
        """UPDATE Channel SET lastViewedTimestamp = ? WHERE name = ? AND userId = ? AND categoryCreatorId = ?""",
        4) {
          bindLong(0, lastViewedTimestamp)
          bindString(1, name)
          bindLong(2, userId)
          bindString(3, categoryCreatorId)
        }
    notifyQueries(-341_505_357) { emit ->
      emit("Channel")
    }
  }

  public fun updateById(
    name: String,
    streamUrl: String,
    streamIcon: String,
    epgChannelId: String?,
    categoryCreatorId: String,
    isFavorite: Long,
    licenseKey: String?,
    lastViewedTimestamp: Long,
    lastUpdated: Long,
    lastSeen: Long,
    contentHash: String?,
    syncVersion: Long,
    channelId: Long,
  ) {
    driver.execute(1_075_698_683, """
        |UPDATE Channel SET name = ?, streamUrl = ?, streamIcon = ?, epgChannelId = ?, categoryCreatorId = ?, isFavorite = ?, licenseKey = ?, lastViewedTimestamp = ?, lastUpdated = ?, lastSeen = ?, contentHash = ?, syncVersion = ?
        |WHERE channelId = ?
        """.trimMargin(), 13) {
          bindString(0, name)
          bindString(1, streamUrl)
          bindString(2, streamIcon)
          bindString(3, epgChannelId)
          bindString(4, categoryCreatorId)
          bindLong(5, isFavorite)
          bindString(6, licenseKey)
          bindLong(7, lastViewedTimestamp)
          bindLong(8, lastUpdated)
          bindLong(9, lastSeen)
          bindString(10, contentHash)
          bindLong(11, syncVersion)
          bindLong(12, channelId)
        }
    notifyQueries(1_075_698_683) { emit ->
      emit("Channel")
    }
  }

  public fun markAsSeen(
    lastSeen: Long,
    userId: Long,
    channelId: Collection<Long>,
  ) {
    val channelIdIndexes = createArguments(count = channelId.size)
    driver.execute(null,
        """UPDATE Channel SET lastSeen = ? WHERE userId = ? AND channelId IN $channelIdIndexes""",
        2 + channelId.size) {
          bindLong(0, lastSeen)
          bindLong(1, userId)
          channelId.forEachIndexed { index, channelId_ ->
            bindLong(index + 2, channelId_)
          }
        }
    notifyQueries(1_457_793_722) { emit ->
      emit("Channel")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(1_825_232_904, """DELETE FROM Channel WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(1_825_232_904) { emit ->
      emit("Channel")
      emit("CustomGroupChannel")
    }
  }

  public fun deleteStale(userId: Long, lastSeen: Long) {
    driver.execute(-1_545_881_362, """DELETE FROM Channel WHERE userId = ? AND lastSeen < ?""", 2) {
          bindLong(0, userId)
          bindLong(1, lastSeen)
        }
    notifyQueries(-1_545_881_362) { emit ->
      emit("Channel")
      emit("CustomGroupChannel")
    }
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(101_850_082,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? ORDER BY name ASC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Channel.sq:selectAllByUserId"
  }

  private inner class SelectAllPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(766_099_312,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Channel.sq:selectAllPaged"
  }

  private inner class SelectByCategoryPagedQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_940_550_172,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND categoryCreatorId = ? ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 4) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
      bindLong(2, value)
      bindLong(3, value_)
    }

    override fun toString(): String = "Channel.sq:selectByCategoryPaged"
  }

  private inner class SelectFavoritesPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_770_109_818,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND isFavorite = 1 ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Channel.sq:selectFavoritesPaged"
  }

  private inner class SelectRecentlyViewedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_753_673_432,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 ORDER BY lastViewedTimestamp DESC LIMIT 10""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Channel.sq:selectRecentlyViewed"
  }

  private inner class SelectRecentlyViewedPagedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_002_806_637,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND lastViewedTimestamp IS NOT NULL AND lastViewedTimestamp != 0 ORDER BY lastViewedTimestamp DESC LIMIT ? OFFSET ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "Channel.sq:selectRecentlyViewedPaged"
  }

  private inner class SearchByNameQuery<out T : Any>(
    public val userId: Long,
    public val `value`: String?,
    public val value_: Long,
    public val value__: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-279_979_254,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND LOWER(name) LIKE LOWER(?) ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 4) {
      bindLong(0, userId)
      bindString(1, value)
      bindLong(2, value_)
      bindLong(3, value__)
    }

    override fun toString(): String = "Channel.sq:searchByName"
  }

  private inner class SelectByNameQuery<out T : Any>(
    public val name: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_640_553_310,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE name = ? AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindString(0, name)
      bindLong(1, userId)
    }

    override fun toString(): String = "Channel.sq:selectByName"
  }

  private inner class SelectByEpgIdQuery<out T : Any>(
    public val `value`: String?,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-690_326_300,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE LOWER(TRIM(epgChannelId)) = LOWER(TRIM(?)) AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindString(0, value)
      bindLong(1, userId)
    }

    override fun toString(): String = "Channel.sq:selectByEpgId"
  }

  private inner class SelectFavoriteStatusQuery<out T : Any>(
    public val name: String,
    public val categoryCreatorId: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(887_236_746,
        """SELECT isFavorite FROM Channel WHERE name = ? AND categoryCreatorId = ? AND userId = ? LIMIT 1""",
        mapper, 3) {
      bindString(0, name)
      bindString(1, categoryCreatorId)
      bindLong(2, userId)
    }

    override fun toString(): String = "Channel.sq:selectFavoriteStatus"
  }

  private inner class FindExistingChannelQuery<out T : Any>(
    public val userId: Long,
    public val name: String,
    public val categoryCreatorId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_967_034_817,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND name = ? AND categoryCreatorId = ? LIMIT 1""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, name)
      bindString(2, categoryCreatorId)
    }

    override fun toString(): String = "Channel.sq:findExistingChannel"
  }

  private inner class SelectAllForSyncQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_907_383_295,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Channel.sq:selectAllForSync"
  }

  private inner class CountByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_084_880_916, """SELECT COUNT(*) FROM Channel WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Channel.sq:countByUserId"
  }

  private inner class CountByCategoryAndUserIdQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_002_732_089,
        """SELECT COUNT(*) FROM Channel WHERE userId = ? AND categoryCreatorId = ?""", mapper, 2) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
    }

    override fun toString(): String = "Channel.sq:countByCategoryAndUserId"
  }

  private inner class CountRecentChannelsQuery<out T : Any>(
    public val userId: Long,
    public val lastSeen: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(146_813_562,
        """SELECT COUNT(*) FROM Channel WHERE userId = ? AND lastSeen >= ?""", mapper, 2) {
      bindLong(0, userId)
      bindLong(1, lastSeen)
    }

    override fun toString(): String = "Channel.sq:countRecentChannels"
  }

  private inner class SelectAllLimitedQuery<out T : Any>(
    public val userId: Long,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_533_271_403,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? ORDER BY name ASC LIMIT ?""",
        mapper, 2) {
      bindLong(0, userId)
      bindLong(1, value)
    }

    override fun toString(): String = "Channel.sq:selectAllLimited"
  }

  private inner class SelectByCategoryLimitedQuery<out T : Any>(
    public val userId: Long,
    public val categoryCreatorId: String,
    public val `value`: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(126_755_849,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel WHERE userId = ? AND categoryCreatorId = ? ORDER BY name ASC LIMIT ?""",
        mapper, 3) {
      bindLong(0, userId)
      bindString(1, categoryCreatorId)
      bindLong(2, value)
    }

    override fun toString(): String = "Channel.sq:selectByCategoryLimited"
  }
}
