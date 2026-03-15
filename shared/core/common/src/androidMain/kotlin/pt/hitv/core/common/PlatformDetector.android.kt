package pt.hitv.core.common

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Android implementation of PlatformDetector.
 * Uses Android system features and display metrics to detect device type.
 *
 * Requires [AndroidContextHolder.applicationContext] to be initialized at app startup.
 */
actual object PlatformDetector {

    /**
     * Checks if the device is an Android TV or set-top box.
     * Determined by the presence of the LEANBACK feature or UI_MODE_TYPE_TELEVISION.
     */
    actual fun isTvDevice(): Boolean {
        val context = AndroidContextHolder.applicationContext
        val hasLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        if (hasLeanback) return true

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Checks if the device is likely a tablet based on smallest screen width >= 600dp.
     */
    actual fun isTablet(): Boolean {
        val context = AndroidContextHolder.applicationContext
        val metrics = context.resources.displayMetrics
        val widthDp = metrics.widthPixels / metrics.density
        val heightDp = metrics.heightPixels / metrics.density
        val smallestWidth = minOf(widthDp, heightDp)
        return !isTvDevice() && smallestWidth >= 600
    }
}
