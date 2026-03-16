package pt.hitv.android.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import pt.hitv.android.navigation.HitvMainScreen

/**
 * Root composable for the HITV Android app.
 *
 * Sets up Voyager Navigator and delegates to [HitvMainScreen]
 * for adaptive navigation chrome (bottom bar, side rail, TV drawer).
 */
@Composable
fun HitvApp() {
    HitvMainScreen(
        modifier = Modifier.fillMaxSize()
    )
}
