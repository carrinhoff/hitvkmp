package pt.hitv.core.common.analytics

/**
 * Content types for analytics tracking
 */
enum class ContentType(val value: String) {
    MOVIE("movie"),
    TV_SHOW("tv_show"),
    TV_EPISODE("tv_episode"),
    LIVE_CHANNEL("live_channel"),
    YOUTUBE_TRAILER("youtube_trailer")
}

/**
 * Screen names for analytics tracking
 */
enum class ScreenName(val screenName: String) {
    LOGIN("Login"),
    OPTIONS("Options"),
    LIVE_CHANNELS("Live Channels"),
    MOVIES("Movies"),
    SERIES("Series"),
    MOVIE_INFO("Movie Info"),
    SERIES_INFO("Series Info"),
    SEASON_DETAILS("Season Details"),
    EPG("EPG"),
    SWITCH_ACCOUNT("Switch Account"),
    MOVIE_PLAYER("Movie Player"),
    SERIES_PLAYER("Series Player"),
    LIVE_PLAYER("Live Player"),
    YOUTUBE_PLAYER("YouTube Player"),
    PREMIUM("Premium"),
    THEME_SETTINGS("Theme Settings"),
    PARENTAL_CONTROL("Parental Control"),
    MANAGE_CATEGORIES("Manage Categories"),
    CUSTOM_GROUPS("Custom Groups"),
    FEEDBACK("Feedback")
}

/**
 * Analytics helper interface for cross-module analytics tracking.
 * Implemented by platform-specific modules with Firebase Analytics (Android)
 * or equivalent (iOS).
 */
interface AnalyticsHelper {

    fun logScreenView(screen: ScreenName, screenClass: String)

    fun logCustomEvent(eventName: String, params: Map<String, Any>? = null)

    // Content & Navigation Analytics
    fun logSearch(term: String)
    fun logCategorySelected(categoryName: String)

    // Authentication Analytics
    fun logLogin(method: String)
    fun setUserId(userId: String?)
    fun setUserProperty(key: String, value: String?)

    // Account Management Analytics
    fun logSwitchAccount(userId: String, hostname: String)
    fun logAddAccountClicked()
    fun logDeleteAccountIntent(userId: String, hostname: String)
    fun logDeleteAccountConfirmed(userId: String)

    // Playback Analytics
    fun logPlaybackEvent(eventName: String, contentType: ContentType, itemId: String)

    // Premium Feature Analytics
    fun logPremiumClicked(sourceScreen: String)
    fun logThemeSelectionOpened(sourceScreen: String)
    fun logThemeSelected(themeName: String, themeType: String, sourceScreen: String)
    fun logThemeApplied(themeName: String, themeType: String, previousTheme: String?, sourceScreen: String)
    fun logUnlockThemesClicked(sourceScreen: String)
    fun logRemoveAdsClicked(sourceScreen: String)
    fun logPremiumPurchaseInitiated(purchaseType: String, sourceScreen: String)
    fun logPremiumPurchaseResult(purchaseType: String, result: String, sourceScreen: String)

    // Trial Offer Analytics
    fun logTrialModalShown(devicePlatform: String, isEligible: Boolean)
    fun logTrialModalAccepted(devicePlatform: String, timeToActionSeconds: Long)
    fun logTrialModalDismissed(devicePlatform: String, timeToActionSeconds: Long)

    // Premium Screen Analytics
    fun logPremiumTabViewed(sourceScreen: String)
    fun logPremiumScreenViewed(sourceScreen: String)
    fun logPremiumFeatureClicked(featureName: String, sourceScreen: String)

    // Content Detail Analytics
    fun logContentDetailLoaded(
        contentType: ContentType,
        contentId: String,
        loadTimeMs: Long,
        dataSource: String
    )

    fun logContentDetailLoadFailed(
        contentType: ContentType,
        contentId: String,
        failureReason: String,
        retryAttempt: Int = 0
    )

    // Favorite Analytics
    fun logToggleFavorite(
        contentType: ContentType,
        contentId: String,
        contentName: String?,
        isAdding: Boolean
    )

