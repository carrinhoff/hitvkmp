package pt.hitv.core.common.util

/**
 * Platform-specific QR code generator.
 * - Android: Uses ZXing library
 * - iOS: Uses CoreImage CIFilter
 */
expect object QRCodeGenerator {

    /**
     * Generates a QR code as raw pixel data (RGBA bytes).
     * @param content The text to encode in the QR code
     * @param size The size of the QR code in pixels (width = height)
     * @return Raw RGBA byte array of the QR code image, or null on failure
     */
    fun generateQRCode(content: String, size: Int): ByteArray?

    /**
     * Generates a random session ID for pairing.
     * @param length The length of the session ID (default 6)
     * @return A random alphanumeric session ID
     */
    fun generateSessionId(length: Int = 6): String
}
