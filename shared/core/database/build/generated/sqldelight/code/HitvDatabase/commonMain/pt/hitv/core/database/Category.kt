package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class Category(
  public val categoryLocalId: Long,
  public val categoryId: Long,
  public val categoryName: String,
  public val userId: Long,
  public val isPinned: Long,
  public val isHidden: Long,
  public val isDefault: Long,
)
