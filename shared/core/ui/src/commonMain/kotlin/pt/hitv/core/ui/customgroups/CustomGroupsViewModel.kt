package pt.hitv.core.ui.customgroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pt.hitv.core.model.Channel
import pt.hitv.core.model.CustomGroup
import pt.hitv.core.domain.repositories.CustomGroupRepository

/**
 * ViewModel for managing custom channel groups.
 * Ported from Hilt to plain ViewModel for Koin injection.
 */
class CustomGroupsViewModel(
    private val customGroupRepository: CustomGroupRepository
) : ViewModel() {

    val customGroups: StateFlow<List<CustomGroup>> = customGroupRepository.getAllCustomGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<CustomGroupsUiState>(CustomGroupsUiState.Idle)
    val uiState: StateFlow<CustomGroupsUiState> = _uiState.asStateFlow()

    private val _groupChannels = MutableStateFlow<List<Channel>>(emptyList())
    val groupChannels: StateFlow<List<Channel>> = _groupChannels.asStateFlow()

    fun createCustomGroup(name: String, icon: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = CustomGroupsUiState.Loading
                customGroupRepository.createCustomGroup(name, icon)
                _uiState.value = CustomGroupsUiState.Success("Group created successfully")
            } catch (e: Exception) {
                _uiState.value = CustomGroupsUiState.Error(e.message ?: "Failed to create group")
            }
        }
    }

    fun deleteCustomGroup(groupId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = CustomGroupsUiState.Loading
                customGroupRepository.deleteCustomGroup(groupId)
                _uiState.value = CustomGroupsUiState.Success("Group deleted successfully")
            } catch (e: Exception) {
                _uiState.value = CustomGroupsUiState.Error(e.message ?: "Failed to delete group")
            }
        }
    }

    fun updateCustomGroup(group: CustomGroup) {
        viewModelScope.launch {
            try {
                _uiState.value = CustomGroupsUiState.Loading
                customGroupRepository.updateCustomGroup(group)
                _uiState.value = CustomGroupsUiState.Success("Group updated successfully")
            } catch (e: Exception) {
                _uiState.value = CustomGroupsUiState.Error(e.message ?: "Failed to update group")
            }
        }
    }

    suspend fun getCustomGroupById(groupId: Long): CustomGroup? {
        return try {
            customGroupRepository.getCustomGroupById(groupId)
        } catch (e: Exception) {
            null
        }
    }

    fun loadGroupChannels(groupId: Long) {
        viewModelScope.launch {
            try {
                val channels = customGroupRepository.getChannelsInGroup(groupId)
                _groupChannels.value = channels
            } catch (e: Exception) {
                _groupChannels.value = emptyList()
            }
        }
    }

    fun removeChannelFromGroup(groupId: Long, channelId: Long) {
        viewModelScope.launch {
            try {
                customGroupRepository.removeChannelFromGroup(groupId, channelId)
                loadGroupChannels(groupId)
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }

    fun resetUiState() {
        _uiState.value = CustomGroupsUiState.Idle
    }
}

sealed class CustomGroupsUiState {
    data object Idle : CustomGroupsUiState()
    data object Loading : CustomGroupsUiState()
    data class Success(val message: String) : CustomGroupsUiState()
    data class Error(val message: String) : CustomGroupsUiState()
}
