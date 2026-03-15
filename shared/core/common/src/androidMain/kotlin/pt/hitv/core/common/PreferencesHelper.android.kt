package pt.hitv.core.common

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/**
 * Android: Holder for the application context, must be initialized at app startup.
 */
object AndroidContextHolder {
    lateinit var applicationContext: Context
}

/**
 * Android actual implementation using EncryptedSharedPreferences for secure storage.
 */
actual fun createEncryptedSettings(): Settings {
    val context = AndroidContextHolder.applicationContext
    return try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            "SPFile_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        SharedPreferencesSettings(encryptedPrefs)
    } catch (e: Exception) {
        Log.e("PreferencesHelper", "Failed to create EncryptedSharedPreferences, falling back to plain prefs", e)
        val fallbackPrefs = context.getSharedPreferences("SPFile", Context.MODE_PRIVATE)
        SharedPreferencesSettings(fallbackPrefs)
    }
}
