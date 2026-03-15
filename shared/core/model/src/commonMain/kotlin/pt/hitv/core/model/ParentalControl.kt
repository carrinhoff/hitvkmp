package pt.hitv.core.model

/**
 * Domain model for parental control settings.
 */
data class ParentalControl(
    val id: Long = 0L,
    val categoryId: Int,
    val categoryName: String,
    val userId: Int,
    val isProtected: Boolean = false,
    val createdAt: Long = 0L
)
