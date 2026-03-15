package pt.hitv.core.common

import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiomPad

/**
 * iOS implementation of PlatformDetector.
 * Uses UIKit APIs to detect device type.
 */
actual object PlatformDetector {

    /**
     * iOS does not have TV devices in the same way Android does.
     * Always returns false. For tvOS support, a separate source set would be needed.
     */
    actual fun isTvDevice(): Boolean = false

    /**
     * Checks if the device is an iPad based on UIDevice userInterfaceIdiom.
     */
    actual fun isTablet(): Boolean {
        return UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad
    }
}
