package pt.hitv.core.data.security

/**
 * Platform-specific encryption manager for sensitive data (e.g., passwords).
 *
 * - Android: Uses Android KeyStore + EncryptedSharedPreferences with AES/CBC/PKCS5Padding
 * - iOS: Uses iOS Keychain Services for secure key storage + CommonCrypto for AES encryption
 */
expect class CryptoManager {
    /**
     * Encrypts a password using AES encryption.
     * @param password The plaintext password to encrypt.
     * @return The encrypted password as a Base64-encoded string (IV + ciphertext).
     */
    fun encryptPassword(password: String): String

    /**
     * Decrypts an encrypted password.
     * @param encryptedPassword The Base64-encoded encrypted password (IV + ciphertext).
     * @return The decrypted plaintext password, or empty string on failure.
     */
    fun decryptPassword(encryptedPassword: String): String
}
