package pt.hitv.core.navigation.adaptive

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import pt.hitv.core.model.enums.MainTab

/**
 * Manages per-tab state including scroll-to-top signals.
 * Scroll-to-top is triggered by incrementing the signal counter when the user
 * re-selects an already active tab.
 */
class TabState {
    private val scrollToTopSignals: Map<MainTab, MutableState<Int>> =
        MainTab.entries.associateWith { mutableIntStateOf(0) }

    /**
     * Get the current scroll-to-top signal value for a tab.
     * Feature screens observe this and scroll to top when it changes.
     */
    fun scrollToTopSignal(tab: MainTab): Int =
        scrollToTopSignals[tab]?.value ?: 0

    /**
     * Request scroll-to-top for a tab. Called when the user taps an already-active tab.
     */
    fun requestScrollToTop(tab: MainTab) {
        scrollToTopSignals[tab]?.let { state ->
            state.value = state.value + 1
        }
    }
}
