package pt.hitv.feature.settings.options.options.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.designsystem.theme.ThemeManager.AppTheme

class ThemeSettingsViewModel(
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _currentTheme = MutableStateFlow(themeManager.getCurrentTheme())
    val currentTheme: StateFlow<AppTheme> = _currentTheme.asStateFlow()

    private val _hasPremiumThemes = MutableStateFlow(themeManager.hasPremiumThemes())
    val hasPremiumThemes: StateFlow<Boolean> = _hasPremiumThemes.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    sealed class PurchaseState {
        data object Idle : PurchaseState()
        data object Loading : PurchaseState()
        data object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    init {
        _hasPremiumThemes.value = themeManager.hasPremiumThemes()
    }

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            val success = themeManager.setTheme(theme)
            if (success) _currentTheme.value = theme
        }
    }

    fun resetPurchaseState() { _purchaseState.value = PurchaseState.Idle }
}
