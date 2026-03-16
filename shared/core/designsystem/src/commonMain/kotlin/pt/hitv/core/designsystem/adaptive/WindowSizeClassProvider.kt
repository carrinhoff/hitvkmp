package pt.hitv.core.designsystem.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Simplified Window Size Class provider for Compose Multiplatform.
 * Replaces the Android-specific WindowSizeClass APIs with a cross-platform equivalent.
 */

enum class WindowWidthSizeClass {
    COMPACT,   // width < 600dp (most phones in portrait)
    MEDIUM,    // 600dp <= width < 840dp (tablets in portrait)
    EXPANDED   // width >= 840dp (tablets in landscape, TVs)
}

enum class WindowHeightSizeClass {
    COMPACT,   // height < 480dp (phone landscape)
    MEDIUM,    // 480dp <= height < 900dp
    EXPANDED   // height >= 900dp (tablets in portrait)
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
)

/**
 * Device form factor classification based on window size.
 */
enum class DeviceFormFactor {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE_OR_TV
}

/**
 * Returns a simplified device form factor classification based on window size.
 */
fun WindowSizeClass.getDeviceFormFactor(): DeviceFormFactor {
    return when {
        widthSizeClass == WindowWidthSizeClass.COMPACT &&
            heightSizeClass == WindowHeightSizeClass.COMPACT -> DeviceFormFactor.PHONE_LANDSCAPE

        widthSizeClass == WindowWidthSizeClass.COMPACT -> DeviceFormFactor.PHONE_PORTRAIT

        widthSizeClass == WindowWidthSizeClass.MEDIUM -> DeviceFormFactor.TABLET_PORTRAIT

        else -> DeviceFormFactor.TABLET_LANDSCAPE_OR_TV
    }
}

/**
 * Returns the number of columns recommended for grid layouts based on window size.
 */
fun WindowSizeClass.getRecommendedGridColumns(
    compact: Int = 3,
    medium: Int = 4,
    expanded: Int = 5
): Int {
    return when (widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> compact
        WindowWidthSizeClass.MEDIUM -> medium
        WindowWidthSizeClass.EXPANDED -> expanded
    }
}

/**
 * Returns whether a two-pane layout should be used.
 */
fun WindowSizeClass.shouldUseTwoPane(): Boolean {
    return widthSizeClass == WindowWidthSizeClass.MEDIUM ||
        widthSizeClass == WindowWidthSizeClass.EXPANDED
}

/**
 * Returns whether to show navigation rail instead of bottom navigation.
 */
fun WindowSizeClass.shouldUseNavigationRail(): Boolean {
    return widthSizeClass == WindowWidthSizeClass.MEDIUM ||
        widthSizeClass == WindowWidthSizeClass.EXPANDED
}

// Helpers
fun WindowSizeClass.isCompactWidth(): Boolean = widthSizeClass == WindowWidthSizeClass.COMPACT
fun WindowSizeClass.isMediumWidth(): Boolean = widthSizeClass == WindowWidthSizeClass.MEDIUM
fun WindowSizeClass.isExpandedWidth(): Boolean = widthSizeClass == WindowWidthSizeClass.EXPANDED
fun WindowSizeClass.isCompactHeight(): Boolean = heightSizeClass == WindowHeightSizeClass.COMPACT
fun WindowSizeClass.isMediumHeight(): Boolean = heightSizeClass == WindowHeightSizeClass.MEDIUM
fun WindowSizeClass.isExpandedHeight(): Boolean = heightSizeClass == WindowHeightSizeClass.EXPANDED

fun WindowSizeClass.toDebugString(): String {
    return "${widthSizeClass.name} x ${heightSizeClass.name}"
}
