package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Episode(
  public val episode_id: String,
  public val episode_num: Long?,
  public val title: String?,
  public val container_extension: String?,
  public val custom_sid: String?,
  public val added: String?,
  public val season: Long?,
  public val direct_source: String?,
  public val seasonCreatorId: String,
  public val userId: Long,
)
