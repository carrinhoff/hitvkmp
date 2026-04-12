package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ProgrammeQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectChannelWithProgrammes(
    display_name: String?,
    userId: Long,
    start_time: Long,
    mapper: (
      channel_id: String,
      display_name: String?,
      logo: String?,
      userId: Long,
      id: Long,
      channel_name: String?,
      start_time: Long,
      end_time: Long,
      userId_: Long,
      imageUrl: String?,
    ) -> T,
  ): Query<T> = SelectChannelWithProgrammesQuery(display_name, userId, start_time) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9)
    )
  }

  public fun selectChannelWithProgrammes(
    display_name: String?,
    userId: Long,
    start_time: Long,
  ): Query<SelectChannelWithProgrammes> = selectChannelWithProgrammes(display_name, userId,
      start_time) { channel_id, display_name_, logo, userId_, id, channel_name, start_time_,
      end_time, userId__, imageUrl ->
    SelectChannelWithProgrammes(
      channel_id,
      display_name_,
      logo,
      userId_,
      id,
      channel_name,
      start_time_,
      end_time,
      userId__,
      imageUrl
    )
  }

  public fun <T : Any> selectChannelWithProgrammeDetails(
    channel_id: String,
    userId: Long,
    start_time: Long,
    mapper: (
      channel_id: String,
      display_name: String?,
      logo: String?,
      userId: Long,
      id: Long,
      channel_name: String?,
      start_time: Long,
      end_time: Long,
      userId_: Long,
      imageUrl: String?,
      title_id: Long,
      title: String?,
      programme_id: Long?,
      userId__: Long,
      desc_id: Long,
      desc: String?,
      programme_id_: Long?,
      userId___: Long,
    ) -> T,
  ): Query<T> = SelectChannelWithProgrammeDetailsQuery(channel_id, userId, start_time) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getString(5),
      cursor.getLong(6)!!,
      cursor.getLong(7)!!,
      cursor.getLong(8)!!,
      cursor.getString(9),
      cursor.getLong(10)!!,
      cursor.getString(11),
      cursor.getLong(12),
      cursor.getLong(13)!!,
      cursor.getLong(14)!!,
      cursor.getString(15),
      cursor.getLong(16),
      cursor.getLong(17)!!
    )
  }

  public fun selectChannelWithProgrammeDetails(
    channel_id: String,
    userId: Long,
    start_time: Long,
  ): Query<SelectChannelWithProgrammeDetails> = selectChannelWithProgrammeDetails(channel_id,
      userId, start_time) { channel_id_, display_name, logo, userId_, id, channel_name, start_time_,
      end_time, userId__, imageUrl, title_id, title, programme_id, userId___, desc_id, desc,
      programme_id_, userId____ ->
    SelectChannelWithProgrammeDetails(
      channel_id_,
      display_name,
      logo,
      userId_,
      id,
      channel_name,
      start_time_,
      end_time,
      userId__,
      imageUrl,
      title_id,
      title,
      programme_id,
      userId___,
      desc_id,
      desc,
      programme_id_,
      userId____
    )
  }

  public fun <T : Any> selectAllChannelsWithProgrammes(userId: Long, mapper: (
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
    channel_id: String?,
    display_name: String?,
    logo: String?,
    userId_: Long?,
    id: Long?,
    channel_name: String?,
    start_time: Long?,
    end_time: Long?,
    userId__: Long?,
    imageUrl: String?,
    title_id: Long?,
    title: String?,
    programme_id: Long?,
    userId___: Long?,
    desc_id: Long?,
    desc: String?,
    programme_id_: Long?,
    userId____: Long?,
  ) -> T): Query<T> = SelectAllChannelsWithProgrammesQuery(userId) { cursor ->
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
      cursor.getLong(13)!!,
      cursor.getString(14),
      cursor.getString(15),
      cursor.getString(16),
      cursor.getLong(17),
      cursor.getLong(18),
      cursor.getString(19),
      cursor.getLong(20),
      cursor.getLong(21),
      cursor.getLong(22),
      cursor.getString(23),
      cursor.getLong(24),
      cursor.getString(25),
      cursor.getLong(26),
      cursor.getLong(27),
      cursor.getLong(28),
      cursor.getString(29),
      cursor.getLong(30),
      cursor.getLong(31)
    )
  }

  public fun selectAllChannelsWithProgrammes(userId: Long): Query<SelectAllChannelsWithProgrammes> =
      selectAllChannelsWithProgrammes(userId) { channelId, name, streamUrl, streamIcon,
      epgChannelId, categoryCreatorId, isFavorite, licenseKey, userId_, lastViewedTimestamp,
      lastUpdated, lastSeen, contentHash, syncVersion, channel_id, display_name, logo, userId__, id,
      channel_name, start_time, end_time, userId___, imageUrl, title_id, title, programme_id,
      userId____, desc_id, desc, programme_id_, userId_____ ->
    SelectAllChannelsWithProgrammes(
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
      syncVersion,
      channel_id,
      display_name,
      logo,
      userId__,
      id,
      channel_name,
      start_time,
      end_time,
      userId___,
      imageUrl,
      title_id,
      title,
      programme_id,
      userId____,
      desc_id,
      desc,
      programme_id_,
      userId_____
    )
  }

  public fun <T : Any> selectCategoriesWithEpgCounts(
    userId: Long,
    end_time: Long,
    start_time: Long,
    mapper: (
      categoryId: Long,
      categoryName: String,
      channelCount: Long,
    ) -> T,
  ): Query<T> = SelectCategoriesWithEpgCountsQuery(userId, end_time, start_time) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getLong(2)!!
    )
  }

  public fun selectCategoriesWithEpgCounts(
    userId: Long,
    end_time: Long,
    start_time: Long,
  ): Query<SelectCategoriesWithEpgCounts> = selectCategoriesWithEpgCounts(userId, end_time,
      start_time) { categoryId, categoryName, channelCount ->
    SelectCategoriesWithEpgCounts(
      categoryId,
      categoryName,
      channelCount
    )
  }

  public fun <T : Any> selectProgrammesForCategory(
    categoryCreatorId: String,
    userId: Long,
    end_time: Long,
    start_time: Long,
    mapper: (
      channel_id: String,
      logo: String?,
      display_name: String?,
      channel_name: String,
      start_time: Long,
      end_time: Long,
      id: Long,
      title: String?,
      programme_id: Long,
      title_id: Long?,
      description: String?,
      desc_id: Long?,
    ) -> T,
  ): Query<T> = SelectProgrammesForCategoryQuery(categoryCreatorId, userId, end_time, start_time) {
      cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getString(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!,
      cursor.getString(7),
      cursor.getLong(8)!!,
      cursor.getLong(9),
      cursor.getString(10),
      cursor.getLong(11)
    )
  }

  public fun selectProgrammesForCategory(
    categoryCreatorId: String,
    userId: Long,
    end_time: Long,
    start_time: Long,
  ): Query<SelectProgrammesForCategory> = selectProgrammesForCategory(categoryCreatorId, userId,
      end_time, start_time) { channel_id, logo, display_name, channel_name, start_time_, end_time_,
      id, title, programme_id, title_id, description, desc_id ->
    SelectProgrammesForCategory(
      channel_id,
      logo,
      display_name,
      channel_name,
      start_time_,
      end_time_,
      id,
      title,
      programme_id,
      title_id,
      description,
      desc_id
    )
  }

  public fun <T : Any> lastInsertProgrammeId(mapper: (MAX: Long?) -> T): Query<T> =
      Query(-2_017_412_881, arrayOf("Programme"), driver, "Programme.sq", "lastInsertProgrammeId",
      "SELECT MAX(id) FROM Programme") { cursor ->
    mapper(
      cursor.getLong(0)
    )
  }

  public fun lastInsertProgrammeId(): Query<LastInsertProgrammeId> = lastInsertProgrammeId { MAX ->
    LastInsertProgrammeId(
      MAX
    )
  }

  public fun insertProgramme(
    channel_name: String?,
    start_time: Long,
    end_time: Long,
    userId: Long,
    imageUrl: String?,
  ) {
    driver.execute(-310_173_270, """
        |INSERT OR REPLACE INTO Programme(channel_name, start_time, end_time, userId, imageUrl)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindString(0, channel_name)
          bindLong(1, start_time)
          bindLong(2, end_time)
          bindLong(3, userId)
          bindString(4, imageUrl)
        }
    notifyQueries(-310_173_270) { emit ->
      emit("Programme")
    }
  }

  public fun insertTitle(
    title: String?,
    programme_id: Long?,
    userId: Long,
  ) {
    driver.execute(-1_010_098_202, """
        |INSERT OR IGNORE INTO Title(title, programme_id, userId)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
          bindString(0, title)
          bindLong(1, programme_id)
          bindLong(2, userId)
        }
    notifyQueries(-1_010_098_202) { emit ->
      emit("Title")
    }
  }

  public fun insertDescription(
    desc: String?,
    programme_id: Long?,
    userId: Long,
  ) {
    driver.execute(-164_651_254, """
        |INSERT OR IGNORE INTO Description(desc, programme_id, userId)
        |VALUES (?, ?, ?)
        """.trimMargin(), 3) {
          bindString(0, desc)
          bindLong(1, programme_id)
          bindLong(2, userId)
        }
    notifyQueries(-164_651_254) { emit ->
      emit("Description")
    }
  }

  public fun deleteProgrammesByUserId(userId: Long) {
    driver.execute(-834_776_296, """DELETE FROM Programme WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-834_776_296) { emit ->
      emit("Description")
      emit("Programme")
      emit("Title")
    }
  }

  public fun deleteTitlesByUserId(userId: Long) {
    driver.execute(144_023_900,
        """DELETE FROM Title WHERE programme_id IN (SELECT id FROM Programme WHERE userId = ?)""",
        1) {
          bindLong(0, userId)
        }
    notifyQueries(144_023_900) { emit ->
      emit("Title")
    }
  }

  public fun deleteDescriptionsByUserId(userId: Long) {
    driver.execute(1_863_847_224,
        """DELETE FROM Description WHERE programme_id IN (SELECT id FROM Programme WHERE userId = ?)""",
        1) {
          bindLong(0, userId)
        }
    notifyQueries(1_863_847_224) { emit ->
      emit("Description")
    }
  }

  public fun deleteEpgChannelsByUserId(userId: Long) {
    driver.execute(1_871_439_749, """DELETE FROM EpgChannel WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(1_871_439_749) { emit ->
      emit("EpgChannel")
      emit("Programme")
    }
  }

  private inner class SelectChannelWithProgrammesQuery<out T : Any>(
    public val display_name: String?,
    public val userId: Long,
    public val start_time: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("EpgChannel", "Programme", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("EpgChannel", "Programme", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(null, """
    |SELECT EpgChannel.channel_id, EpgChannel.display_name, EpgChannel.logo, EpgChannel.userId, Programme.id, Programme.channel_name, Programme.start_time, Programme.end_time, Programme.userId, Programme.imageUrl FROM EpgChannel AS epgc
    |INNER JOIN Programme AS pg ON epgc.channel_id = pg.channel_name
    |WHERE epgc.display_name ${ if (display_name == null) "IS" else "=" } ? AND pg.userId = ?
    |AND ? BETWEEN pg.start_time AND pg.end_time
    """.trimMargin(), mapper, 3) {
      bindString(0, display_name)
      bindLong(1, userId)
      bindLong(2, start_time)
    }

    override fun toString(): String = "Programme.sq:selectChannelWithProgrammes"
  }

  private inner class SelectChannelWithProgrammeDetailsQuery<out T : Any>(
    public val channel_id: String,
    public val userId: Long,
    public val start_time: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("EpgChannel", "Programme", "Title", "Description", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("EpgChannel", "Programme", "Title", "Description", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(919_776_506, """
    |SELECT EpgChannel.channel_id, EpgChannel.display_name, EpgChannel.logo, EpgChannel.userId, Programme.id, Programme.channel_name, Programme.start_time, Programme.end_time, Programme.userId, Programme.imageUrl, Title.title_id, Title.title, Title.programme_id, Title.userId, Description.desc_id, Description.desc, Description.programme_id, Description.userId FROM EpgChannel AS epgc
    |INNER JOIN Programme AS pg ON epgc.channel_id = pg.channel_name
    |INNER JOIN Title AS t ON t.programme_id = pg.id
    |INNER JOIN Description AS d ON d.programme_id = pg.id
    |WHERE epgc.channel_id = ? AND pg.userId = ?
    |AND ? BETWEEN pg.start_time AND pg.end_time
    """.trimMargin(), mapper, 3) {
      bindString(0, channel_id)
      bindLong(1, userId)
      bindLong(2, start_time)
    }

    override fun toString(): String = "Programme.sq:selectChannelWithProgrammeDetails"
  }

  private inner class SelectAllChannelsWithProgrammesQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Channel", "EpgChannel", "Programme", "Title", "Description", listener =
          listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Channel", "EpgChannel", "Programme", "Title", "Description", listener =
          listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(2_137_627_353, """
    |SELECT Channel.channelId, Channel.name, Channel.streamUrl, Channel.streamIcon, Channel.epgChannelId, Channel.categoryCreatorId, Channel.isFavorite, Channel.licenseKey, Channel.userId, Channel.lastViewedTimestamp, Channel.lastUpdated, Channel.lastSeen, Channel.contentHash, Channel.syncVersion, EpgChannel.channel_id, EpgChannel.display_name, EpgChannel.logo, EpgChannel.userId, Programme.id, Programme.channel_name, Programme.start_time, Programme.end_time, Programme.userId, Programme.imageUrl, Title.title_id, Title.title, Title.programme_id, Title.userId, Description.desc_id, Description.desc, Description.programme_id, Description.userId FROM Channel AS ch
    |LEFT JOIN EpgChannel AS epgc ON epgc.channel_id = ch.epgChannelId AND epgc.userId = ch.userId
    |LEFT JOIN Programme AS pg ON pg.channel_name = epgc.channel_id AND pg.userId = ch.userId
    |LEFT JOIN Title AS t ON t.programme_id = pg.id
    |LEFT JOIN Description AS d ON d.programme_id = pg.id
    |WHERE ch.userId = ?
    """.trimMargin(), mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Programme.sq:selectAllChannelsWithProgrammes"
  }

  private inner class SelectCategoriesWithEpgCountsQuery<out T : Any>(
    public val userId: Long,
    public val end_time: Long,
    public val start_time: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", "EpgChannel", "Channel", "Programme", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", "EpgChannel", "Channel", "Programme", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_776_005_367, """
    |SELECT
    |    c.categoryId,
    |    c.categoryName,
    |    COUNT(DISTINCT epgc.channel_id) AS channelCount
    |FROM Category c
    |INNER JOIN Channel ch ON ch.categoryCreatorId = CAST(c.categoryId AS TEXT) AND ch.userId = c.userId
    |INNER JOIN EpgChannel epgc ON epgc.channel_id = ch.epgChannelId AND epgc.userId = ch.userId
    |WHERE c.userId = ?
    |AND ch.epgChannelId IS NOT NULL
    |AND ch.epgChannelId != ''
    |AND EXISTS (
    |    SELECT 1 FROM Programme pg
    |    WHERE pg.channel_name = epgc.channel_id
    |    AND pg.userId = ch.userId
    |    AND pg.id IS NOT NULL
    |    AND pg.start_time IS NOT NULL
    |    AND pg.end_time IS NOT NULL
    |    AND pg.start_time < pg.end_time
    |    AND pg.end_time > ?
    |    AND pg.start_time < ?
    |)
    |GROUP BY c.categoryId, c.categoryName
    |ORDER BY c.categoryName ASC
    """.trimMargin(), mapper, 3) {
      bindLong(0, userId)
      bindLong(1, end_time)
      bindLong(2, start_time)
    }

    override fun toString(): String = "Programme.sq:selectCategoriesWithEpgCounts"
  }

  private inner class SelectProgrammesForCategoryQuery<out T : Any>(
    public val categoryCreatorId: String,
    public val userId: Long,
    public val end_time: Long,
    public val start_time: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("EpgChannel", "Channel", "Programme", "Title", "Description", listener =
          listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("EpgChannel", "Channel", "Programme", "Title", "Description", listener =
          listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_655_529_147, """
    |SELECT
    |    epgc.channel_id,
    |    epgc.logo,
    |    epgc.display_name,
    |    ch.name AS channel_name,
    |    pg.start_time,
    |    pg.end_time,
    |    pg.id,
    |    t.title AS title,
    |    pg.id AS programme_id,
    |    t.title_id AS title_id,
    |    d.desc AS description,
    |    d.desc_id AS desc_id
    |FROM Channel ch
    |INNER JOIN EpgChannel epgc ON epgc.channel_id = ch.epgChannelId AND epgc.userId = ch.userId
    |INNER JOIN Programme pg ON pg.channel_name = epgc.channel_id AND pg.userId = ch.userId
    |LEFT JOIN Title t ON t.programme_id = pg.id
    |LEFT JOIN Description d ON d.programme_id = pg.id
    |WHERE ch.categoryCreatorId = ?
    |AND ch.userId = ?
    |AND pg.end_time > ?
    |AND pg.start_time < ?
    |ORDER BY epgc.display_name, pg.start_time ASC
    """.trimMargin(), mapper, 4) {
      bindString(0, categoryCreatorId)
      bindLong(1, userId)
      bindLong(2, end_time)
      bindLong(3, start_time)
    }

    override fun toString(): String = "Programme.sq:selectProgrammesForCategory"
  }
}
