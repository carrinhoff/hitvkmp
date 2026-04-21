@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package pt.hitv.feature.player

import platform.Foundation.NSNumber
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientationLandscapeLeft
import platform.UIKit.UIDeviceOrientationLandscapeRight

/**
 * Force-rotates the app between portrait and landscape, mirroring the rotate
 * button in the Android player.
 *
 * Uses the KVC-on-`UIDevice` form (`setValue:forKey:` with `"orientation"`).
 * Technically a private API but it's the only single-call rotation approach
 * that works on iOS 13–17 from a non-SwiftUI context. The proper iOS 16+
 * replacement (`UIWindowScene.requestGeometryUpdate(.iOS(...))`) needs
 * `UIWindowSceneGeometryPreferencesIOS` — not yet in our cinterop.
 *
 * Info.plist declares portrait + landscapeLeft + landscapeRight under
 * `UISupportedInterfaceOrientations`, so the system honors the request.
 *
 * UIInterfaceOrientation values (NSInteger):
 *   1 = portrait, 2 = portraitUpsideDown, 3 = landscapeRight, 4 = landscapeLeft
 * We always target portrait or landscapeRight (=3) — the device picks up
 * which physical landscape orientation the user is holding it in.
 */
internal fun toggleDeviceOrientation() {
    val device = UIDevice.currentDevice
    val isLandscape = device.orientation == UIDeviceOrientationLandscapeLeft ||
        device.orientation == UIDeviceOrientationLandscapeRight
    val targetInterfaceOrientation = if (isLandscape) 1 else 3
    device.setValue(NSNumber.numberWithInt(targetInterfaceOrientation), forKey = "orientation")
}
