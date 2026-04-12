package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class SelectCategoriesWithEpgCounts(
  public val categoryId: Long,
  public val categoryName: String,
  public val channelCount: Long,
)
