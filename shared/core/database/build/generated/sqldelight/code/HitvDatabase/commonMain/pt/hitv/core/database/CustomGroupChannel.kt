package pt.hitv.core.database

import kotlin.Long

public data class CustomGroupChannel(
  public val id: Long,
  public val groupId: Long,
  public val channelId: Long,
  public val channelUserId: Long,
  public val position: Long,
  public val addedAt: Long,
)
