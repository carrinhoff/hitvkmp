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
        analytics.logEvent("screen_view") {
            param("screen_name", screen.screenName)
            param("screen_class", screenClass)
        }
    }

    override fun logCustomEvent(eventName: String, params: Map<String, Any>?) {
        analytics.logEvent(eventName) {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Boolean -> param(key, value.toString())
                    else -> param(key, value.toString())
                }
            }
        }
    }

    override fun logSearch(term: String) {
        analytics.logEvent("search") {
            param("search_term", term)
        }
    }

    override fun logCategorySelected(categoryName: String) {
        analytics.logEvent("select_category") {
            param("category_name", categoryName)
        }
    }

    override fun logLogin(method: String) {
        analytics.logEvent("login") {
            param("method", method)
        }
    }

    override fun setUserId(userId: String?) {
        analytics.setUserId(userId)
    }

    override fun setUserProperty(key: String, value: String?) {
        analytics.setUserProperty(key, value ?: "")
    }

    override fun logSwitchAccount(userId: String, hostname: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_SWITCH_ACCOUNT) {
            param(AnalyticsHelper.PARAM_SELECTED_USER_ID, userId)
            param(AnalyticsHelper.PARAM_SELECTED_HOSTNAME, hostname)
        }
    }

    override fun logAddAccountClicked() {
        analytics.logEvent(AnalyticsHelper.EVENT_ADD_ACCOUNT_CLICKED) {}
    }

    override fun logDeleteAccountIntent(userId: String, hostname: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_DELETE_ACCOUNT_INTENT) {
            param(AnalyticsHelper.PARAM_DELETED_USER_ID, userId)
            param(AnalyticsHelper.PARAM_DELETED_HOSTNAME, hostname)
        }
    }

    override fun logDeleteAccountConfirmed(userId: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_DELETE_ACCOUNT_CONFIRMED) {
            param(AnalyticsHelper.PARAM_DELETED_USER_ID, userId)
        }
    }

    override fun logPlaybackEvent(eventName: String, contentType: ContentType, itemId: String) {
        analytics.logEvent(eventName) {
            param("content_type", contentType.value)
            param("item_id", itemId)
        }
    }

    override fun logPremiumClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_CLICKED) {
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logThemeSelectionOpened(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_SELECTION_OPENED) {
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logThemeSelected(themeName: String, themeType: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_SELECTED) {
            param(AnalyticsHelper.PARAM_THEME_NAME, themeName)
            param(AnalyticsHelper.PARAM_THEME_TYPE, themeType)
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logThemeApplied(
        themeName: String,
        themeType: String,
        previousTheme: String?,
        sourceScreen: String
    ) {
        analytics.logEvent(AnalyticsHelper.EVENT_THEME_APPLIED) {
            param(AnalyticsHelper.PARAM_THEME_NAME, themeName)
            param(AnalyticsHelper.PARAM_THEME_TYPE, themeType)
            previousTheme?.let { param(AnalyticsHelper.PARAM_PREVIOUS_THEME, it) }
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logUnlockThemesClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_UNLOCK_THEMES_CLICKED) {
            param(AnalyticsHelper.PARAM_PURCHASE_TYPE, "themes")
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logRemoveAdsClicked(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_REMOVE_ADS_CLICKED) {
            param(AnalyticsHelper.PARAM_PURCHASE_TYPE, "remove_ads")
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logPremiumPurchaseInitiated(purchaseType: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_PURCHASE_INITIATED) {
            param(AnalyticsHelper.PARAM_PURCHASE_TYPE, purchaseType)
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logPremiumPurchaseResult(purchaseType: String, result: String, sourceScreen: String) {
        val eventName = when (result) {
            "success" -> AnalyticsHelper.EVENT_PREMIUM_PURCHASE_SUCCESS
            else -> AnalyticsHelper.EVENT_PREMIUM_PURCHASE_FAILED
        }
        analytics.logEvent(eventName) {
            param(AnalyticsHelper.PARAM_PURCHASE_TYPE, purchaseType)
            param(AnalyticsHelper.PARAM_PURCHASE_RESULT, result)
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logTrialModalShown(devicePlatform: String, isEligible: Boolean) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_SHOWN) {
            param(AnalyticsHelper.PARAM_DEVICE_PLATFORM, devicePlatform)
            param(AnalyticsHelper.PARAM_IS_TRIAL_ELIGIBLE, isEligible.toString())
        }
    }

    override fun logTrialModalAccepted(devicePlatform: String, timeToActionSeconds: Long) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_ACCEPTED) {
            param(AnalyticsHelper.PARAM_DEVICE_PLATFORM, devicePlatform)
            param(AnalyticsHelper.PARAM_TIME_TO_ACTION, timeToActionSeconds)
        }
    }

    override fun logTrialModalDismissed(devicePlatform: String, timeToActionSeconds: Long) {
        analytics.logEvent(AnalyticsHelper.EVENT_TRIAL_MODAL_DISMISSED) {
            param(AnalyticsHelper.PARAM_DEVICE_PLATFORM, devicePlatform)
            param(AnalyticsHelper.PARAM_TIME_TO_ACTION, timeToActionSeconds)
        }
    }

    override fun logPremiumTabViewed(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_TAB_VIEWED) {
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logPremiumScreenViewed(sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_SCREEN_VIEWED) {
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logPremiumFeatureClicked(featureName: String, sourceScreen: String) {
        analytics.logEvent(AnalyticsHelper.EVENT_PREMIUM_FEATURE_CLICKED) {
            param(AnalyticsHelper.PARAM_FEATURE_NAME, featureName)
            param(AnalyticsHelper.PARAM_SOURCE_SCREEN, sourceScreen)
        }
    }

    override fun logContentDetailLoaded(
        contentType: ContentType,
        contentId: String,
        loadTimeMs: Long,
        dataSource: String
    ) {
        analytics.logEvent("content_detail_loaded") {
            param("content_type", contentType.value)
            param("content_id", contentId)
            param("load_time_ms", loadTimeMs.toString())
            param("data_source", dataSource)
        }
    }

    override fun logContentDetailLoadFailed(
        contentType: ContentType,
        contentId: String,
        failureReason: String,
        retryAttempt: Int
    ) {
        analytics.logEvent("content_detail_load_failed") {
            param("content_type", contentType.value)
            param("content_id", contentId)
            param("failure_reason", failureReason.take(100))
            param("retry_attempt", retryAttempt.toString())
        }
    }

    override fun logToggleFavorite(
        contentType: ContentType,
        contentId: String,
        contentName: String?,
        isAdding: Boolean
    ) {
        val eventName = if (isAdding) "add_favorite" else "remove_favorite"
        analytics.logEvent(eventName) {
            param("content_type", contentType.value)
            param("item_id", contentId)
            contentName?.let { param("item_name", it) }
        }
    }

    override fun logError(
        errorType: String,
        errorMessage: String,
        screenName: String?,
        contentId: String?,
        throwable: Throwable?
    ) {
        analytics.logEvent("error_occurred") {
            param("error_type", errorType)
            param("error_message", errorMessage.take(100))
            screenName?.let { param("screen_name", it) }
            contentId?.let { param("content_id", it) }
            throwable?.let { param("error_details", it::class.simpleName ?: "Unknown") }
        }
    }

    override fun logParseError(
        contentType: ContentType,
        contentId: String,
        errorMessage: String,
        apiEndpoint: String?
    ) {
        analytics.logEvent("parse_error") {
            param("content_type", contentType.value)
            param("content_id", contentId)
            param("error_message", errorMessage.take(100))
            apiEndpoint?.let { param("api_endpoint", it) }
        }
    }

    override fun logNetworkError(
        endpoint: String,
        errorMessage: String,
        httpStatus: Int?,
        contentType: ContentType?
    ) {
        analytics.logEvent("network_error") {
            param("api_endpoint", endpoint.take(100))
            param("error_message", errorMessage.take(100))
            httpStatus?.let { param("http_status", it.toString()) }
            contentType?.let { param("content_type", it.value) }
        }
    }

    override fun logScreenLoadTime(
        screenName: ScreenName,
        loadTimeMs: Long,
        dataSource: String?
    ) {
        analytics.logEvent("screen_load_time") {
            param("screen_name", screenName.screenName)
            param("load_time_ms", loadTimeMs.toString())
            dataSource?.let { param("data_source", it) }
        }
    }

    override fun logUserStuck(
        screenName: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String?
    ) {
        analytics.logEvent("user_stuck") {
            param("screen_name", screenName.screenName)
            param("time_on_screen_seconds", timeOnScreenSeconds.toString())
            lastAction?.let { param("user_action", it) }
        }
    }

    override fun logBackPressed(
        currentScreen: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String?
    ) {
        analytics.logEvent("back_pressed") {
            param("screen_name", currentScreen.screenName)
            param("time_on_screen_seconds", timeOnScreenSeconds.toString())
            lastAction?.let { param("user_action", it) }
        }
    }
}
