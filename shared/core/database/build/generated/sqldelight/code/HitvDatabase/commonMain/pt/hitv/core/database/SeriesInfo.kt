package pt.hitv.core.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class SeriesInfo(
  public val seriesId: String,
  public val name: String?,
  public val cover: String?,
  public val plot: String?,
  public val cast_: String?,
  public val director: String?,
  public val genre: String?,
  public val releaseDate: String?,
  public val last_modified: String?,
  public val rating: String?,
  public val rating_5based: Double?,
  public val backdrop_path: String?,
  public val youtube_trailer: String?,
  public val episode_run_time: String?,
  public val category_id: String?,
  public val userId: Long,
)
