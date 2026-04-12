package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import kotlin.Any
import kotlin.Long
import kotlin.String

public class CategoryTvShowQueries(
  driver: SqlDriver,
) : TransacterImpl(driver) {
  public fun <T : Any> selectAllByUserId(userId: Long, mapper: (
    categoryTvShowLocalId: Long,
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

  public fun selectAllByUserId(userId: Long): Query<CategoryTvShow> = selectAllByUserId(userId) {
      categoryTvShowLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    CategoryTvShow(
      categoryTvShowLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectAllSorted(userId: Long, mapper: (
    categoryTvShowLocalId: Long,
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

  public fun selectAllSorted(userId: Long): Query<CategoryTvShow> = selectAllSorted(userId) {
      categoryTvShowLocalId, categoryId, categoryName, userId_, isPinned, isHidden, isDefault ->
    CategoryTvShow(
      categoryTvShowLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectVisibleSorted(userId: Long, mapper: (
    categoryTvShowLocalId: Long,
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

  public fun selectVisibleSorted(userId: Long): Query<CategoryTvShow> =
      selectVisibleSorted(userId) { categoryTvShowLocalId, categoryId, categoryName, userId_,
      isPinned, isHidden, isDefault ->
    CategoryTvShow(
      categoryTvShowLocalId,
      categoryId,
      categoryName,
      userId_,
      isPinned,
      isHidden,
      isDefault
    )
  }

  public fun <T : Any> selectDefaultCategory(userId: Long, mapper: (
    categoryTvShowLocalId: Long,
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

  public fun selectDefaultCategory(userId: Long): Query<CategoryTvShow> =
      selectDefaultCategory(userId) { categoryTvShowLocalId, categoryId, categoryName, userId_,
      isPinned, isHidden, isDefault ->
    CategoryTvShow(
      categoryTvShowLocalId,
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
    driver.execute(213_000_732, """
        |INSERT OR REPLACE INTO CategoryTvShow(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(213_000_732) { emit ->
      emit("CategoryTvShow")
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
    driver.execute(720_830_826, """
        |INSERT OR IGNORE INTO CategoryTvShow(categoryId, categoryName, userId, isPinned, isHidden, isDefault)
        |VALUES (?, ?, ?, ?, ?, ?)
        """.trimMargin(), 6) {
          bindLong(0, categoryId)
          bindString(1, categoryName)
          bindLong(2, userId)
          bindLong(3, isPinned)
          bindLong(4, isHidden)
          bindLong(5, isDefault)
        }
    notifyQueries(720_830_826) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun updatePinStatus(
    isPinned: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_606_631_586,
        """UPDATE CategoryTvShow SET isPinned = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isPinned)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_606_631_586) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun updateHideStatus(
    isHidden: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(1_752_227_961,
        """UPDATE CategoryTvShow SET isHidden = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isHidden)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(1_752_227_961) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun updateAllHideStatus(isHidden: Long, userId: Long) {
    driver.execute(1_324_409_232, """UPDATE CategoryTvShow SET isHidden = ? WHERE userId = ?""", 2)
        {
          bindLong(0, isHidden)
          bindLong(1, userId)
        }
    notifyQueries(1_324_409_232) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun updateAllPinStatus(isPinned: Long, userId: Long) {
    driver.execute(-1_455_210_325, """UPDATE CategoryTvShow SET isPinned = ? WHERE userId = ?""", 2)
        {
          bindLong(0, isPinned)
          bindLong(1, userId)
        }
    notifyQueries(-1_455_210_325) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun updateDefaultStatus(
    isDefault: Long,
    categoryId: Long,
    userId: Long,
  ) {
    driver.execute(-1_909_825_842,
        """UPDATE CategoryTvShow SET isDefault = ? WHERE categoryId = ? AND userId = ?""", 3) {
          bindLong(0, isDefault)
          bindLong(1, categoryId)
          bindLong(2, userId)
        }
    notifyQueries(-1_909_825_842) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun clearAllDefaults(userId: Long) {
    driver.execute(1_098_375_554, """UPDATE CategoryTvShow SET isDefault = 0 WHERE userId = ?""", 1)
        {
          bindLong(0, userId)
        }
    notifyQueries(1_098_375_554) { emit ->
      emit("CategoryTvShow")
    }
  }

  public fun deleteByUserId(userId: Long) {
    driver.execute(-1_565_050_300, """DELETE FROM CategoryTvShow WHERE userId = ?""", 1) {
          bindLong(0, userId)
        }
    notifyQueries(-1_565_050_300) { emit ->
      emit("CategoryTvShow")
    }
  }

  private inner class SelectAllByUserIdQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryTvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryTvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(625_852_454,
        """SELECT CategoryTvShow.categoryTvShowLocalId, CategoryTvShow.categoryId, CategoryTvShow.categoryName, CategoryTvShow.userId, CategoryTvShow.isPinned, CategoryTvShow.isHidden, CategoryTvShow.isDefault FROM CategoryTvShow WHERE userId = ?""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryTvShow.sq:selectAllByUserId"
  }

  private inner class SelectAllSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryTvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryTvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(353_836_966,
        """SELECT CategoryTvShow.categoryTvShowLocalId, CategoryTvShow.categoryId, CategoryTvShow.categoryName, CategoryTvShow.userId, CategoryTvShow.isPinned, CategoryTvShow.isHidden, CategoryTvShow.isDefault FROM CategoryTvShow WHERE userId = ? ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryTvShow.sq:selectAllSorted"
  }

  private inner class SelectVisibleSortedQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryTvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryTvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-878_100_041,
        """SELECT CategoryTvShow.categoryTvShowLocalId, CategoryTvShow.categoryId, CategoryTvShow.categoryName, CategoryTvShow.userId, CategoryTvShow.isPinned, CategoryTvShow.isHidden, CategoryTvShow.isDefault FROM CategoryTvShow WHERE userId = ? AND isHidden = 0 ORDER BY isPinned DESC""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryTvShow.sq:selectVisibleSorted"
  }

  private inner class SelectDefaultCategoryQuery<out T : Any>(
    public val userId: Long,
    mapper: (SqlCursor) -> T,
  ) : Query<T>(mapper) {
    override fun addListener(listener: Query.Listener) {
      driver.addListener("CategoryTvShow", listener = listener)
    }

    override fun removeListener(listener: Query.Listener) {
      driver.removeListener("CategoryTvShow", listener = listener)
    }

    override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> =
        driver.executeQuery(-1_339_048_889,
        """SELECT CategoryTvShow.categoryTvShowLocalId, CategoryTvShow.categoryId, CategoryTvShow.categoryName, CategoryTvShow.userId, CategoryTvShow.isPinned, CategoryTvShow.isHidden, CategoryTvShow.isDefault FROM CategoryTvShow WHERE userId = ? AND isDefault = 1 LIMIT 1""",
        mapper, 1) {
      bindLong(0, userId)
    }

    override fun toString(): String = "CategoryTvShow.sq:selectDefaultCategory"
  }
}
