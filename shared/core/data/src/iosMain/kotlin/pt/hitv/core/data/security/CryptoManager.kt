package pt.hitv.core.data.security

/**
 * iOS CryptoManager — simplified (no encryption, same as Android KMP version).
 * Passwords are stored via PreferencesHelper (multiplatform-settings).
 */
actual class CryptoManager {
    actual fun encryptPassword(password: String): String = password
    actual fun decryptPassword(encryptedPassword: String): String = encryptedPassword
}
