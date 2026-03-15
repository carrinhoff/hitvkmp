package pt.hitv.core.common.analytics

/**
 * No-op implementation of AnalyticsHelper for testing or when analytics is disabled.
 */
class NoOpAnalyticsHelper : AnalyticsHelper {
    override fun logScreenView(screen: ScreenName, screenClass: String) {}
    override fun logCustomEvent(eventName: String, params: Map<String, Any>?) {}
    override fun logSearch(term: String) {}
    override fun logCategorySelected(categoryName: String) {}
    override fun logLogin(method: String) {}
    override fun setUserId(userId: String?) {}
    override fun setUserProperty(key: String, value: String?) {}
    override fun logSwitchAccount(userId: String, hostname: String) {}
    override fun logAddAccountClicked() {}
    override fun logDeleteAccountIntent(userId: String, hostname: String) {}
    override fun logDeleteAccountConfirmed(userId: String) {}
    override fun logPlaybackEvent(eventName: String, contentType: ContentType, itemId: String) {}
    override fun logPremiumClicked(sourceScreen: String) {}
    override fun logThemeSelectionOpened(sourceScreen: String) {}
    override fun logThemeSelected(themeName: String, themeType: String, sourceScreen: String) {}
    override fun logThemeApplied(themeName: String, themeType: String, previousTheme: String?, sourceScreen: String) {}
    override fun logUnlockThemesClicked(sourceScreen: String) {}
    override fun logRemoveAdsClicked(sourceScreen: String) {}
    override fun logPremiumPurchaseInitiated(purchaseType: String, sourceScreen: String) {}
    override fun logPremiumPurchaseResult(purchaseType: String, result: String, sourceScreen: String) {}
    override fun logTrialModalShown(devicePlatform: String, isEligible: Boolean) {}
    override fun logTrialModalAccepted(devicePlatform: String, timeToActionSeconds: Long) {}
    override fun logTrialModalDismissed(devicePlatform: String, timeToActionSeconds: Long) {}
    override fun logPremiumTabViewed(sourceScreen: String) {}
    override fun logPremiumScreenViewed(sourceScreen: String) {}
    override fun logPremiumFeatureClicked(featureName: String, sourceScreen: String) {}
    override fun logContentDetailLoaded(contentType: ContentType, contentId: String, loadTimeMs: Long, dataSource: String) {}
    override fun logContentDetailLoadFailed(contentType: ContentType, contentId: String, failureReason: String, retryAttempt: Int) {}
    override fun logToggleFavorite(contentType: ContentType, contentId: String, contentName: String?, isAdding: Boolean) {}
    override fun logError(errorType: String, errorMessage: String, screenName: String?, contentId: String?, throwable: Throwable?) {}
    override fun logParseError(contentType: ContentType, contentId: String, errorMessage: String, apiEndpoint: String?) {}
    override fun logNetworkError(endpoint: String, errorMessage: String, httpStatus: Int?, contentType: ContentType?) {}
    override fun logScreenLoadTime(screenName: ScreenName, loadTimeMs: Long, dataSource: String?) {}
    override fun logUserStuck(screenName: ScreenName, timeOnScreenSeconds: Long, lastAction: String?) {}
    override fun logBackPressed(currentScreen: ScreenName, timeOnScreenSeconds: Long, lastAction: String?) {}
}
