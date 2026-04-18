package pt.hitv.feature.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication
import platform.UIKit.setStatusBarHidden

/**
 * iOS actual for [KeepScreenOnAndFullscreen].
 *
 * Mirrors the Android flow of "hide system bars + keep the screen on while the player
 * is foregrounded" using UIKit primitives:
 * - `idleTimerDisabled = true` prevents the device from auto-locking during playback
 *   (same intent as `FLAG_KEEP_SCREEN_ON`).
 * - `setStatusBarHidden(true, animated = false)` hides the status bar for an edge-to-edge
 *   video experience, equivalent to `WindowInsetsControllerCompat.hide(systemBars())`.
 *
 * Reverted in `onDispose` so leaving the player returns the UI chrome to normal.
 *
 * NOTE: `setStatusBarHidden` is deprecated in favor of per-UIViewController
 * `prefersStatusBarHidden`, but our ComposeUIViewController doesn't expose that override
 * idiomatically from Kotlin, so we fall back to the app-wide API. This matches the
 * simplicity the plan asks for; if Apple ever removes it we can switch to a status-bar-
 * manager-less approach via `Info.plist` `UIViewControllerBasedStatusBarAppearance = NO`.
 */
@Composable
actual fun KeepScreenOnAndFullscreen() {
    DisposableEffect(Unit) {
        val app = UIApplication.sharedApplication
        app.idleTimerDisabled = true
        app.setStatusBarHidden(true, animated = false)
        onDispose {
            app.idleTimerDisabled = false
            app.setStatusBarHidden(false, animated = false)
        }
    }
}
