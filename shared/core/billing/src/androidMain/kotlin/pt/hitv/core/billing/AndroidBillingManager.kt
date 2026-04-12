package pt.hitv.core.billing

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pt.hitv.core.common.constants.BillingConstants

/**
 * Android implementation of [BillingManager] using Google Play Billing Library.
 *
 * Handles:
 * - BillingClient setup and connection management
 * - Product detail queries for subscriptions and one-time purchases
 * - Purchase flow launching
 * - Purchase acknowledgment
 * - Premium state tracking via queryPurchasesAsync
 */
class AndroidBillingManager(
    private val context: Context
) : BillingManager, PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private val _isBillingSetup = MutableStateFlow(false)
    private val _premiumThemesOwned = MutableStateFlow(false)
    private val _removeAdsOwned = MutableStateFlow(false)
    private val _annualPremiumOwned = MutableStateFlow(false)
    private val _lifetimePremiumOwned = MutableStateFlow(false)

    private val _productPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _productTrialInfo = MutableStateFlow<Map<String, String?>>(emptyMap())

    override val isPremium: Flow<Boolean> = combine(
        _annualPremiumOwned,
        _lifetimePremiumOwned
    ) { annual, lifetime -> annual || lifetime }

    override val subscriptionStatus: Flow<SubscriptionStatus> = combine(
        _annualPremiumOwned,
        _lifetimePremiumOwned
    ) { annual, lifetime ->
        when {
            annual || lifetime -> SubscriptionStatus.ACTIVE
            else -> SubscriptionStatus.NOT_SUBSCRIBED
        }
    }

    override fun initialize() {
        if (BillingConstants.USE_FAKE_BILLING) {
            _productPrices.value = mapOf(
                BillingConstants.ANNUAL_PREMIUM to "FAKE \u20ac4.99",
                BillingConstants.LIFETIME_PREMIUM to "FAKE \u20ac9.99"
            )
            _productTrialInfo.value = mapOf(
                BillingConstants.ANNUAL_PREMIUM to "3 dias",
                BillingConstants.LIFETIME_PREMIUM to null
            )
            return
        }

        if (billingClient != null && _isBillingSetup.value) {
            queryProductDetails()
            return
        }

        billingClient = BillingClient.newBuilder(context)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .enablePrepaidPlans()
                    .build()
            )
            .setListener(this)
            .build()

        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                _isBillingSetup.value =
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK
                if (_isBillingSetup.value) {
                    queryPurchases()
                    queryProductDetails()
                }
            }

            override fun onBillingServiceDisconnected() {
                _isBillingSetup.value = false
            }
        })
    }

    override suspend fun getProducts(): List<ProductDetails> {
        val prices = _productPrices.value
        val trialInfo = _productTrialInfo.value
        return prices.map { (productId, price) ->
            val trial = trialInfo[productId]
            ProductDetails(
                productId = productId,
                title = when (productId) {
                    BillingConstants.ANNUAL_PREMIUM -> "Annual Premium"
                    BillingConstants.LIFETIME_PREMIUM -> "Lifetime Premium"
                    else -> productId
                },
                description = if (trial != null) "Free trial: $trial" else "",
                price = price
            )
        }
    }

    override suspend fun purchaseProduct(productId: String): PurchaseResult {
        if (BillingConstants.USE_FAKE_BILLING) {
            when (productId) {
                BillingConstants.THEME_PREMIUM -> _premiumThemesOwned.value = true
                BillingConstants.REMOVE_ADS -> _removeAdsOwned.value = true
                BillingConstants.ANNUAL_PREMIUM -> _annualPremiumOwned.value = true
                BillingConstants.LIFETIME_PREMIUM -> _lifetimePremiumOwned.value = true
            }
            return PurchaseResult(isSuccess = true, productId = productId)
        }

        if (!_isBillingSetup.value) {
            return PurchaseResult(
                isSuccess = false,
                errorMessage = "Google Play Services not available."
            )
        }

        // Note: actual billing flow launch requires an Activity reference.
        // In a real implementation, the ViewModel/UI would call launchBillingFlow
        // with the Activity. This suspend function is for the common interface.
        return PurchaseResult(
            isSuccess = false,
            errorMessage = "Use launchBillingFlow(activity, productId) for Android."
        )
    }

    override suspend fun restorePurchases(): Boolean {
        if (BillingConstants.USE_FAKE_BILLING) return true
        queryPurchases()
        return true
    }

    override fun hasPremiumThemes(): Boolean = _premiumThemesOwned.value

    override fun cleanup() {
        billingClient?.endConnection()
        billingClient = null
        _isBillingSetup.value = false
    }

    // ==================== PurchasesUpdatedListener ====================

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { purchase ->
                if (purchase.products.contains(BillingConstants.THEME_PREMIUM)) {
                    acknowledgeAndOwn(purchase, BillingConstants.THEME_PREMIUM)
                }
                if (purchase.products.contains(BillingConstants.REMOVE_ADS)) {
                    acknowledgeAndOwn(purchase, BillingConstants.REMOVE_ADS)
                }
                if (purchase.products.contains(BillingConstants.ANNUAL_PREMIUM)) {
                    acknowledgeAndOwn(purchase, BillingConstants.ANNUAL_PREMIUM)
                }
                if (purchase.products.contains(BillingConstants.LIFETIME_PREMIUM)) {
                    acknowledgeAndOwn(purchase, BillingConstants.LIFETIME_PREMIUM)
                }
            }
        }
    }

    // ==================== Internal ====================

    private fun queryPurchases() {
        if (BillingConstants.USE_FAKE_BILLING) return

        var foundPremium = false
        var foundRemoveAds = false
        var foundAnnual = false
        var foundLifetime = false

        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, inAppPurchasesList ->
            inAppPurchasesList.forEach { purchase ->
                if (purchase.products.contains(BillingConstants.THEME_PREMIUM) && purchase.isAcknowledged) foundPremium = true
                if (purchase.products.contains(BillingConstants.REMOVE_ADS) && purchase.isAcknowledged) foundRemoveAds = true
                if (purchase.products.contains(BillingConstants.LIFETIME_PREMIUM) && purchase.isAcknowledged) foundLifetime = true
            }

            billingClient?.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { _, subscriptionsList ->
                subscriptionsList.forEach { purchase ->
                    if (purchase.products.contains(BillingConstants.ANNUAL_PREMIUM) && purchase.isAcknowledged) foundAnnual = true
                }

                _premiumThemesOwned.value = foundPremium
                _removeAdsOwned.value = foundRemoveAds
                _annualPremiumOwned.value = foundAnnual
                _lifetimePremiumOwned.value = foundLifetime
            }
        }
    }

    private fun queryProductDetails() {
        if (BillingConstants.USE_FAKE_BILLING || !_isBillingSetup.value) {
            _productPrices.value = mapOf(
                BillingConstants.ANNUAL_PREMIUM to "TEST \u20ac4.99",
                BillingConstants.LIFETIME_PREMIUM to "TEST \u20ac9.99"
            )
            return
        }

        val subscriptionParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConstants.ANNUAL_PREMIUM)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingConstants.LIFETIME_PREMIUM)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        val pricesMap = mutableMapOf<String, String>()
        val trialInfoMap = mutableMapOf<String, String?>()

        billingClient?.queryProductDetailsAsync(subscriptionParams) { subscriptionResult, subscriptionDetailsResult ->
            if (subscriptionResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (productDetails in subscriptionDetailsResult) {
                    val subscriptionOfferDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
                    val phases = subscriptionOfferDetails?.pricingPhases?.pricingPhaseList

                    val price = if (!phases.isNullOrEmpty()) {
                        phases.last().formattedPrice
                    } else ""

                    val trialInfo = if (phases != null && phases.size > 1) {
                        parseBillingPeriod(phases.first().billingPeriod)
                    } else null

                    pricesMap[productDetails.productId] = price
                    trialInfoMap[productDetails.productId] = trialInfo
                }
            } else {
                pricesMap[BillingConstants.ANNUAL_PREMIUM] = "\u20ac4.99"
                trialInfoMap[BillingConstants.ANNUAL_PREMIUM] = null
            }

            billingClient?.queryProductDetailsAsync(inAppParams) { inAppResult, inAppDetailsResult ->
                if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    for (productDetails in inAppDetailsResult) {
                        val price = productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                        pricesMap[productDetails.productId] = price
                    }
                } else {
                    pricesMap[BillingConstants.LIFETIME_PREMIUM] = "\u20ac9.99"
                }

                _productPrices.value = pricesMap
                _productTrialInfo.value = trialInfoMap
            }
        }
    }

    private fun parseBillingPeriod(period: String): String? {
        return try {
            when {
                period.matches(Regex("P(\\d+)D")) -> {
                    val days = period.replace("P", "").replace("D", "").toInt()
                    "$days ${if (days == 1) "dia" else "dias"}"
                }
                period.matches(Regex("P(\\d+)W")) -> {
                    val weeks = period.replace("P", "").replace("W", "").toInt()
                    "$weeks ${if (weeks == 1) "semana" else "semanas"}"
                }
                period.matches(Regex("P(\\d+)M")) -> {
                    val months = period.replace("P", "").replace("M", "").toInt()
                    "$months ${if (months == 1) "m\u00eas" else "meses"}"
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun acknowledgeAndOwn(purchase: Purchase, productId: String) {
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    setOwnership(productId)
                }
            }
        } else {
            setOwnership(productId)
        }
    }

    private fun setOwnership(productId: String) {
        when (productId) {
            BillingConstants.THEME_PREMIUM -> _premiumThemesOwned.value = true
            BillingConstants.REMOVE_ADS -> _removeAdsOwned.value = true
            BillingConstants.ANNUAL_PREMIUM -> _annualPremiumOwned.value = true
            BillingConstants.LIFETIME_PREMIUM -> _lifetimePremiumOwned.value = true
        }
    }
}
