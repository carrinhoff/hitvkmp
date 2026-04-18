package pt.hitv.core.common

/**
 * Cross-platform app metadata provider.
 *
 * - Android: reads from `PackageManager.getPackageInfo(...)`.
 * - iOS: reads `CFBundleShortVersionString` and `CFBundleVersion` from `NSBundle.mainBundle`.
 */
expect class AppInfoProvider() {

    /** Human-readable version, e.g. `"1.0.0"`. */
    val versionName: String

    /** Monotonic integer build number, e.g. `15`. */
    val versionCode: Int
}
