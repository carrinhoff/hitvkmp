package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Description(
  public val desc_id: Long,
  public val desc: String?,
  public val programme_id: Long?,
  public val userId: Long,
)
