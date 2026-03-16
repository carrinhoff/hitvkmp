package pt.hitv.core.ui.customgroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pt.hitv.core.model.Channel
import pt.hitv.core.domain.repositories.CustomGroupRepository

/**
 * ViewModel for the Add Channels screen.
 * Ported from Hilt to plain ViewModel for Koin injection.
 * Uses simple list instead of Paging 3 for KMP compatibility.
 */
class AddChannelsViewModel(
    private val customGroupRepository: CustomGroupRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedChannels = MutableStateFlow<List<Channel>>(emptyList())
    val selectedChannels: StateFlow<List<Channel>> = _selectedChannels.asStateFlow()

    private val _existingChannelIds = MutableStateFlow<Set<Long>>(emptySet())
    val existingChannelIds: StateFlow<Set<Long>> = _existingChannelIds.asStateFlow()

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // React to search query changes
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .collectLatest { query ->
                    loadChannels(query)
                }
        }
    }

    private suspend fun loadChannels(query: String) {
        try {
            _isLoading.value = true
            val channels = if (query.isBlank()) {
                customGroupRepository.getAllChannelsList()
            } else {
                customGroupRepository.searchAllChannelsList(query)
            }
            _allChannels.value = channels
        } catch (e: Exception) {
            _allChannels.value = emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    fun loadExistingChannels(groupId: Long) {
        viewModelScope.launch {
            try {
                val channels = customGroupRepository.getChannelsInGroup(groupId)
                _existingChannelIds.value = channels.mapNotNull { it.id?.toLongOrNull() }.toSet()
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleChannelSelection(channel: Channel) {
        val currentList = _selectedChannels.value.toMutableList()
        val existing = currentList.find { it.id == channel.id }

        if (existing != null) {
            currentList.remove(existing)
        } else {
            currentList.add(channel)
        }

        _selectedChannels.value = currentList
    }

    fun addSelectedChannels(groupId: Long) {
        viewModelScope.launch {
            try {
                val channelsToAdd = _selectedChannels.value.mapNotNull { channel ->
                    val channelId = channel.id?.toLongOrNull()
                    if (channelId != null) {
                        Pair(channelId, 0)
                    } else {
                        null
                    }
                }

                customGroupRepository.addChannelsToGroup(groupId, channelsToAdd)
                _selectedChannels.value = emptyList()
            } catch (e: Exception) {
                // Error handled silently
            }
        }
    }

    fun clearSelection() {
        _selectedChannels.value = emptyList()
    }
}
