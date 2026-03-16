package pt.hitv.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkCategory(
    @SerialName("category_id") val categoryId: Int,
    @SerialName("category_name") val categoryName: String? = null
)
