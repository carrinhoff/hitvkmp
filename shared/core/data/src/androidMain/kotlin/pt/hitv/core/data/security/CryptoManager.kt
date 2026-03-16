package pt.hitv.core.data.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Android implementation of CryptoManager using Android KeyStore + EncryptedSharedPreferences.
 */
actual class CryptoManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "crypto_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val secretKey: SecretKey by lazy { getOrGenerateKey() }

    private fun getOrGenerateKey(): SecretKey {
        val existingKey = sharedPreferences.getString("encryption_key", null)
        return if (existingKey != null) {
            val decodedKey = Base64.decode(existingKey, Base64.DEFAULT)
            SecretKeySpec(decodedKey, 0, decodedKey.size, "AES")
        } else {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val newKey = keyGenerator.generateKey()
            sharedPreferences.edit()
                .putString("encryption_key", Base64.encodeToString(newKey.encoded, Base64.DEFAULT))
                .apply()
            newKey
        }
    }

    actual fun encryptPassword(password: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(password.toByteArray())
        val combined = iv + encryptedBytes
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    actual fun decryptPassword(encryptedPassword: String): String {
        return try {
            val combinedBytes = Base64.decode(encryptedPassword, Base64.DEFAULT)
            if (combinedBytes.size < 17) {
                Log.e(TAG, "Encrypted data too short: ${combinedBytes.size} bytes")
                return ""
            }
            val iv = combinedBytes.copyOfRange(0, 16)
            val encryptedBytes = combinedBytes.copyOfRange(16, combinedBytes.size)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt password", e)
            ""
        }
    }

    companion object {
        private const val TAG = "CryptoManager"
    }
}
