package pt.hitv.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val categoryId: Int,
    val categoryName: String
)
