package pt.hitv.core.designsystem.modifiers

import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.*

/**
 * D-pad clickable modifier for TV apps (Android-only).
 *
 * Handles both short clicks and long presses using Android's native repeatCount:
 * - Short press: Normal click action
 * - Long press (hold ~500ms): Long press action
 *
 * This is more reliable than manual timing as it uses the system's built-in
 * key repeat mechanism.
 *
 * Uses a state flag to prevent triggering both long press AND click.
 */
fun Modifier.dpadClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
): Modifier = composed {
    var longPressTriggered by remember { mutableStateOf(false) }

    this.onKeyEvent { keyEvent ->
        if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {

            when (keyEvent.type) {
                KeyEventType.KeyDown -> {
                    if (keyEvent.nativeKeyEvent.repeatCount == 0) {
                        longPressTriggered = false
                    } else if (keyEvent.nativeKeyEvent.repeatCount == 1) {
                        longPressTriggered = true
                        onLongClick()
                    }
                    return@onKeyEvent true
                }
                KeyEventType.KeyUp -> {
                    if (!longPressTriggered) {
                        onClick()
                    }
                    return@onKeyEvent true
                }
            }
        }
        false
    }
}
