package pt.hitv.core.database

import kotlin.Long
import kotlin.String

public data class UserCredentials(
  public val userId: Long,
  public val username: String,
  public val encryptedPassword: String,
  public val hostname: String,
  public val expirationDate: String?,
  public val epgUrl: String?,
  public val allowedOutputFormats: String?,
  public val channelPreviewEnabled: Long,
)
