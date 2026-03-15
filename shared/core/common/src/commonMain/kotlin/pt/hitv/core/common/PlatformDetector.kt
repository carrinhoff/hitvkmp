package pt.hitv.core.common

/**
 * Platform detection utility that provides information about the current device type
 * and form factor.
 *
 * For UI layout decisions, prefer using window size classes from the design system
 * instead of device type detection.
 */
expect object PlatformDetector {

    /**
     * Checks if the device is a TV (Android TV / set-top box).
     * @return true if running on a TV device, false otherwise
     */
    fun isTvDevice(): Boolean

    /**
     * Checks if the device is likely a tablet (based on screen size).
     * @return true if the device appears to be a tablet, false otherwise
     */
    fun isTablet(): Boolean
}
