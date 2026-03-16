package pt.hitv.core.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * iOS implementation of [BillingManager].
 *
 * This is a stub for StoreKit 2 integration. Currently returns NOT_SUBSCRIBED
 * for all queries. Full StoreKit 2 implementation will be added later using
 * Kotlin/Native interop with:
 * - Product.products(for:)
 * - Product.purchase()
 * - Transaction.currentEntitlements
 */
class IosBillingManager : BillingManager {

    private val _isPremium = MutableStateFlow(false)
    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus.NOT_SUBSCRIBED)

    override val isPremium: Flow<Boolean> = _isPremium
    override val subscriptionStatus: Flow<SubscriptionStatus> = _subscriptionStatus

    override fun initialize() {
        // TODO: Initialize StoreKit 2
        // Register for Transaction.updates
    }

    override suspend fun getProducts(): List<ProductDetails> {
        // TODO: Call Product.products(for: productIds) via interop
        return emptyList()
    }

    override suspend fun purchaseProduct(productId: String): PurchaseResult {
        // TODO: Call Product.purchase() via StoreKit 2 interop
        return PurchaseResult(
            isSuccess = false,
            errorMessage = "StoreKit 2 integration not yet implemented"
        )
    }

    override suspend fun restorePurchases(): Boolean {
        // TODO: Call AppStore.sync() and check Transaction.currentEntitlements
        return false
    }

    override fun hasPremiumThemes(): Boolean = false

    override fun cleanup() {
        // No-op for iOS; StoreKit handles its own lifecycle
    }
}
