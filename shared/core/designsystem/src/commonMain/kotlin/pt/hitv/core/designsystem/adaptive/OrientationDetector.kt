package pt.hitv.core.designsystem.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Orientation detection for Compose Multiplatform.
 * Replaces Android's LocalConfiguration.current.orientation with a cross-platform solution.
 */
enum class Orientation {
    PORTRAIT,
    LANDSCAPE
}

/**
 * Detects the current orientation based on the available window dimensions.
 * Uses BoxWithConstraints which works on all Compose Multiplatform targets.
 */
@Composable
fun rememberOrientation(maxWidth: Int, maxHeight: Int): Orientation {
    return remember(maxWidth, maxHeight) {
        if (maxWidth > maxHeight) Orientation.LANDSCAPE else Orientation.PORTRAIT
    }
}
