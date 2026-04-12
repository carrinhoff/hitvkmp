package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class ParentalControl(
  public val id: Long,
  public val categoryId: Long,
  public val categoryName: String,
  public val userId: Long,
  public val isProtected: Long,
  public val createdAt: Long,
)
