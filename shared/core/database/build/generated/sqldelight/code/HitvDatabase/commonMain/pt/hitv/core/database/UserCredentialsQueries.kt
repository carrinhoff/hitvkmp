package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class UserCredentialsQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectByUserId(userId: Long, mapper: (
    userId: Long,
    username: String,
    encryptedPassword: String,
    hostname: String,
    expirationDate: String?,
    epgUrl: String?,
    allowedOutputFormats: String?,
    channelPreviewEnabled: Long,
  ) -> T): Query<T> = SelectByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getLong(7)!!
    )
  }

  public fun selectByUserId(userId: Long): Query<UserCredentials> = selectByUserId(userId) {
      userId_, username, encryptedPassword, hostname, expirationDate, epgUrl, allowedOutputFormats,
      channelPreviewEnabled ->
    UserCredentials(
      userId_,
      username,
      encryptedPassword,
      hostname,
      expirationDate,
      epgUrl,
      allowedOutputFormats,
      channelPreviewEnabled
    )
  }

  public fun <T : Any> selectByUsername(username: String, mapper: (
    userId: Long,
    username: String,
    encryptedPassword: String,
    hostname: String,
    expirationDate: String?,
    epgUrl: String?,
    allowedOutputFormats: String?,
    channelPreviewEnabled: Long,
  ) -> T): Query<T> = SelectByUsernameQuery(username) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getLong(7)!!
    )
  }

  public fun selectByUsername(username: String): Query<UserCredentials> =
      selectByUsername(username) { userId, username_, encryptedPassword, hostname, expirationDate,
      epgUrl, allowedOutputFormats, channelPreviewEnabled ->
    UserCredentials(
      userId,
      username_,
      encryptedPassword,
      hostname,
      expirationDate,
      epgUrl,
      allowedOutputFormats,
      channelPreviewEnabled
    )
  }

  public fun selectUserId(username: String, hostname: String): Query<Long> =
      SelectUserIdQuery(username, hostname) { cursor ->
    cursor.getLong(0)!!
  }

  public fun <T : Any> selectAll(mapper: (
    userId: Long,
    username: String,
    encryptedPassword: String,
    hostname: String,
    expirationDate: String?,
    epgUrl: String?,
    allowedOutputFormats: String?,
    channelPreviewEnabled: Long,
  ) -> T): Query<T> = Query(-1_999_611_945, arrayOf("UserCredentials"), driver,
      "UserCredentials.sq", "selectAll",
      "SELECT UserCredentials.userId, UserCredentials.username, UserCredentials.encryptedPassword, UserCredentials.hostname, UserCredentials.expirationDate, UserCredentials.epgUrl, UserCredentials.allowedOutputFormats, UserCredentials.channelPreviewEnabled FROM UserCredentials") {
      cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getString(1)!!,
      cursor.getString(2)!!,
      cursor.getString(3)!!,
      cursor.getString(4),
      cursor.getString(5),
      cursor.getString(6),
      cursor.getLong(7)!!
    )
  }

  public fun selectAll(): Query<UserCredentials> = selectAll { userId, username, encryptedPassword,
      hostname, expirationDate, epgUrl, allowedOutputFormats, channelPreviewEnabled ->
    UserCredentials(
      userId,
      username,
      encryptedPassword,
      hostname,
      expirationDate,
      epgUrl,
      allowedOutputFormats,
      channelPreviewEnabled
    )
  }

  public fun <T : Any> lastInsertRowId(mapper: (MAX: Long?) -> T): Query<T> = Query(-1_574_461_384,
      arrayOf("UserCredentials"), driver, "UserCredentials.sq", "lastInsertRowId",
      "SELECT MAX(userId) FROM UserCredentials") { cursor ->
    mapper(
      cursor.getLong(0)
    )
  }

  public fun lastInsertRowId(): Query<LastInsertRowId> = lastInsertRowId { MAX ->
    LastInsertRowId(
      MAX
    )
  }

  public fun insert(
    username: String,
    encryptedPassword: String,
    hostname: String,
    expirationDate: String?,
    epgUrl: String?,
    allowedOutputFormats: String?,
    channelPreviewEnabled: Long,
  ) {
    driver.execute(-327_432_409, """
        |INSERT OR IGNORE INTO UserCredentials(username, encryptedPassword, hostname, expirationDate, epgUrl, allowedOutputFormats, channelPreviewEnabled)
        |VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimMargin(), 7) {
          bindString(0, username)
          bindString(1, encryptedPassword)
          bindString(2, hostname)
          bindString(3, expirationDate)
          bindString(4, epgUrl)
          bindString(5, allowedOutputFormats)
          bindLong(6, channelPreviewEnabled)
        }
    notifyQueries(-327_432_409) { emit ->
      emit("UserCredentials")
    }
  }

  public fun updateCredentials(
    encryptedPassword: String,
    expirationDate: String?,
    allowedOutputFormats: String?,
    userId: Long,
  ) {
    driver.execute(-364_251_835,
        """UPDATE UserCredentials SET encryptedPassword = ?, expirationDate = ?, allowedOutputFormats = ? WHERE userId = ?""",
        4) {
          bindString(0, encryptedPassword)
          bindString(1, expirationDate)
          bindString(2, allowedOutputFormats)
          bindLong(3, userId)
        }
    notifyQueries(-364_251_835) { emit ->
      emit("UserCredentials")
    }
  }

  public fun updateAccountCredentials(
    username: String,
    encryptedPassword: String,
    hostname: String,
    epgUrl: String?,
    userId: Long,
  ) {
    driver.execute(-1_320_956_890,
        """UPDATE UserCredentials SET username = ?, encryptedPassword = ?, hostname = ?, epgUrl = ? WHERE userId = ?""",
        5) {
          bindString(0, username)
          bindString(1, encryptedPassword)
          bindString(2, hostname)
          bindString(3, epgUrl)
          bindLong(4, userId)
        }
    notifyQueries(-1_320_956_890) { emit ->
      emit("UserCredentials")
    }
  }

  public fun updateEpgUrl(epgUrl: String?, userId: Long) {
    driver.execute(-2_105_998_646, """UPDATE UserCredentials SET epgUrl = ? WHERE userId = ?""", 2)
        {
          bindString(0, epgUrl)
          bindLong(1, userId)
        }
    notifyQueries(-2_105_998_646) { emit ->
      emit("UserCredentials")
    }
  }

  public fun updateChannelPreviewEnabled(channelPreviewEnabled: Long, userId: Long) {
    driver.execute(912_523_589,
        """UPDATE UserCredentials SET channelPreviewEnabled = ? WHERE userId = ?""", 2) {
          bindLong(0, channelPreviewEnabled)
          bindLong(1, userId)
        }
    notifyQueries(912_523_589) { emit ->
      emit("UserCredentials")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(-1_587_147_082, """DELETE FROM UserCredentials WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-1_587_147_082) { emit ->
      emit("UserCredentials")
    }
  }

  private inner class SelectByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("UserCredentials", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("UserCredentials", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_418_124_551,
        """SELECT UserCredentials.userId, UserCredentials.username, UserCredentials.encryptedPassword, UserCredentials.hostname, UserCredentials.expirationDate, UserCredentials.epgUrl, UserCredentials.allowedOutputFormats, UserCredentials.channelPreviewEnabled FROM UserCredentials WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "UserCredentials.sq:selectByUserId"
  }

  private inner class SelectByUsernameQuery<out T : Any>(
    public val username: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("UserCredentials", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("UserCredentials", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_314_163_543,
        """SELECT UserCredentials.userId, UserCredentials.username, UserCredentials.encryptedPassword, UserCredentials.hostname, UserCredentials.expirationDate, UserCredentials.epgUrl, UserCredentials.allowedOutputFormats, UserCredentials.channelPreviewEnabled FROM UserCredentials WHERE username = ?""",
        mapper, 1) {
      bindString(0, username)
    }

    override fun toString(): String = "UserCredentials.sq:selectByUsername"
  }

  private inner class SelectUserIdQuery<out T : Any>(
    public val username: String,
    public val hostname: String,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("UserCredentials", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("UserCredentials", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_335_893_072,
        """SELECT userId FROM UserCredentials WHERE username = ? AND hostname = ?""", mapper, 2) {
      bindString(0, username)
      bindString(1, hostname)
    }

    override fun toString(): String = "UserCredentials.sq:selectUserId"
  }
}
