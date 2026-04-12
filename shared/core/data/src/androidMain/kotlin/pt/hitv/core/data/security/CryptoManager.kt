package pt.hitv.core.data.security

/**
 * Simplified Android CryptoManager - no encryption for KMP version.
 * Passwords are stored as-is (plain text).
 */
actual class CryptoManager {
    actual fun encryptPassword(password: String): String = password
    actual fun decryptPassword(encryptedPassword: String): String = encryptedPassword
}
