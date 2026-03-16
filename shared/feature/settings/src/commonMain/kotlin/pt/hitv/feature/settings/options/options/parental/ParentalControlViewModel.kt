package pt.hitv.feature.settings.options.options.parental

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.model.Category
import pt.hitv.core.model.ParentalControl
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.domain.manager.ParentalControlManager

class ParentalControlViewModel(
    private val parentalControlManager: ParentalControlManager,
    private val preferencesHelper: PreferencesHelper,
    private val streamRepository: StreamRepository
) : ViewModel() {

    private val userId: Int get() = preferencesHelper.getUserId()

    private val _uiState = MutableStateFlow(ParentalControlUiState())
    val uiState: StateFlow<ParentalControlUiState> = _uiState.asStateFlow()

    private val _sessionRefreshTrigger = MutableStateFlow(0L)

    val sessionTimeoutMinutes: StateFlow<Int> = _sessionRefreshTrigger.flatMapLatest {
        flow {
            while (true) { emit(parentalControlManager.getSessionTimeoutMinutes()); kotlinx.coroutines.delay(1000) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    val isSessionActive: StateFlow<Boolean> = _sessionRefreshTrigger.flatMapLatest {
        flow {
            while (true) { emit(parentalControlManager.isSessionAuthenticated()); kotlinx.coroutines.delay(1000) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val remainingSessionTime: StateFlow<Long> = _sessionRefreshTrigger.flatMapLatest {
        flow {
            while (true) { emit(parentalControlManager.getRemainingSessionTime()); kotlinx.coroutines.delay(1000) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    private fun refreshSessionState() { _sessionRefreshTrigger.value = Clock.System.now().toEpochMilliseconds() }

    init { loadData() }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                parentalControlManager.getAllParentalControls(userId),
                streamRepository.getAllChannelCategoriesForParentalControl(userId),
                parentalControlManager.getProtectedCategoriesCount(userId)
            ) { parentalControls, categories, protectedCount -> Triple(parentalControls, categories, protectedCount) }
                .collect { (parentalControls, categories, protectedCount) ->
                    val hasPin = preferencesHelper.hasParentalControlPin()
                    val isEnabled = parentalControlManager.isParentalControlEnabled()
                    val categoryProtectionMap = parentalControls.associateBy { it.categoryId }
                    val categoriesWithStatus = categories.map { category ->
                        CategoryProtectionStatus(category = category, isProtected = categoryProtectionMap[category.categoryId]?.isProtected ?: false)
                    }
                    _uiState.value = ParentalControlUiState(hasPinSet = hasPin, isEnabled = isEnabled, categories = categoriesWithStatus, protectedCategoriesCount = protectedCount, isLoading = false)
                }
        }
    }

    fun setPin(pin: String) {
        viewModelScope.launch {
            parentalControlManager.setPin(pin); parentalControlManager.validatePin(pin)
            _uiState.value = _uiState.value.copy(hasPinSet = true, isEnabled = true, showSetPinDialog = false); refreshSessionState()
        }
    }

    fun validatePin(pin: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch { if (parentalControlManager.validatePin(pin)) { refreshSessionState(); onSuccess() } else onError() }
    }

    fun toggleCategoryProtection(categoryId: Int, categoryName: String, isProtected: Boolean) {
        viewModelScope.launch { parentalControlManager.setCategoryProtection(categoryId = categoryId, categoryName = categoryName, userId = userId, isProtected = isProtected) }
    }

    fun removePin(onSuccess: () -> Unit) {
        viewModelScope.launch {
            parentalControlManager.clearPin(); parentalControlManager.deleteAllParentalControls(userId)
            _uiState.value = _uiState.value.copy(hasPinSet = false, isEnabled = false); onSuccess()
        }
    }

    fun showSetPinDialog() { _uiState.value = _uiState.value.copy(showSetPinDialog = true) }
    fun hideSetPinDialog() { _uiState.value = _uiState.value.copy(showSetPinDialog = false) }
    fun showChangePinDialog() { _uiState.value = _uiState.value.copy(showChangePinDialog = true) }
    fun hideChangePinDialog() { _uiState.value = _uiState.value.copy(showChangePinDialog = false) }
    fun showRemovePinDialog() { _uiState.value = _uiState.value.copy(showRemovePinDialog = true) }
    fun hideRemovePinDialog() { _uiState.value = _uiState.value.copy(showRemovePinDialog = false) }

    fun changePin(currentPin: String, newPin: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            if (parentalControlManager.validatePin(currentPin)) {
                parentalControlManager.setPin(newPin); parentalControlManager.validatePin(newPin)
                _uiState.value = _uiState.value.copy(showChangePinDialog = false); refreshSessionState(); onSuccess()
            } else onError()
        }
    }

    fun setSessionTimeout(minutes: Int) { parentalControlManager.setSessionTimeout(minutes); refreshSessionState() }
    fun endSession() { parentalControlManager.endSession(); refreshSessionState() }
}

data class ParentalControlUiState(
    val hasPinSet: Boolean = false, val isEnabled: Boolean = false,
    val categories: List<CategoryProtectionStatus> = emptyList(), val protectedCategoriesCount: Int = 0,
    val isLoading: Boolean = true, val showSetPinDialog: Boolean = false,
    val showChangePinDialog: Boolean = false, val showRemovePinDialog: Boolean = false
)

data class CategoryProtectionStatus(val category: Category, val isProtected: Boolean)
