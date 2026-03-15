package pt.hitv.core.model

import kotlinx.datetime.Clock

data class CustomGroup(
    val id: Long = 0,
    val name: String,
    val icon: String? = null,
    val channelCount: Int = 0,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val sortOrder: Int = 0,
    val isPinned: Boolean = false,
    val isHidden: Boolean = false,
    val isDefault: Boolean = false
)
