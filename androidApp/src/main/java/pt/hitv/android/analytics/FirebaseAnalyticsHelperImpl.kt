package pt.hitv.android.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.analytics.analytics
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ContentType
import pt.hitv.core.common.analytics.ScreenName

/**
 * Firebase implementation of AnalyticsHelper using GitLive Firebase KMP SDK.
 * Ported from the original Hilt-based FirebaseAnalyticsHelperImpl.
 */
class FirebaseAnalyticsHelperImpl : AnalyticsHelper {

    private val analytics = Firebase.analytics

    override fun logScreenView(screen: ScreenName, screenClass: String) {
        analytics.logEvent("screen_view", mapOf(
            "screen_name" to screen.screenName,
            "screen_class" to screenClass
        ))
    }

    override fun logCustomEvent(eventName: String, params: Map<String, Any>?) {
        analytics.logEvent(eventName, params)
    }

    override fun logSearch(term: String) {
        analytics.logEvent("search", mapOf(
            "search_term" to term
        ))
    }

    override fun logCategorySelected(categoryName: String) {
        analytics.logEvent("select_category", mapOf(
            "category_name" to categoryName
        ))
    }

    override fun logLogin(method: String) {
        analytics.logEvent("login", mapOf(
            "method" to method
        ))
    }

    override fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    override fun setUserProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value ?: "")
    }

    override fun logSwitchAccount(userId: String, hostname: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_SWITCH_ACCOUNT, mapOf(
            AnalyticsHelper.PARAM_SELECTED_USER_ID to userId,
            AnalyticsHelper.PARAM_SELECTED_HOSTNAME to hostname
        ))
    }

    override fun logAddAccountClicked() {
        analytics.logEvent(AnalyticsHelper.EVENT_ADD_ACCOUNT_CLICKED)
    }

    override fun logDeleteAccountIntent(userId: String, hostname: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_DELETE_ACCOUNT_INTENT, mapOf(
            AnalyticsHelper.PARAM_DELETED_USER_ID to userId,
            AnalyticsHelper.PARAM_DELETED_HOSTNAME to hostname
        ))
    }

    override fun logDeleteAccountConfirmed(userId: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_DELETE_ACCOUNT_CONFIRMED, mapOf(
            AnalyticsHelper.PARAM_DELETED_USER_ID to userId
        ))
    }

    override fun logPlaybackEvent(eventName: String, contentType: ContentType, itemId: String) {
        analytics.logEvent(eventName, mapOf(
            "content_type" to contentType.value,
            "item_id" to itemId
        ))
    }

    override fun logPremiumClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_CLICKED, mapOf(
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logThemeSelectionOpened(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_SELECTION_OPENED, mapOf(
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logThemeSelected(themeName: String, themeType: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_SELECTED, mapOf(
            AnalyticsHelper.PARAM_THEME_NAME to themeName,
            AnalyticsHelper.PARAM_THEME_TYPE to themeType,
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logThemeApplied(
        themeName: String,
        themeType: String,
        previousTheme: String?,
        sourceScreen: String
    ) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_APPLIED, buildMap {
            put(AnalyticsHelper.PARAM_THEME_NAME, themeName)
            put(AnalyticsHelper.PARAM_THEME_TYPE, themeType)
            previousTheme?.let { put(AnalyticsHelper.PARAM_PREVIOUS_THEME, it) }
            put(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        })
    }

    override fun logUnlockThemesClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_UNLOCK_THEMES_CLICKED, mapOf(
            AnalyticsHelper.PARAM_PURCHASE_TYPE to "themes",
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logRemoveAdsClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_REMOVE_ADS_CLICKED, mapOf(
            AnalyticsHelper.PARAM_PURCHASE_TYPE to "remove_ads",
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logPremiumPurchaseInitiated(purchaseType: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_PURCHASE_INITIATED, mapOf(
            AnalyticsHelper.PARAM_PURCHASE_TYPE to purchaseType,
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logPremiumPurchaseResult(purchaseType: String, result: String, sourceScreen: String) {
        val eventName = when (result) {
            "success" -> AnalyticsHelper.EVENT_PREMIUM_PURCHASE_SUCCESS
            else -> AnalyticsHelper.EVENT_PREMIUM_PURCHASE_FAILED
        }
        analytics.logEvent(eventName, mapOf(
            AnalyticsHelper.PARAM_PURCHASE_TYPE to purchaseType,
            AnalyticsHelper.PARAM_PURCHASE_RESULT to result,
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logTrialModalShown(devicePlatform: String, isEligible: Boolean) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_SHOWN, mapOf(
            AnalyticsHelper.PARAM_DEVICE_PLATFORM to devicePlatform,
            AnalyticsHelper.PARAM_IS_TRIAL_ELIGIBLE to isEligible.toString()
        ))
    }

    override fun logTrialModalAccepted(devicePlatform: String, timeToActionSeconds: Long) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_ACCEPTED, mapOf(
            AnalyticsHelper.PARAM_DEVICE_PLATFORM to devicePlatform,
            AnalyticsHelper.PARAM_TIME_TO_ACTION to timeToActionSeconds
        ))
    }

    override fun logTrialModalDismissed(devicePlatform: String, timeToActionSeconds: Long) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_DISMISSED, mapOf(
            AnalyticsHelper.PARAM_DEVICE_PLATFORM to devicePlatform,
            AnalyticsHelper.PARAM_TIME_TO_ACTION to timeToActionSeconds
        ))
    }

    override fun logPremiumTabViewed(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_TAB_VIEWED, mapOf(
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logPremiumScreenViewed(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_SCREEN_VIEWED, mapOf(
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logPremiumFeatureClicked(featureName: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_FEATURE_CLICKED, mapOf(
            AnalyticsHelper.PARAM_FEATURE_NAME to featureName,
            AnalyticsHelper.PARAM_SOURCE_SCREEN to sourceScreen
        ))
    }

    override fun logContentDetailLoaded(
        contentType: ContentType,
        contentId: String,
        loadTimeMs: Long,
        dataSource: String
    ) {
        analytics.logEvent("content_detail_loaded", mapOf(
            "content_type" to contentType.value,
            "content_id" to contentId,
            "load_time_ms" to loadTimeMs.toString(),
            "data_source" to dataSource
        ))
    }

    override fun logContentDetailLoadFailed(
        contentType: ContentType,
        contentId: String,
        failureReason: String,
        retryAttempt: Int
    ) {
        analytics.logEvent("content_detail_load_failed", mapOf(
            "content_type" to contentType.value,
            "content_id" to contentId,
            "failure_reason" to failureReason.take(100),
            "retry_attempt" to retryAttempt.toString()
        ))
    }

    override fun logToggleFavorite(
        contentType: ContentType,
        contentId: String,
        contentName: String?,
        isAdding: Boolean
    ) {
        val eventName = if (isAdding) "add_favorite" else "remove_favorite"
        analytics.logEvent(eventName, buildMap {
            put("content_type", contentType.value)
            put("item_id", contentId)
            contentName?.let { put("item_name", it) }
        })
    }

    override fun logError(
        errorType: String,
        errorMessage: String,
        screenName: String?,
        contentId: String?,
        throwable: Throwable?
    ) {
        analytics.logEvent("error_occurred", buildMap {
            put("error_type", errorType)
            put("error_message", errorMessage.take(100))
            screenName?.let { put("screen_name", it) }
            contentId?.let { put("content_id", it) }
            throwable?.let { put("error_details", it::class.simpleName ?: "Unknown") }
        })
    }

    override fun logParseError(
        contentType: ContentType,
        contentId: String,
        errorMessage: String,
        apiEndpoint: String?
    ) {
        analytics.logEvent("parse_error", buildMap {
            put("content_type", contentType.value)
            put("content_id", contentId)
            put("error_message", errorMessage.take(100))
            apiEndpoint?.let { put("api_endpoint", it) }
        })
    }

    override fun logNetworkError(
        endpoint: String,
        errorMessage: String,
        httpStatus: Int?,
        contentType: ContentType?
    ) {
        analytics.logEvent("network_error", buildMap {
            put("api_endpoint", endpoint.take(100))
            put("error_message", errorMessage.take(100))
            httpStatus?.let { put("http_status", it.toString()) }
            contentType?.let { put("content_type", it.value) }
        })
    }

    override fun logScreenLoadTime(
        screenName: ScreenName,
        loadTimeMs: Long,
        dataSource: String?
    ) {
        analytics.logEvent("screen_load_time", buildMap {
            put("screen_name", screenName.screenName)
            put("load_time_ms", loadTimeMs.toString())
            dataSource?.let { put("data_source", it) }
        })
    }

    override fun logUserStuck(
        screenName: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String?
    ) {
        analytics.logEvent("user_stuck", buildMap {
            put("screen_name", screenName.screenName)
            put("time_on_screen_seconds", timeOnScreenSeconds.toString())
            lastAction?.let { put("user_action", it) }
        })
    }

    override fun logBackPressed(
        currentScreen: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String?
    ) {
        analytics.logEvent("back_pressed", buildMap {
            put("screen_name", currentScreen.screenName)
            put("time_on_screen_seconds", timeOnScreenSeconds.toString())
            lastAction?.let { put("user_action", it) }
        })
    }
}
