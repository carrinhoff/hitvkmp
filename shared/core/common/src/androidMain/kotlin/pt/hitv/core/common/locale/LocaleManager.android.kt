package pt.hitv.core.common.locale

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Android implementation of LocaleManager.
 * Applies locale changes via Android Context configuration.
 */
actual class LocaleManager {

    actual fun getCurrentLanguageName(languageCode: String): String {
        return SupportedLanguage.fromCode(languageCode)?.nativeName ?: "System Default"
    }

    /**
     * Apply language to an Android Context.
     */
    fun applyLanguage(context: Context, languageCode: String): Context {
        if (languageCode == "system" || languageCode.isEmpty()) {
            return context
        }

        val locale = when {
            languageCode.contains("-r") -> {
                val parts = languageCode.split("-r")
                Locale(parts[0], parts[1])
            }
            else -> Locale(languageCode)
        }

        Locale.setDefault(locale)

        val resources = context.resources
        val config = resources.configuration

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
            context
        }
    }

    /**
     * Restart the app to apply language changes.
     */
    fun restartApp(activity: Activity) {
        val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        activity.finishAffinity()
    }
}
