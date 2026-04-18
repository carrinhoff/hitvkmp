package pt.hitv.core.common

import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Android implementation of [UrlOpener].
 *
 * Uses [AndroidContextHolder.applicationContext] (the same pattern as
 * `PreferencesHelper.android.kt` + `PlatformDetector.android.kt`) so this class
 * has no constructor arguments and can be registered as a plain Koin `single`.
 */
actual class UrlOpener {

    actual fun open(url: String) {
        try {
            val context = AndroidContextHolder.applicationContext
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w("UrlOpener", "Failed to open url: $url", e)
        }
    }
}
