package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class SelectChannelWithProgrammes(
  public val channel_id: String,
  public val display_name: String?,
  public val logo: String?,
  public val userId: Long,
  public val id: Long,
  public val channel_name: String?,
  public val start_time: Long,
  public val end_time: Long,
  public val userId_: Long,
  public val imageUrl: String?,
)