    // Error & Diagnostic Logging
    fun logError(
        errorType: String,
        errorMessage: String,
        screenName: String? = null,
        contentId: String? = null,
        throwable: Throwable? = null
    )

    fun logParseError(
        contentType: ContentType,
        contentId: String,
        errorMessage: String,
        apiEndpoint: String? = null
    )

    fun logNetworkError(
        endpoint: String,
        errorMessage: String,
        httpStatus: Int? = null,
        contentType: ContentType? = null
    )

    fun logScreenLoadTime(
        screenName: ScreenName,
        loadTimeMs: Long,
        dataSource: String? = null
    )

    fun logUserStuck(
        screenName: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String? = null
    )

    fun logBackPressed(
        currentScreen: ScreenName,
        timeOnScreenSeconds: Long,
        lastAction: String? = null
    )

    companion object {
        // Event Names
        const val EVENT_PREMIUM_CLICKED = "premium_clicked"
        const val EVENT_THEME_SELECTION_OPENED = "theme_selection_opened"
        const val EVENT_THEME_SELECTED = "theme_selected"
        const val EVENT_THEME_APPLIED = "theme_applied"
        const val EVENT_UNLOCK_THEMES_CLICKED = "unlock_themes_clicked"
        const val EVENT_REMOVE_ADS_CLICKED = "remove_ads_clicked"
        const val EVENT_PREMIUM_PURCHASE_INITIATED = "premium_purchase_initiated"
        const val EVENT_PREMIUM_PURCHASE_SUCCESS = "premium_purchase_success"
        const val EVENT_PREMIUM_PURCHASE_FAILED = "premium_purchase_failed"
        const val EVENT_TRIAL_MODAL_SHOWN = "trial_modal_shown"
        const val EVENT_TRIAL_MODAL_ACCEPTED = "trial_modal_accepted"
        const val EVENT_TRIAL_MODAL_DISMISSED = "trial_modal_dismissed"
        const val EVENT_PREMIUM_TAB_VIEWED = "premium_tab_viewed"
        const val EVENT_PREMIUM_SCREEN_VIEWED = "premium_screen_viewed"
        const val EVENT_PREMIUM_FEATURE_CLICKED = "premium_feature_clicked"
        const val EVENT_REFRESH_DATA_CLICKED = "refresh_data_clicked"

        // Playback Event Names
        const val EVENT_PLAYBACK_START = "playback_start"
        const val EVENT_PLAYBACK_PAUSE = "playback_pause"
        const val EVENT_PLAYBACK_COMPLETE = "playback_complete"

        // Account Event Names
        const val EVENT_SWITCH_ACCOUNT = "switch_account"
        const val EVENT_ADD_ACCOUNT_CLICKED = "add_account_clicked"
        const val EVENT_DELETE_ACCOUNT_INTENT = "delete_account_intent"
        const val EVENT_DELETE_ACCOUNT_CONFIRMED = "delete_account_confirmed"

        // Parameter Names
        const val PARAM_THEME_NAME = "theme_name"
        const val PARAM_THEME_TYPE = "theme_type"
        const val PARAM_PREVIOUS_THEME = "previous_theme"
        const val PARAM_PURCHASE_TYPE = "purchase_type"
        const val PARAM_PURCHASE_RESULT = "purchase_result"
        const val PARAM_SOURCE_SCREEN = "source_screen"
        const val PARAM_DEVICE_PLATFORM = "device_platform"
        const val PARAM_IS_TRIAL_ELIGIBLE = "is_trial_eligible"
        const val PARAM_TIME_TO_ACTION = "time_to_action_seconds"
        const val PARAM_FEATURE_NAME = "feature_name"
        const val PARAM_SELECTED_USER_ID = "selected_user_id"
        const val PARAM_SELECTED_HOSTNAME = "selected_hostname"
        const val PARAM_DELETED_USER_ID = "deleted_user_id"
        const val PARAM_DELETED_HOSTNAME = "deleted_hostname"
    }
}
