package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class SelectProgrammesForCategory(
  public val channel_id: String,
  public val logo: String?,
  public val display_name: String?,
  public val channel_name: String,
  public val start_time: Long,
  public val end_time: Long,
  public val id: Long,
  public val title: String?,
  public val programme_id: Long,
  public val title_id: Long?,
  public val description: String?,
  public val desc_id: Long?,
)
