package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class EpgChannel(
  public val channel_id: String,
  public val display_name: String?,
  public val logo: String?,
  public val userId: Long,
)
