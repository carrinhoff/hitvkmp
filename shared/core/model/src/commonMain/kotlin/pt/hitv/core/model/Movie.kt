package pt.hitv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    val movieId: Long,
    val name: String,
    val streamId: String,
    val streamIcon: String? = "",
    val rating: String? = "",
    val added: String,
    val categoryId: String?,
    val containerExtension: String,
    val isFavorite: Boolean = false,
    val customSid: String? = "",
    val directSource: String? = "",
    val num: String? = null,
    val streamType: String? = "",
    val rating5based: Double? = 0.0,
    val lastViewedTimestamp: Long = 0L
)
