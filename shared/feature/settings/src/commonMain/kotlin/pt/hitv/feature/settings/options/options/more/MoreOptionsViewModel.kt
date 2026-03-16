package pt.hitv.feature.settings.options.options.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.ThemeManager

data class MoreOptionsUiState(
    val channelPreviewEnabled: Boolean = true,
    val currentAccountInfo: Triple<String, String, String?>? = null,
    val currentLanguage: String = "",
    val lastFocusedItemId: String? = null,
    val shouldRestoreFocus: Boolean = false
)

class MoreOptionsViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val accountManagerRepository: AccountManagerRepository,
    private val themeManager: ThemeManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreOptionsUiState())
    val uiState: StateFlow<MoreOptionsUiState> = _uiState.asStateFlow()

    fun saveLastFocusedItem(itemId: String) {
        _uiState.update { it.copy(lastFocusedItemId = itemId) }
    }

    fun prepareFocusRestoration() {
        _uiState.update { it.copy(shouldRestoreFocus = true) }
    }

    fun consumeFocusRestoration() {
        _uiState.update { it.copy(shouldRestoreFocus = false) }
    }

    init {
        loadChannelPreviewSetting()
        loadCurrentAccountInfo()
        loadCurrentLanguage()
    }

    private fun loadChannelPreviewSetting() {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                val userCredentials = accountManagerRepository.getCredentialsByUserId(currentUserId)
                _uiState.update { it.copy(channelPreviewEnabled = userCredentials?.channelPreviewEnabled ?: true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(channelPreviewEnabled = true) }
            }
        }
    }

    fun updateChannelPreviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                accountManagerRepository.updateChannelPreviewEnabled(currentUserId, enabled)
                _uiState.update { it.copy(channelPreviewEnabled = enabled) }
            } catch (_: Exception) {}
        }
    }

    private fun loadCurrentAccountInfo() {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                val userCredentials = accountManagerRepository.getCredentialsByUserId(currentUserId)
                if (userCredentials != null) {
                    _uiState.update { it.copy(currentAccountInfo = Triple(userCredentials.username, userCredentials.hostname, userCredentials.expirationDate)) }
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshCurrentAccountInfo() { loadCurrentAccountInfo(); loadChannelPreviewSetting() }

    private fun loadCurrentLanguage() {
        _uiState.update { it.copy(currentLanguage = preferencesHelper.getAppLanguage()) }
    }

    fun updateAppLanguage(languageCode: String) {
        preferencesHelper.setAppLanguage(languageCode)
        _uiState.update { it.copy(currentLanguage = languageCode) }
    }
}
