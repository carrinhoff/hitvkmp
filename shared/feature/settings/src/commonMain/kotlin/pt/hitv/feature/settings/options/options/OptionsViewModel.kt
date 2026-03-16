package pt.hitv.feature.settings.options.options

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.common.PreferencesHelper

data class OptionsUiState(
    val isLoading: Boolean = false
)

class OptionsViewModel(
    private val streamRepository: StreamRepository,
    private val userRepository: AccountManagerRepository,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(OptionsUiState())
    val uiState: StateFlow<OptionsUiState> = _uiState.asStateFlow()

    fun changeLoadingState(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    fun isXtreamAccount(): Boolean {
        return preferencesHelper.getPassword().isNotEmpty()
    }
}
