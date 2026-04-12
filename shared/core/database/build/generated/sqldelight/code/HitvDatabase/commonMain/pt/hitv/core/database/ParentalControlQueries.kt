package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class ParentalControlQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    id: Long,
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isProtected: Long,
    createdAt: Long,
  ) -> T): Query<T> = SelectAllByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectAllByUserId(userId: Long): Query<ParentalControl> = selectAllByUserId(userId) {
      id, categoryId, categoryName, userId_, isProtected, createdAt ->
    ParentalControl(
      id,
      categoryId,
      categoryName,
      userId_,
      isProtected,
      createdAt
    )
  }

  public fun <T : Any> selectByCategory(
    categoryId: Long,
    userId: Long,
    mapper: (
      id: Long,
      categoryId: Long,
      categoryName: String,
      userId: Long,
      isProtected: Long,
      createdAt: Long,
    ) -> T,
  ): Query<T> = SelectByCategoryQuery(categoryId, userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!
    )
  }

  public fun selectByCategory(categoryId: Long, userId: Long): Query<ParentalControl> =
      selectByCategory(categoryId, userId) { id, categoryId_, categoryName, userId_, isProtected,
      createdAt ->
    ParentalControl(
      id,
      categoryId_,
      categoryName,
      userId_,
      isProtected,
      createdAt
    )
  }

  public fun isCategoryProtected(categoryId: Long, userId: Long): Query<Long> =
      IsCategoryProtectedQuery(categoryId, userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun selectProtectedCategoryIds(userId: Long): Query<Long> =
      SelectProtectedCategoryIdsQuery(userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun countProtected(userId: Long): Query<Long> = CountProtectedQuery(userId) { cursor ->
    cursor.getLong(0)!!
  }

  public fun insertOrReplace(
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isProtected: Long,
    createdAt: Long,
  ) {
    driver.execute(2_000_998_259, """
        |INSERT OR REPLACE INTO ParentalControl(categoryId, categoryName, userId, isProtected, createdAt)
        |VALUES (?, ?, ?, ?, ?)
        """.trimMargin(), 5) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isProtected)
          bindLong(4, createdAt)
        }
    notifyQueries(2_000_998_259) { emit ->
      emit("ParentalControl")
    }
  }

  public fun updateProtectionStatus(
    isProtected: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(-1_730_579_495,
        """UPDATE ParentalControl SET isProtected = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isProtected)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(-1_730_579_495) { emit ->
      emit("ParentalControl")
    }
  }

  public fun deleteByCategory(categoryId: Long, userId: Long) {
    driver.execute(-864_108_795,
        """DELETE FROM ParentalControl WHERE categoryId = ? AND userId = ?""", 2) {
          bindLong(0, categoryId)
          bindLong(1, userId)
        }
    notifyQueries(-864_108_795) { emit ->
      emit("ParentalControl")
    }
  }

  public fun deleteAllByUserId(userId: Long) {
    driver.execute(-549_376_082, """DELETE FROM ParentalControl WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-549_376_082) { emit ->
      emit("ParentalControl")
    }
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ParentalControl", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ParentalControl", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(904_557_501,
        """SELECT ParentalControl.id, ParentalControl.categoryId, ParentalControl.categoryName, ParentalControl.userId, ParentalControl.isProtected, ParentalControl.createdAt FROM ParentalControl WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "ParentalControl.sq:selectAllByUserId"
  }

  private inner class SelectByCategoryQuery<out T : Any>(
    public val categoryId: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ParentalControl", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ParentalControl", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(983_907_606,
        """SELECT ParentalControl.id, ParentalControl.categoryId, ParentalControl.categoryName, ParentalControl.userId, ParentalControl.isProtected, ParentalControl.createdAt FROM ParentalControl WHERE categoryId = ? AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindLong(0, categoryId)
      bindLong(1, userId)
    }

    override fun toString(): String = "ParentalControl.sq:selectByCategory"
  }

  private inner class IsCategoryProtectedQuery<out T : Any>(
    public val categoryId: Long,
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ParentalControl", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ParentalControl", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(689_085_857,
        """SELECT isProtected FROM ParentalControl WHERE categoryId = ? AND userId = ? LIMIT 1""",
        mapper, 2) {
      bindLong(0, categoryId)
      bindLong(1, userId)
    }

    override fun toString(): String = "ParentalControl.sq:isCategoryProtected"
  }

  private inner class SelectProtectedCategoryIdsQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ParentalControl", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ParentalControl", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(984_112_557,
        """SELECT categoryId FROM ParentalControl WHERE userId = ? AND isProtected = 1""", mapper,
        1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "ParentalControl.sq:selectProtectedCategoryIds"
  }

  private inner class CountProtectedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("ParentalControl", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("ParentalControl", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_698_998_564,
        """SELECT COUNT(*) FROM ParentalControl WHERE userId = ? AND isProtected = 1""", mapper, 1)
        {
      bindLong(0, userId)
    }

    override fun toString(): String = "ParentalControl.sq:countProtected"
  }
}
