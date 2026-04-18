package pt.hitv.feature.player

import androidx.compose.runtime.Composable

/**
 * Keeps the screen on and hides system bars while a player is active.
 *
 * Platform actuals:
 * - Android: `WindowInsetsControllerCompat` to hide status/navigation bars +
 *   `FLAG_KEEP_SCREEN_ON` via `WindowCompat`.
 * - iOS: `UIApplication.sharedApplication.idleTimerDisabled = true` + status bar
 *   hidden, reverted in `DisposableEffect.onDispose`.
 */
@Composable
expect fun KeepScreenOnAndFullscreen()
