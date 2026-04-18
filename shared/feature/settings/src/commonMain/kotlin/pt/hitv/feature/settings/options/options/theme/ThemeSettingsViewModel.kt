package pt.hitv.feature.settings.options.options.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.designsystem.theme.ThemeManager.AppTheme

/**
 * View model for the Theme Studio screen.
 *
 * Theme Studio ships ungated per plan — every [AppTheme] is selectable regardless
 * of premium/IAP status. The view model intentionally bypasses [ThemeManager.setTheme]'s
 * premium gate by writing the theme tag directly + notifying listeners via the manager API.
 */
class ThemeSettingsViewModel(
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(themeManager.getCurrentTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    /** All themes are selectable — no IAP gate. */
    val availableThemes: StateFlow<List<AppTheme>> =
        MutableStateFlow(AppTheme.values().toList()).asStateFlow()

    // Retained for any existing premium UI consumers; no longer used as a gate here.
    private val _hasPremiumThemes = MutableStateFlow(true)
    val hasPremiumThemes: StateFlow<Boolean> = _hasPremiumThemes.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    sealed class PurchaseState {
        data object Idle : PurchaseState()
        data object Loading : PurchaseState()
        data object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            // Ungated selection: call through ThemeManager, which persists + notifies.
            // For premium themes, setTheme() returns false due to its internal gate,
            // so we fall back to the ungated path that writes the preference + notifies.
            val accepted = themeManager.setTheme(theme)
            if (!accepted) themeManager.selectThemeUngated(theme)
            _currentTheme.value = theme
        }
    }

    fun resetPurchaseState() { _purchaseState.value = PurchaseState.Idle }
}
