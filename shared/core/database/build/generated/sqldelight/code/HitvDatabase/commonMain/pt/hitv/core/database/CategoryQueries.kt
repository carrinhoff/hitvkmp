package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class CategoryQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    categoryLocalId: Long,
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

  public fun selectAllByUserId(userId: Long): Query<Category> = selectAllByUserId(userId) {
      categoryLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    Category(
      categoryLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectAllSorted(userId: Long, mapper: (
    categoryLocalId: Long,
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

  public fun selectAllSorted(userId: Long): Query<Category> = selectAllSorted(userId) {
      categoryLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    Category(
      categoryLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectVisibleSorted(userId: Long, mapper: (
    categoryLocalId: Long,
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

  public fun selectVisibleSorted(userId: Long): Query<Category> = selectVisibleSorted(userId) {
      categoryLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    Category(
      categoryLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectDefaultCategory(userId: Long, mapper: (
    categoryLocalId: Long,
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

  public fun selectDefaultCategory(userId: Long): Query<Category> = selectDefaultCategory(userId) {
      categoryLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    Category(
      categoryLocalId,
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
    driver.execute(-1_021_392_389, """
        |INSERT OR REPLACE INTO Category(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(-1_021_392_389) { emit ->
      emit("Category")
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
    driver.execute(-1_535_745_621, """
        |INSERT OR IGNORE INTO Category(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(-1_535_745_621) { emit ->
      emit("Category")
    }
  }

  public fun updatePinStatus(
    isPinned: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(372_238_465,
        """UPDATE Category SET isPinned = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isPinned)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(372_238_465) { emit ->
      emit("Category")
    }
  }

  public fun updateHideStatus(
    isHidden: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(2_140_746_874,
        """UPDATE Category SET isHidden = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isHidden)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(2_140_746_874) { emit ->
      emit("Category")
    }
  }

  public fun updateAllHideStatus(isHidden: Long, userId: Long) {
    driver.execute(754_483_695, """UPDATE Category SET isHidden = ? WHERE userId = ?""", 2) {
          bindLong(0, isHidden)
          bindLong(1, userId)
        }
    notifyQueries(754_483_695) { emit ->
      emit("Category")
    }
  }

  public fun updateAllPinStatus(isPinned: Long, userId: Long) {
    driver.execute(-1_750_689_684, """UPDATE Category SET isPinned = ? WHERE userId = ?""", 2) {
          bindLong(0, isPinned)
          bindLong(1, userId)
        }
    notifyQueries(-1_750_689_684) { emit ->
      emit("Category")
    }
  }

  public fun updateDefaultStatus(
    isDefault: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_815_215_917,
        """UPDATE Category SET isDefault = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isDefault)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_815_215_917) { emit ->
      emit("Category")
    }
  }

  public fun clearAllDefaults(userId: Long) {
    driver.execute(1_486_894_467, """UPDATE Category SET isDefault = 0 WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(1_486_894_467) { emit ->
      emit("Category")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(473_340_549, """DELETE FROM Category WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(473_340_549) { emit ->
      emit("Category")
    }
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-214_963_131,
        """SELECT Category.categoryLocalId, Category.categoryId, Category.categoryName, Category.userId, Category.isPinned, Category.isHidden, Category.isDefault FROM Category WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Category.sq:selectAllByUserId"
  }

  private inner class SelectAllSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-880_556_155,
        """SELECT Category.categoryLocalId, Category.categoryId, Category.categoryName, Category.userId, Category.isPinned, Category.isHidden, Category.isDefault FROM Category WHERE userId = ? ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Category.sq:selectAllSorted"
  }

  private inner class SelectVisibleSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_448_025_578,
        """SELECT Category.categoryLocalId, Category.categoryId, Category.categoryName, Category.userId, Category.isPinned, Category.isHidden, Category.isDefault FROM Category WHERE userId = ? AND isHidden = 0 ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Category.sq:selectVisibleSorted"
  }

  private inner class SelectDefaultCategoryQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("Category", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("Category", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(718_323_942,
        """SELECT Category.categoryLocalId, Category.categoryId, Category.categoryName, Category.userId, Category.isPinned, Category.isHidden, Category.isDefault FROM Category WHERE userId = ? AND isDefault = 1 LIMIT 1""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "Category.sq:selectDefaultCategory"
  }
}
