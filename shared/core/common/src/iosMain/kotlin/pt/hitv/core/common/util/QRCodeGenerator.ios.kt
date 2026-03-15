package pt.hitv.core.common.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreImage.CIContext
import platform.CoreImage.CIFilter
import platform.CoreImage.filterWithName
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation

/**
 * iOS implementation using CoreImage CIFilter for QR code generation.
 */
@OptIn(ExperimentalForeignApi::class)
actual object QRCodeGenerator {

    actual fun generateQRCode(content: String, size: Int): ByteArray? {
        return try {
            val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
                ?: return null

            val filter = CIFilter.filterWithName("CIQRCodeGenerator") ?: return null
            filter.setValue(data, forKey = "inputMessage")
            filter.setValue("H", forKey = "inputCorrectionLevel")

            val outputImage = filter.outputImage ?: return null

            val context = CIContext()
            val cgImage = context.createCGImage(outputImage, fromRect = outputImage.extent)
                ?: return null

            // Scale the image to the requested size
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            val bitmapContext = CGBitmapContextCreate(
                data = null,
                width = size.toULong(),
                height = size.toULong(),
                bitsPerComponent = 8u,
                bytesPerRow = (4 * size).toULong(),
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
            ) ?: return null

            // Draw without interpolation for sharp QR code
            CGContextDrawImage(bitmapContext, CGRectMake(0.0, 0.0, size.toDouble(), size.toDouble()), cgImage)

            val scaledImage = CGBitmapContextCreateImage(bitmapContext) ?: return null
            val uiImage = UIImage(cGImage = scaledImage)
            val pngData = UIImagePNGRepresentation(uiImage) ?: return null

            val bytes = ByteArray(pngData.length.toInt())
            bytes.usePinned { pinned ->
                pngData.getBytes(pinned.addressOf(0), pngData.length)
            }
            bytes
        } catch (e: Exception) {
            null
        }
    }

    actual fun generateSessionId(length: Int): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}
