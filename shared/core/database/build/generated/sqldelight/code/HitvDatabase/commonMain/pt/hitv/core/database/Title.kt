package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Title(
  public val title_id: Long,
  public val title: String?,
  public val programme_id: Long?,
  public val userId: Long,
)
