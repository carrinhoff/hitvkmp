package pt.hitv.core.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class EpisodeInfo(
  public val episodeCreatorId: String,
  public val tmdb_id: Double?,
  public val release_date: String?,
  public val plot: String?,
  public val duration_secs: Double?,
  public val duration: String?,
  public val movie_image: String?,
  public val bitrate: Double?,
  public val rating: Double?,
  public val season: String?,
  public val userId: Long,
  public val playbackPosition: Long,
)
