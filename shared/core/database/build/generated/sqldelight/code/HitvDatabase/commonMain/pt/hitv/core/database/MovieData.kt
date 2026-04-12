package pt.hitv.core.database

import kotlin.Double
import kotlin.Long
import kotlin.String

public data class MovieData(
  public val streamId: Long,
  public val name: String,
  public val added: Double,
  public val category_id: Long,
  public val container_extension: String,
  public val custom_sid: String?,
  public val direct_source: String?,
  public val userId: Long,
)
