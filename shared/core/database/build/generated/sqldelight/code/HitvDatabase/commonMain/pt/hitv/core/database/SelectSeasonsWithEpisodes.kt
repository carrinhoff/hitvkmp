package pt.hitv.core.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class SelectSeasonsWithEpisodes(
  public val seasonId: String,
  public val airDate: String?,
  public val episodeCount: Long?,
  public val name: String?,
  public val overview: String?,
  public val cover: String?,
  public val coverBig: String?,
  public val seriesId: String,
  public val episodeId: String,
  public val episodeNum: Long?,
  public val containerExtension: String?,
  public val added: String?,
  public val season: Long?,
  public val title: String?,
  public val tmdbId: Double?,
  public val releaseDate: String?,
  public val plot: String?,
  public val durationSecs: Double?,
  public val duration: String?,
  public val bitrate: Double?,
  public val rating: Double?,
  public val movieImage: String?,
  public val seasonNumber: Long,
  public val playbackPosition: Long,
)
