package pt.hitv.core.navigation.adaptive

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import pt.hitv.core.model.enums.MainTab

/**
 * Manages per-tab state including scroll-to-top signals and navigator stack depth
 * (used to hide the bottom nav chrome when the active tab has pushed a secondary screen).
 */
class TabState {
    private val scrollToTopSignals: Map<MainTab, MutableState<Int>> =
        MainTab.entries.associateWith { mutableIntStateOf(0) }

    /**
     * Per-tab navigator stack size. Each tab's Voyager Navigator reports into this
     * whenever it pushes/pops. `AdaptiveScaffold` reads the ACTIVE tab's entry to
     * decide whether to show or hide the bottom bar / nav rail.
     */
    private val navStackSizes: Map<MainTab, MutableState<Int>> =
        MainTab.entries.associateWith { mutableStateOf(1) }

    fun scrollToTopSignal(tab: MainTab): Int =
        scrollToTopSignals[tab]?.value ?: 0

    fun requestScrollToTop(tab: MainTab) {
        scrollToTopSignals[tab]?.let { state -> state.value = state.value + 1 }
    }

    /** Called by each tab's Navigator whenever its stack changes. */
    fun setNavStackSize(tab: MainTab, size: Int) {
        navStackSizes[tab]?.value = size
    }

    /** True when the given tab is on its root destination. */
    fun isTabOnRoot(tab: MainTab): Boolean =
        (navStackSizes[tab]?.value ?: 1) <= 1
}
