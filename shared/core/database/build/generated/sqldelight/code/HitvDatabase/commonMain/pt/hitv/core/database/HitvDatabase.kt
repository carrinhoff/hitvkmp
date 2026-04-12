package pt.hitv.core.database

import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.Unit
import pt.hitv.core.database.database.newInstance
import pt.hitv.core.database.database.schema

public interface HitvDatabase : Transacter {
  public val categoryQueries: CategoryQueries

  public val categoryTvShowQueries: CategoryTvShowQueries

  public val categoryVodQueries: CategoryVodQueries

  public val channelQueries: ChannelQueries

  public val customGroupQueries: CustomGroupQueries

  public val epgChannelQueries: EpgChannelQueries

  public val movieQueries: MovieQueries

  public val movieInfoQueries: MovieInfoQueries

  public val parentalControlQueries: ParentalControlQueries

  public val programmeQueries: ProgrammeQueries

  public val seriesInfoQueries: SeriesInfoQueries

  public val tvShowQueries: TvShowQueries

  public val userCredentialsQueries: UserCredentialsQueries

  public companion object {
    public val Schema: SqlSchema<QueryResult.Value<Unit>>
      get() = HitvDatabase::class.schema

    public operator fun invoke(driver: SqlDriver): HitvDatabase =
        HitvDatabase::class.newInstance(driver)
  }
}
