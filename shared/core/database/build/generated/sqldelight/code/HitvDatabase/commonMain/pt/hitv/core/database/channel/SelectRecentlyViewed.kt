package pt.hitv.core.database.channel

import kotlin.Long
import kotlin.String

public data class SelectRecentlyViewed(
  public val channelId: Long,
  public val name: String,
  public val streamUrl: String,
  public val streamIcon: String,
  public val epgChannelId: String?,
  public val categoryCreatorId: String,
  public val isFavorite: Long,
  public val licenseKey: String?,
  public val userId: Long,
  public val lastViewedTimestamp: Long,
  public val lastUpdated: Long,
  public val lastSeen: Long,
  public val contentHash: String?,
  public val syncVersion: Long,
)
