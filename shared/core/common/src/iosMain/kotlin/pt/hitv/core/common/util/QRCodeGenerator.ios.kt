package pt.hitv.core.common.util

/**
 * iOS QR code generator — not needed (QR pairing is TV-only).
 */
actual object QRCodeGenerator {
    actual fun generateQRCode(content: String, size: Int): ByteArray? = null
    actual fun generateSessionId(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length).map { chars.random() }.joinToString("")
    }
}
