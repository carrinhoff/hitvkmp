package pt.hitv.core.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class SelectMovieDataWithInfo(
  public val streamId: Long,
  public val name: String,
  public val added: Double,
  public val category_id: Long,
  public val container_extension: String,
  public val custom_sid: String?,
  public val direct_source: String?,
  public val userId: Long,
  public val movieInfoId: Long?,
  public val streamIdCreator: Long?,
  public val kinopoisk_url: String?,
  public val tmdb_id: String?,
  public val name_: String?,
  public val o_name: String?,
  public val cover_big: String?,
  public val movie_image: String?,
  public val releasedate: String?,
  public val episode_run_time: String?,
  public val youtube_trailer: String?,
  public val director: String?,
  public val actors: String?,
  public val cast_: String?,
  public val description: String?,
  public val plot: String?,
  public val age: String?,
  public val mpaa_rating: String?,
  public val rating_count_kinopoisk: String?,
  public val country: String?,
  public val genre: String?,
  public val backdrop_path: String?,
  public val duration_secs: String?,
  public val duration: String?,
  public val bitrate: String?,
  public val rating: String?,
  public val userId_: Long?,
  public val playbackPosition: Long?,
)
