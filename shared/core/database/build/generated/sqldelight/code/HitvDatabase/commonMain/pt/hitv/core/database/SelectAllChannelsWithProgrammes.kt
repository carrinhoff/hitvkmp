package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class SelectAllChannelsWithProgrammes(
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
  public val channel_id: String?,
  public val display_name: String?,
  public val logo: String?,
  public val userId_: Long?,
  public val id: Long?,
  public val channel_name: String?,
  public val start_time: Long?,
  public val end_time: Long?,
  public val userId__: Long?,
  public val imageUrl: String?,
  public val title_id: Long?,
  public val title: String?,
  public val programme_id: Long?,
  public val userId___: Long?,
  public val desc_id: Long?,
  public val desc: String?,
  public val programme_id_: Long?,
  public val userId____: Long?,
)
