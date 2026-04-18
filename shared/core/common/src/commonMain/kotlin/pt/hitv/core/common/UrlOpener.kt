package pt.hitv.core.common

/**
 * Cross-platform external URL opener.
 *
 * - Android: starts an `ACTION_VIEW` Intent on the application context.
 * - iOS: delegates to `UIApplication.sharedApplication.openURL(_:options:completionHandler:)`.
 *
 * Failures (malformed URL, no handler app) are swallowed silently; callers that
 * need feedback should validate URLs beforehand.
 */
expect class UrlOpener() {

    /**
     * Opens [url] in the platform's default URL handler (browser, external app, etc.).
     */
    fun open(url: String)
}
