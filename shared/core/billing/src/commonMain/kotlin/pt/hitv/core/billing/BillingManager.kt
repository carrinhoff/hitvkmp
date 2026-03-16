package pt.hitv.core.billing

import kotlinx.coroutines.flow.Flow

data class ProductDetails(
    val productId: String,
    val title: String,
    val description: String,
    val price: String
)

data class PurchaseResult(
    val isSuccess: Boolean,
    val productId: String? = null,
    val errorMessage: String? = null
)

enum class SubscriptionStatus {
    ACTIVE, EXPIRED, NOT_SUBSCRIBED, GRACE_PERIOD
}

interface BillingManager {
    val isPremium: Flow<Boolean>
    val subscriptionStatus: Flow<SubscriptionStatus>
    fun initialize()
    suspend fun getProducts(): List<ProductDetails>
    suspend fun purchaseProduct(productId: String): PurchaseResult
    suspend fun restorePurchases(): Boolean
    fun hasPremiumThemes(): Boolean
    fun cleanup()
}
