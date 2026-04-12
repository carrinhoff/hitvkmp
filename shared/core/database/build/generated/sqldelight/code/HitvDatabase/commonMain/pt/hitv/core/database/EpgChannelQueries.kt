package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class EpgChannelQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectByChannelIdAndUserId(
    channel_id: String,
    userId: Long,
    mapper: (
      channel_id: String,
      display_name: String?,
      logo: String?,
      userId: Long,
    ) -> T,
  ): Query<T> = SelectByChannelIdAndUserIdQuery(channel_id, userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getLong(3)!!
    )
  }

  public fun selectByChannelIdAndUserId(channel_id: String, userId: Long): Query<EpgChannel> =
      selectByChannelIdAndUserId(channel_id, userId) { channel_id_, display_name, logo, userId_ ->
    EpgChannel(
      channel_id_,
      display_name,
      logo,
      userId_
    )
  }

  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    channel_id: String,
    display_name: String?,
    logo: String?,
    userId: Long,
  ) -> T): Query<T> = SelectAllByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getString(0)!!,
      cursor.getString(1),
      cursor.getString(2),
      cursor.getLong(3)!!
    )
  }

  public fun selectAllByUserId(userId: Long): Query<EpgChannel> = selectAllByUserId(userId) {
      channel_id, display_name, logo, userId_ ->
    EpgChannel(
      channel_id,
      display_name,
      logo,
      userId_
    )
  }

  public fun insertOrReplace(
    channel_id: String,
    display_name: String?,
    logo: String?,
    userId: Long,
  ) {
    driver.execute(96_947_282, """
        |INSERT OR REPLACE INTO EpgChannel(channel_id, display_name, logo, userId)
        |VALUES (?, ?, ?, ?)
        """.trimMargin(), 4) {
          bindString(0, channel_id)
          bindString(1, display_name)
          bindString(2, logo)
          bindLong(3, userId)
        }
    notifyQueries(96_947_282) { emit ->
      emit("EpgChannel")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(232_321_358, """DELETE FROM EpgChannel WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(232_321_358) { emit ->
      emit("EpgChannel")
      emit("Programme")
    }
  }

  private inner class SelectByChannelIdAndUserIdQuery<out T : Any>(
    public val channel_id: String,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("EpgChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("EpgChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-8_681_736,
        """SELECT EpgChannel.channel_id, EpgChannel.display_name, EpgChannel.logo, EpgChannel.userId FROM EpgChannel WHERE channel_id = ? AND userId = ?""",
        mapper, 2) {
      bindString(0, channel_id)
      bindLong(1, userId)
    }

    override fun toString(): String = "EpgChannel.sq:selectByChannelIdAndUserId"
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("EpgChannel", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("EpgChannel", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(767_636_700,
        """SELECT EpgChannel.channel_id, EpgChannel.display_name, EpgChannel.logo, EpgChannel.userId FROM EpgChannel WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "EpgChannel.sq:selectAllByUserId"
  }
}
