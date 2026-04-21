package pt.hitv.feature.player

/**
 * Android has an explicit "rotate" button because a lot of Android devices ship
 * with auto-rotate globally disabled. iOS doesn't need that affordance — rotating
 * the device itself rotates the app as long as the Info.plist's
 * `UISupportedInterfaceOrientations` lists the target orientation (we list
 * portrait + landscapeLeft + landscapeRight).
 *
 * Programmatic rotation on iOS is a minefield:
 *   - Pre-16: private `UIDevice setValue:forKey:"orientation"` KVC trick, which
 *     needs an NSNumber box. The Kotlin/Native cinterop for our target doesn't
 *     expose `NSNumber.numberWithLong` / `numberWithInt`, and the enum values
 *     `UIDeviceOrientation*` aren't importable as top-level constants either.
 *   - iOS 16+: `UIWindowScene.requestGeometryUpdate(.iOS(...))`, which needs
 *     `UIWindowSceneGeometryPreferencesIOS` — also not in our cinterop.
 *
 * So this helper is intentionally a no-op on iOS. The channel player's rotate
 * button calls it and nothing happens visually; the user rotates the device
 * physically and iOS handles the animation. If we later gain access to
 * `UIWindowSceneGeometryPreferencesIOS` via a cinterop upgrade, this is where
 * to wire it.
 */
internal fun toggleDeviceOrientation() {
    // Intentionally empty — see file-level KDoc.
}
