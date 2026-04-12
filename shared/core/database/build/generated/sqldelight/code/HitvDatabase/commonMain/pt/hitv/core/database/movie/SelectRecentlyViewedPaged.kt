package pt.hitv.core.database.movie

import kotlin.Long
import kotlin.String

public data class SelectRecentlyViewedPaged(
  public val movieId: Long,
  public val name: String,
  public val streamId: String,
  public val streamIcon: String,
  public val rating: String,
  public val added: String,
  public val categoryCreatorId: String,
  public val containerExtension: String,
  public val isFavorite: Long,
  public val userId: Long,
  public val lastViewedTimestamp: Long,
  public val lastUpdated: Long,
  public val lastSeen: Long,
  public val contentHash: String?,
  public val syncVersion: Long,
)
