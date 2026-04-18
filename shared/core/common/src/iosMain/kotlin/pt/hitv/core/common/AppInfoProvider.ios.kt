package pt.hitv.core.common

import platform.Foundation.NSBundle

/**
 * iOS implementation of [AppInfoProvider].
 *
 * Reads `CFBundleShortVersionString` for [versionName] and `CFBundleVersion` for
 * [versionCode] from `NSBundle.mainBundle.infoDictionary`.
 */
actual class AppInfoProvider {

    actual val versionName: String by lazy {
        val value = NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleShortVersionString")
        (value as? String) ?: "0.0.0"
    }

    actual val versionCode: Int by lazy {
        val value = NSBundle.mainBundle.infoDictionary?.get("CFBundleVersion")
        (value as? String)?.toIntOrNull() ?: (value as? Int) ?: 0
    }
}
