package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Boolean
import kotlin.Long
import kotlin.String

public class CustomGroupQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllGroups(mapper: (
    groupId: Long,
    groupName: String,
    groupIcon: String?,
    createdAt: Long,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = Query(-2_109_490_546, arrayOf("CustomGroup"), driver, "CustomGroup.sq",
      "selectAllGroups",
      "SELECT CustomGroup.groupId, CustomGroup.groupName, CustomGroup.groupIcon, CustomGroup.createdAt, CustomGroup.updatedAt, CustomGroup.sortOrder, CustomGroup.isPinned, CustomGroup.isHidden, CustomGroup.isDefault FROM CustomGroup ORDER BY sortOrder ASC, groupName ASC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectAllGroups(): Query<CustomGroup> = selectAllGroups { groupId, groupName,
      groupIcon, createdAt, updatedAt, sortOrder, isPinned, isHidden, isDefault ->
    CustomGroup(
      groupId,
      groupName,
      groupIcon,
      createdAt,
      updatedAt,
      sortOrder,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectAllGroupsSorted(mapper: (
    groupId: Long,
    groupName: String,
    groupIcon: String?,
    createdAt: Long,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = Query(-1_229_383_957, arrayOf("CustomGroup"), driver, "CustomGroup.sq",
      "selectAllGroupsSorted",
      "SELECT CustomGroup.groupId, CustomGroup.groupName, CustomGroup.groupIcon, CustomGroup.createdAt, CustomGroup.updatedAt, CustomGroup.sortOrder, CustomGroup.isPinned, CustomGroup.isHidden, CustomGroup.isDefault FROM CustomGroup ORDER BY isPinned DESC, isHidden ASC, sortOrder ASC, groupName ASC") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectAllGroupsSorted(): Query<CustomGroup> = selectAllGroupsSorted { groupId,
      groupName, groupIcon, createdAt, updatedAt, sortOrder, isPinned, isHidden, isDefault ->
    CustomGroup(
      groupId,
      groupName,
      groupIcon,
      createdAt,
      updatedAt,
      sortOrder,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectGroupById(groupId: Long, mapper: (
    groupId: Long,
    groupName: String,
    groupIcon: String?,
    createdAt: Long,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = SelectGroupByIdQuery(groupId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectGroupById(groupId: Long): Query<CustomGroup> = selectGroupById(groupId) {
      groupId_, groupName, groupIcon, createdAt, updatedAt, sortOrder, isPinned, isHidden,
      isDefault ->
    CustomGroup(
      groupId_,
      groupName,
      groupIcon,
      createdAt,
      updatedAt,
      sortOrder,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun countGroups(): Query<Long> = Query(-883_887_208, arrayOf("CustomGroup"), driver,
      "CustomGroup.sq", "countGroups", "SELECT COUNT(*) FROM CustomGroup") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectDefaultGroup(mapper: (
    groupId: Long,
    groupName: String,
    groupIcon: String?,
    createdAt: Long,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = Query(-155_930_011, arrayOf("CustomGroup"), driver, "CustomGroup.sq",
      "selectDefaultGroup",
      "SELECT CustomGroup.groupId, CustomGroup.groupName, CustomGroup.groupIcon, CustomGroup.createdAt, CustomGroup.updatedAt, CustomGroup.sortOrder, CustomGroup.isPinned, CustomGroup.isHidden, CustomGroup.isDefault FROM CustomGroup WHERE isDefault = 1 LIMIT 1") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!
    )
  }

  public fun selectDefaultGroup(): Query<CustomGroup> = selectDefaultGroup { groupId, groupName,
      groupIcon, createdAt, updatedAt, sortOrder, isPinned, isHidden, isDefault ->
    CustomGroup(
      groupId,
      groupName,
      groupIcon,
      createdAt,
      updatedAt,
      sortOrder,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> lastInsertGroupId(mapper: (MAX: Long?) -> T): Query<T> = Query(-741_240_800,
      arrayOf("CustomGroup"), driver, "CustomGroup.sq", "lastInsertGroupId",
      "SELECT MAX(groupId) FROM CustomGroup") { cursor ->
    mapper(
      cursor.getLong(0)
    )
  }

  public fun lastInsertGroupId(): Query<LastInsertGroupId> = lastInsertGroupId { MAX ->
    LastInsertGroupId(
      MAX
    )
  }

  public fun <T : Any> selectChannelsInGroup(groupId: Long, mapper: (
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
  ) -> T): Query<T> = SelectChannelsInGroupQuery(groupId) { cursor ->
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

  public fun selectChannelsInGroup(groupId: Long): Query<Channel> = selectChannelsInGroup(groupId) {
      channelId, name, streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite,
      licenseKey, userId, lastViewedTimestamp, lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun <T : Any> selectChannelsInGroupPaged(
    groupId: Long,
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
  ): Query<T> = SelectChannelsInGroupPagedQuery(groupId, value, value_) { cursor ->
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

  public fun selectChannelsInGroupPaged(
    groupId: Long,
    value_: Long,
    value__: Long,
  ): Query<Channel> = selectChannelsInGroupPaged(groupId, value_, value__) { channelId, name,
      streamUrl, streamIcon, epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId,
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
      userId,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun countChannelsInGroup(groupId: Long): Query<Long> = CountChannelsInGroupQuery(groupId) {
      cursor ->
    cursor.getLong(0)!!
  }

  public fun isChannelInGroup(groupId: Long, channelId: Long): Query<Boolean> =
      IsChannelInGroupQuery(groupId, channelId) { cursor ->
    cursor.getBoolean(0)!!
  }

  public fun <T : Any> selectGroupChannels(groupId: Long, mapper: (
    id: Long,
    groupId: Long,
    channelId: Long,
    channelUserId: Long,
    position: Long,
    addedAt: Long,
  ) -> T): Query<T> = SelectGroupChannelsQuery(groupId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectGroupChannels(groupId: Long): Query<CustomGroupChannel> =
      selectGroupChannels(groupId) { id, groupId_, channelId, channelUserId, position, addedAt ->
    CustomGroupChannel(
      id,
      groupId_,
      channelId,
      channelUserId,
      position,
      addedAt
    )
  }

  public fun <T : Any> selectAllGroupChannels(mapper: (
    id: Long,
    groupId: Long,
    channelId: Long,
    channelUserId: Long,
    position: Long,
    addedAt: Long,
  ) -> T): Query<T> = Query(-453_686_923, arrayOf("CustomGroupChannel"), driver, "CustomGroup.sq",
      "selectAllGroupChannels",
      "SELECT CustomGroupChannel.id, CustomGroupChannel.groupId, CustomGroupChannel.channelId, CustomGroupChannel.channelUserId, CustomGroupChannel.position, CustomGroupChannel.addedAt FROM CustomGroupChannel") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getLong(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectAllGroupChannels(): Query<CustomGroupChannel> = selectAllGroupChannels { id,
      groupId, channelId, channelUserId, position, addedAt ->
    CustomGroupChannel(
      id,
      groupId,
      channelId,
      channelUserId,
      position,
      addedAt
    )
  }

  public fun countAllGroupChannels(): Query<Long> = Query(396_584_978,
      arrayOf("CustomGroupChannel"), driver, "CustomGroup.sq", "countAllGroupChannels",
      "SELECT COUNT(*) FROM CustomGroupChannel") { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAllChannelsPaged(
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
  ): Query<T> = SelectAllChannelsPagedQuery(value, value_) { cursor ->
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

  public fun selectAllChannelsPaged(value_: Long, value__: Long): Query<Channel> =
      selectAllChannelsPaged(value_, value__) { channelId, name, streamUrl, streamIcon,
      epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId, lastViewedTimestamp,
      lastUpdated, lastSeen, contentHash, syncVersion ->
    Channel(
      channelId,
      name,
      streamUrl,
      streamIcon,
      epgChannelId,
      categoryCreatorId,
      isFavorite,
      licenseKey,
      userId,
      lastViewedTimestamp,
      lastUpdated,
      lastSeen,
      contentHash,
      syncVersion
    )
  }

  public fun countAllChannels(): Query<Long> = Query(1_982_542_157, arrayOf("Channel"), driver,
      "CustomGroup.sq", "countAllChannels", "SELECT COUNT(*) FROM Channel") { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertGroup(
    groupName: String,
    groupIcon: String?,
    createdAt: Long,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) {
    driver.execute(1_704_014_619, """
        |INSERT OR REPLACE INTO CustomGroup(groupName, groupIcon, createdAt, updatedAt, sortOrder, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 8) {
          bindString(0, groupName)
          bindString(1, groupIcon)
          bindLong(2, createdAt)
          bindLong(3, updatedAt)
          bindLong(4, sortOrder)
          bindLong(5, isPinned)
          bindLong(6, isHidden)
          bindLong(7, isDefault)
        }
    notifyQueries(1_704_014_619) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateGroup(
    groupName: String,
    groupIcon: String?,
    updatedAt: Long,
    sortOrder: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
    groupId: Long,
  ) {
    driver.execute(1_233_717_003, """
        |UPDATE CustomGroup SET groupName = ?, groupIcon = ?, updatedAt = ?, sortOrder = ?, isPinned = ?, isHidden = ?, isDefault = ?
        |WHERE groupId = ?
        """.trimMargin(), 8) {
          bindString(0, groupName)
          bindString(1, groupIcon)
          bindLong(2, updatedAt)
          bindLong(3, sortOrder)
          bindLong(4, isPinned)
          bindLong(5, isHidden)
          bindLong(6, isDefault)
          bindLong(7, groupId)
        }
    notifyQueries(1_233_717_003) { emit ->
      emit("CustomGroup")
    }
  }

  public fun deleteGroup(groupId: Long) {
    driver.execute(685_340_521, """DELETE FROM CustomGroup WHERE groupId = ?""", 1) {
          bindLong(0, groupId)
        }
    notifyQueries(685_340_521) { emit ->
      emit("CustomGroup")
      emit("CustomGroupChannel")
    }
  }

  public fun updateGroupSortOrder(sortOrder: Long, groupId: Long) {
    driver.execute(1_471_170_949, """UPDATE CustomGroup SET sortOrder = ? WHERE groupId = ?""", 2) {
          bindLong(0, sortOrder)
          bindLong(1, groupId)
        }
    notifyQueries(1_471_170_949) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateGroupPinStatus(isPinned: Long, groupId: Long) {
    driver.execute(565_509_916, """UPDATE CustomGroup SET isPinned = ? WHERE groupId = ?""", 2) {
          bindLong(0, isPinned)
          bindLong(1, groupId)
        }
    notifyQueries(565_509_916) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateGroupHideStatus(isHidden: Long, groupId: Long) {
    driver.execute(-457_772_737, """UPDATE CustomGroup SET isHidden = ? WHERE groupId = ?""", 2) {
          bindLong(0, isHidden)
          bindLong(1, groupId)
        }
    notifyQueries(-457_772_737) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateGroupDefaultStatus(isDefault: Long, groupId: Long) {
    driver.execute(1_808_027_720, """UPDATE CustomGroup SET isDefault = ? WHERE groupId = ?""", 2) {
          bindLong(0, isDefault)
          bindLong(1, groupId)
        }
    notifyQueries(1_808_027_720) { emit ->
      emit("CustomGroup")
    }
  }

  public fun clearAllDefaults() {
    driver.execute(-273_381_423, """UPDATE CustomGroup SET isDefault = 0""", 0)
    notifyQueries(-273_381_423) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateAllPinStatus(isPinned: Long) {
    driver.execute(-1_158_705_350, """UPDATE CustomGroup SET isPinned = ?""", 1) {
          bindLong(0, isPinned)
        }
    notifyQueries(-1_158_705_350) { emit ->
      emit("CustomGroup")
    }
  }

  public fun updateAllHideStatus(isHidden: Long) {
    driver.execute(1_926_128_865, """UPDATE CustomGroup SET isHidden = ?""", 1) {
          bindLong(0, isHidden)
        }
    notifyQueries(1_926_128_865) { emit ->
      emit("CustomGroup")
    }
  }

  public fun addChannelToGroup(
    groupId: Long,
    channelId: Long,
    channelUserId: Long,
    position: Long,
    addedAt: Long,
  ) {
    driver.execute(-1_883_499_177, """
        |INSERT OR REPLACE INTO CustomGroupChannel(groupId, channelId, channelUserId, position, addedAt)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindLong(0, groupId)
          bindLong(1, channelId)
          bindLong(2, channelUserId)
          bindLong(3, position)
          bindLong(4, addedAt)
        }
    notifyQueries(-1_883_499_177) { emit ->
      emit("CustomGroupChannel")
    }
  }

  public fun removeChannelFromGroupById(groupId: Long, channelId: Long) {
    driver.execute(-93_290_413,
        """DELETE FROM CustomGroupChannel WHERE groupId = ? AND channelId = ?""", 2) {
          bindLong(0, groupId)
          bindLong(1, channelId)
        }
    notifyQueries(-93_290_413) { emit ->
      emit("CustomGroupChannel")
    }
  }

  public fun removeAllChannelsFromGroup(groupId: Long) {
    driver.execute(-1_847_827_053, """DELETE FROM CustomGroupChannel WHERE groupId = ?""", 1) {
          bindLong(0, groupId)
        }
    notifyQueries(-1_847_827_053) { emit ->
      emit("CustomGroupChannel")
    }
  }

  public fun updateChannelPosition(position: Long, id: Long) {
    driver.execute(-765_552_424, """UPDATE CustomGroupChannel SET position = ? WHERE id = ?""", 2) {
          bindLong(0, position)
          bindLong(1, id)
        }
    notifyQueries(-765_552_424) { emit ->
      emit("CustomGroupChannel")
    }
  }

  public fun cleanupOrphanedChannels() {
    driver.execute(418_200_368,
        """DELETE FROM CustomGroupChannel WHERE channelId NOT IN (SELECT channelId FROM Channel)""",
        0)
    notifyQueries(418_200_368) { emit ->
      emit("CustomGroupChannel")
    }
  }

  private inner class SelectGroupByIdQuery<out T : Any>(
    public val groupId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CustomGroup", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CustomGroup", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_478_911_658,
        """SELECT CustomGroup.groupId, CustomGroup.groupName, CustomGroup.groupIcon, CustomGroup.createdAt, CustomGroup.updatedAt, CustomGroup.sortOrder, CustomGroup.isPinned, CustomGroup.isHidden, CustomGroup.isDefault FROM CustomGroup WHERE groupId = ?""",
        mapper, 1) {
      bindLong(0, groupId)
    }

    override fun toString(): String = "CustomGroup.sq:selectGroupById"
  }

  private inner class SelectChannelsInGroupQuery<out T : Any>(
    public val groupId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", "CustomGroupChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", "CustomGroupChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_883_994_979, """
    |SELECT c.channelId, c.name, c.streamUrl, c.streamIcon, c.epgChannelId, c.categoryCreatorId, c.isFavorite, c.licenseKey, c.userId, c.lastViewedTimestamp, c.lastUpdated, c.lastSeen, c.contentHash, c.syncVersion
    |FROM Channel c
    |INNER JOIN CustomGroupChannel cgc ON c.channelId = cgc.channelId
    |WHERE cgc.groupId = ?
    |ORDER BY cgc.position ASC
    """.trimMargin(), mapper, 1) {
      bindLong(0, groupId)
    }

    override fun toString(): String = "CustomGroup.sq:selectChannelsInGroup"
  }

  private inner class SelectChannelsInGroupPagedQuery<out T : Any>(
    public val groupId: Long,
    public val `value`: Long,
    public val value_: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", "CustomGroupChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", "CustomGroupChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_741_805_038, """
    |SELECT c.channelId, c.name, c.streamUrl, c.streamIcon, c.epgChannelId, c.categoryCreatorId, c.isFavorite, c.licenseKey, c.userId, c.lastViewedTimestamp, c.lastUpdated, c.lastSeen, c.contentHash, c.syncVersion
    |FROM Channel c
    |INNER JOIN CustomGroupChannel cgc ON c.channelId = cgc.channelId
    |WHERE cgc.groupId = ?
    |ORDER BY cgc.position ASC
    |LIMIT ? OFFSET ?
    """.trimMargin(), mapper, 3) {
      bindLong(0, groupId)
      bindLong(1, value)
      bindLong(2, value_)
    }

    override fun toString(): String = "CustomGroup.sq:selectChannelsInGroupPaged"
  }

  private inner class CountChannelsInGroupQuery<out T : Any>(
    public val groupId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CustomGroupChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CustomGroupChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_136_618_202,
        """SELECT COUNT(*) FROM CustomGroupChannel WHERE groupId = ?""", mapper, 1) {
      bindLong(0, groupId)
    }

    override fun toString(): String = "CustomGroup.sq:countChannelsInGroup"
  }

  private inner class IsChannelInGroupQuery<out T : Any>(
    public val groupId: Long,
    public val channelId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CustomGroupChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CustomGroupChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(915_598_060,
        """SELECT EXISTS(SELECT 1 FROM CustomGroupChannel WHERE groupId = ? AND channelId = ?)""",
        mapper, 2) {
      bindLong(0, groupId)
      bindLong(1, channelId)
    }

    override fun toString(): String = "CustomGroup.sq:isChannelInGroup"
  }

  private inner class SelectGroupChannelsQuery<out T : Any>(
    public val groupId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CustomGroupChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CustomGroupChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_976_892_760,
        """SELECT CustomGroupChannel.id, CustomGroupChannel.groupId, CustomGroupChannel.channelId, CustomGroupChannel.channelUserId, CustomGroupChannel.position, CustomGroupChannel.addedAt FROM CustomGroupChannel WHERE groupId = ? ORDER BY position ASC""",
        mapper, 1) {
      bindLong(0, groupId)
    }

    override fun toString(): String = "CustomGroup.sq:selectGroupChannels"
  }

  private inner class SelectAllChannelsPagedQuery<out T : Any>(
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
        driver.executeQuery(-1_619_560_501,
        """SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion FROM Channel ORDER BY name ASC LIMIT ? OFFSET ?""",
        mapper, 2) {
      bindLong(0, value)
      bindLong(1, value_)
    }

    override fun toString(): String = "CustomGroup.sq:selectAllChannelsPaged"
  }
}
