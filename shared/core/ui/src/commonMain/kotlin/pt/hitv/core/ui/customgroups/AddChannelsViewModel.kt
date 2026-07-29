package pt.hitv.core.ui.customgroups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pt.hitv.core.model.Channel
import pt.hitv.core.domain.repositories.CustomGroupRepository

/**
 * ViewModel for the Add Channels screen.
 *
 * Paged, not list-based. The comment this replaced said "Uses simple list instead of Paging 3 for
 * KMP compatibility" — but Cash's multiplatform Paging is already used by the Channels, Movies and
 * Series tabs, so there was nothing to work around. The cost was real: `getAllChannelsList()` reads
 * the **entire** Channel table into a `List<Channel>` held in a StateFlow. On a 50k-channel account
 * that is 50k domain objects resident for as long as the screen is open, which is precisely the
 * memory profile iOS kills.
 *
 * The paged repository methods already existed and were already wired for invalidation
 * (`searchAllChannels`, `getAllChannels`); only this class was still calling the unpaged pair.
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

    /**
     * Channels to offer, paged. The 300 ms debounce is kept from the list version so typing does
     * not start a query per keystroke; `flatMapLatest` then discards the previous pager.
     *
     * `cachedIn` keeps the loaded pages across recomposition and configuration changes — without
     * it, rotating the device re-reads every page from scratch.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val channels: Flow<PagingData<Channel>> = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                customGroupRepository.getAllChannels()
            } else {
                customGroupRepository.searchAllChannels(query)
            }
        }
        .cachedIn(viewModelScope)

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
