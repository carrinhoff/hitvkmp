package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Season(
  public val season_id: String,
  public val air_date: String?,
  public val episode_count: Long?,
  public val name: String?,
  public val overview: String?,
  public val season_number: Long,
  public val cover: String?,
  public val cover_big: String?,
  public val series_id: String,
  public val userId: Long,
)
