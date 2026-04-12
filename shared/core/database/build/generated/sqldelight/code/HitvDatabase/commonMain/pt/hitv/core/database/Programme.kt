package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Programme(
  public val id: Long,
  public val channel_name: String?,
  public val start_time: Long,
  public val end_time: Long,
  public val userId: Long,
  public val imageUrl: String?,
)
