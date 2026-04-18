package pt.hitv.core.common

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Android implementation of [AppInfoProvider].
 *
 * Uses the application's [PackageManager] to read `versionName` + `versionCode`
 * (or `longVersionCode` on API 28+, truncated to Int).
 */
actual class AppInfoProvider {

    actual val versionName: String by lazy {
        try {
            val ctx = AndroidContextHolder.applicationContext
            val pkg: PackageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            pkg.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.w("AppInfoProvider", "Failed to read versionName", e)
            "0.0.0"
        }
    }

    actual val versionCode: Int by lazy {
        try {
            val ctx = AndroidContextHolder.applicationContext
            val pkg: PackageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode
            }
        } catch (e: Exception) {
            Log.w("AppInfoProvider", "Failed to read versionCode", e)
            0
        }
    }
}
