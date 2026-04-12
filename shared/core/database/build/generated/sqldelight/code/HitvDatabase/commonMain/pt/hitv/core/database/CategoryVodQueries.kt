package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class CategoryVodQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    categoryVodLocalId: Long,
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = SelectAllByUserIdQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectAllByUserId(userId: Long): Query<CategoryVod> = selectAllByUserId(userId) {
      categoryVodLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    CategoryVod(
      categoryVodLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectAllSorted(userId: Long, mapper: (
    categoryVodLocalId: Long,
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = SelectAllSortedQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectAllSorted(userId: Long): Query<CategoryVod> = selectAllSorted(userId) {
      categoryVodLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    CategoryVod(
      categoryVodLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectVisibleSorted(userId: Long, mapper: (
    categoryVodLocalId: Long,
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = SelectVisibleSortedQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectVisibleSorted(userId: Long): Query<CategoryVod> = selectVisibleSorted(userId) {
      categoryVodLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    CategoryVod(
      categoryVodLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectDefaultCategory(userId: Long, mapper: (
    categoryVodLocalId: Long,
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) -> T): Query<T> = SelectDefaultCategoryQuery(userId) { cursor ->
    mapper(
      cursor.getLong(0)!!,
      cursor.getLong(1)!!,
      cursor.getString(2)!!,
      cursor.getLong(3)!!,
      cursor.getLong(4)!!,
      cursor.getLong(5)!!,
      cursor.getLong(6)!!
    )
  }

  public fun selectDefaultCategory(userId: Long): Query<CategoryVod> =
      selectDefaultCategory(userId) { categoryVodLocalId, categoryId, categoryName, userId_,
      isPinned, isHidden, isDefault ->
    CategoryVod(
      categoryVodLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun insertOrReplace(
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) {
    driver.execute(-201_567_698, """
        |INSERT OR REPLACE INTO CategoryVod(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(-201_567_698) { emit ->
      emit("CategoryVod")
    }
  }

  public fun insertOrIgnore(
    categoryId: Long,
    categoryName: String,
    userId: Long,
    isPinned: Long,
    isHidden: Long,
    isDefault: Long,
  ) {
    driver.execute(1_954_383_640, """
        |INSERT OR IGNORE INTO CategoryVod(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(1_954_383_640) { emit ->
      emit("CategoryVod")
    }
  }

  public fun updatePinStatus(
    isPinned: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_192_063_156,
        """UPDATE CategoryVod SET isPinned = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isPinned)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_192_063_156) { emit ->
      emit("CategoryVod")
    }
  }

  public fun updateHideStatus(
    isHidden: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_785_508_519,
        """UPDATE CategoryVod SET isHidden = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isHidden)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_785_508_519) { emit ->
      emit("CategoryVod")
    }
  }

  public fun updateAllHideStatus(isHidden: Long, userId: Long) {
    driver.execute(648_067_234, """UPDATE CategoryVod SET isHidden = ? WHERE userId = ?""", 2) {
          bindLong(0, isHidden)
          bindLong(1, userId)
        }
    notifyQueries(648_067_234) { emit ->
      emit("CategoryVod")
    }
  }

  public fun updateAllPinStatus(isPinned: Long, userId: Long) {
    driver.execute(462_634_841, """UPDATE CategoryVod SET isPinned = ? WHERE userId = ?""", 2) {
          bindLong(0, isPinned)
          bindLong(1, userId)
        }
    notifyQueries(462_634_841) { emit ->
      emit("CategoryVod")
    }
  }

  public fun updateDefaultStatus(
    isDefault: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_708_799_456,
        """UPDATE CategoryVod SET isDefault = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isDefault)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_708_799_456) { emit ->
      emit("CategoryVod")
    }
  }

  public fun clearAllDefaults(userId: Long) {
    driver.execute(1_131_656_112, """UPDATE CategoryVod SET isDefault = 0 WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(1_131_656_112) { emit ->
      emit("CategoryVod")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(-331_497_486, """DELETE FROM CategoryVod WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-331_497_486) { emit ->
      emit("CategoryVod")
    }
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryVod", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryVod", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_657_549_752,
        """SELECT CategoryVod.categoryVodLocalId, CategoryVod.categoryId, CategoryVod.categoryName, CategoryVod.userId, CategoryVod.isPinned, CategoryVod.isHidden, CategoryVod.isDefault FROM CategoryVod WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryVod.sq:selectAllByUserId"
  }

  private inner class SelectAllSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryVod", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryVod", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-60_731_464,
        """SELECT CategoryVod.categoryVodLocalId, CategoryVod.categoryId, CategoryVod.categoryName, CategoryVod.userId, CategoryVod.isPinned, CategoryVod.isHidden, CategoryVod.isDefault FROM CategoryVod WHERE userId = ? ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryVod.sq:selectAllSorted"
  }

  private inner class SelectVisibleSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryVod", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryVod", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_554_442_039,
        """SELECT CategoryVod.categoryVodLocalId, CategoryVod.categoryId, CategoryVod.categoryName, CategoryVod.userId, CategoryVod.isPinned, CategoryVod.isHidden, CategoryVod.isDefault FROM CategoryVod WHERE userId = ? AND isHidden = 0 ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryVod.sq:selectVisibleSorted"
  }

  private inner class SelectDefaultCategoryQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryVod", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryVod", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(1_531_320_025,
        """SELECT CategoryVod.categoryVodLocalId, CategoryVod.categoryId, CategoryVod.categoryName, CategoryVod.userId, CategoryVod.isPinned, CategoryVod.isHidden, CategoryVod.isDefault FROM CategoryVod WHERE userId = ? AND isDefault = 1 LIMIT 1""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryVod.sq:selectDefaultCategory"
  }
}
