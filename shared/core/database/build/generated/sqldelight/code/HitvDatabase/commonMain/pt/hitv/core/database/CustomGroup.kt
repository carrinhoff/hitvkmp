package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class CustomGroup(
  public val groupId: Long,
  public val groupName: String,
  public val groupIcon: String?,
  public val createdAt: Long,
  public val updatedAt: Long,
  public val sortOrder: Long,
  public val isPinned: Long,
  public val isHidden: Long,
  public val isDefault: Long,
)
