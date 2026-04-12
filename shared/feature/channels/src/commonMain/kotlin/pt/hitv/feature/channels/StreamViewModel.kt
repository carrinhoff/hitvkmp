package pt.hitv.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.cash.paging.PagingData
import app.cash.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.domain.usecases.GetChannelsByCategoryUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteChannelUseCase
import pt.hitv.core.model.*
import pt.hitv.core.data.paging.CHANNEL_FILTER_ALL
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.manager.UserSessionManager
import pt.hitv.core.domain.repositories.AccountManagerRepository

/**
 * UI State for Channels/Streams screen following NiA unidirectional data flow pattern.
 */
data class StreamUiState(
    val isLoading: Boolean = false,
    val isLoadingEpgCategories: Boolean = false,
    val currentCategoryFilter: String? = CHANNEL_FILTER_ALL,
    val currentSearchQuery: String? = null,
    val categories: List<Category> = emptyList(),
    val favorites: List<Channel> = emptyList(),
    val recentlyViewedChannels: List<Channel> = emptyList(),
    val cachedChannels: List<Channel>? = null,
    val currentChannelEpg: ChannelEpgInfo? = null,
    val allChannelsEpg: List<ChannelEpgInfo> = emptyList(),
    val categoriesWithEpg: List<Pair<Category, Int>> = emptyList(),
    val programmesForCategory: List<ChannelEpgInfo> = emptyList(),
    val selectedChannel: Channel? = null,
    val selectedPosition: String? = null,
    val fetchedChannel: Channel? = null,
    val lastClickedItemId: String? = null,
    val lastClickedItemPosition: Int = -1,
    val categoryCounts: Map<String, Int> = emptyMap()
)

/**
 * ViewModel for Live Channels screen.
 * Ported from Hilt to plain class for Koin injection.
 */
class StreamViewModel(
    private val userSessionManager: UserSessionManager,
    private val repository: StreamRepository,
    private val preferencesHelper: PreferencesHelper,
    private val accountManagerRepository: AccountManagerRepository,
    private val getChannelsByCategoryUseCase: GetChannelsByCategoryUseCase,
    private val toggleFavoriteChannelUseCase: ToggleFavoriteChannelUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StreamUiState())
    val uiState: StateFlow<StreamUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _currentUserId = userSessionManager.userIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferencesHelper.getUserId()
        )
    val currentUserId: StateFlow<Int> = _currentUserId

    private val _refreshPagingEvent = MutableSharedFlow<Unit>()
    val refreshPagingEvent: SharedFlow<Unit> = _refreshPagingEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val channelsPagerFlow: Flow<PagingData<Channel>> = combine(
        _currentUserId,
        _uiState.map { it.currentCategoryFilter }.distinctUntilChanged(),
        _uiState.map { it.currentSearchQuery }.distinctUntilChanged()
    ) { userId, category, query -> Triple(userId, category, query) }
        .flatMapLatest { (_, category, query) ->
            getChannelsByCategoryUseCase(category, query)
        }.cachedIn(viewModelScope)

    init {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            _currentUserId.collect { userId ->
                if (userId != -1) {
                    loadDefaultCategory()
                    fetchChannelCategories()
                    getFavorites()
                    fetchRecentlyViewedChannels()
                }
            }
        }
    }

    private fun loadDefaultCategory() {
        viewModelScope.launch {
            try {
                val defaultCategoryId = repository.getDefaultChannelCategoryId()
                val categoryId = if (defaultCategoryId != null) {
                    defaultCategoryId
                } else {
                    val defaultCustomGroupId = repository.getDefaultCustomGroupId()
                    defaultCustomGroupId ?: CHANNEL_FILTER_ALL
                }
                _uiState.update { it.copy(currentCategoryFilter = categoryId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentCategoryFilter = CHANNEL_FILTER_ALL) }
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        if (_uiState.value.currentCategoryFilter != category) {
            _uiState.update {
                it.copy(
                    currentCategoryFilter = category,
                    currentSearchQuery = null
                )
            }
        }
    }

    fun setSearchQuery(query: String?) {
        val trimmedQuery = query?.trim().takeIf { !it.isNullOrEmpty() }
        if (_uiState.value.currentSearchQuery != trimmedQuery) {
            _uiState.update { it.copy(currentSearchQuery = trimmedQuery) }
        }
    }

    fun invalidateChannelsPaging() {
        viewModelScope.launch {
            _refreshPagingEvent.emit(Unit)
        }
    }

    private var categoriesJob: Job? = null
    private var favoritesJob: Job? = null
    private var recentChannelsJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchChannelCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _currentUserId.flatMapLatest { userId ->
                repository.getAllChannelCategories(userId)
            }
                .catch { _uiState.update { it.copy(categories = emptyList(), isLoading = false) } }
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories, isLoading = false) }
                }
        }
    }

    fun getFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            repository.getFavoritesChannel()
                .catch { }
                .collect { channels ->
                    _uiState.update { it.copy(favorites = channels) }
                }
        }
    }

    fun fetchRecentlyViewedChannels() {
        recentChannelsJob?.cancel()
        recentChannelsJob = viewModelScope.launch {
            repository.getRecentlyViewedChannels()
                .catch { }
                .collect { channels ->
                    _uiState.update { it.copy(recentlyViewedChannels = channels) }
                }
        }
    }

    suspend fun getTotalChannelCount(): Int = repository.getTotalChannelCount()
    suspend fun getCategoryChannelCount(categoryId: String): Int = repository.getCategoryChannelCount(categoryId)

    fun fetchCategoryCounts() {
        viewModelScope.launch {
            try {
                val totalCount = getTotalChannelCount()
                _uiState.update { it.copy(categoryCounts = it.categoryCounts + (CHANNEL_FILTER_ALL to totalCount)) }

                _uiState.value.categories.forEach { category ->
                    val count = getCategoryChannelCount(category.categoryId.toString())
                    _uiState.update { it.copy(categoryCounts = it.categoryCounts + (category.categoryId.toString() to count)) }
                }
            } catch (_: Exception) {}
        }
    }

    fun fetchChannelsFromDB() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllChannelsFlow()
                    .catch {
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(cachedChannels = null) }
                        }
                    }
                    .collect { channelList ->
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(cachedChannels = channelList) }
                        }
                    }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(cachedChannels = null) }
                }
            }
        }
    }

    fun saveFavoriteChannel(channel: Channel, isFavoritesFilterActive: Boolean) {
        viewModelScope.launch {
            try {
                toggleFavoriteChannelUseCase(channel)
                getFavorites()
                // Always refresh paging to update the star icon
                _refreshPagingEvent.emit(Unit)
            } catch (e: Exception) { }
        }
    }

    fun saveRecentlyViewedChannel(channel: Channel, currentTimeMillis: Long) {
        viewModelScope.launch {
            try {
                repository.saveRecentlyViewedChannel(channel.copy(lastViewedTimestamp = currentTimeMillis))
                fetchRecentlyViewedChannels()
            } catch (e: Exception) { }
        }
    }

    fun fetchCurrentEpg(channel: Channel, currentTimeMillis: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(currentChannelEpg = null) }
            try {
                val epgData = repository.fetchCurrentEpg(channel, currentTimeMillis)
                _uiState.update { it.copy(currentChannelEpg = epgData) }
            } catch (e: Exception) {
                _uiState.update { it.copy(currentChannelEpg = null) }
            }
        }
    }

    suspend fun fetchCurrentEpgSuspend(channel: Channel, currentTimeMillis: Long): ChannelEpgInfo? {
        return try {
            repository.fetchCurrentEpg(channel, currentTimeMillis)
        } catch (e: Exception) {
            null
        }
    }

    fun getAllChannelsEpg() {
        viewModelScope.launch {
            try {
                val epgData = repository.getAllChannelsEpg()
                _uiState.update { it.copy(allChannelsEpg = epgData) }
            } catch (e: Exception) { }
        }
    }

    fun getCategoriesWithEpgData() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isLoadingEpgCategories = true) }
            }
            try {
                val categoriesWithCounts = repository.getCategoriesWithCounts()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            categoriesWithEpg = categoriesWithCounts,
                            isLoadingEpgCategories = false
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            categoriesWithEpg = emptyList(),
                            isLoadingEpgCategories = false
                        )
                    }
                }
            }
        }
    }

    fun getProgrammesForCategory(categoryId: String) {
        viewModelScope.launch {
            try {
                val startTime = Clock.System.now().toEpochMilliseconds()
                val endTime = startTime + 24 * 60 * 60 * 1000 // 24 hours
                val programmes = repository.getProgrammesForCategory(categoryId, startTime, endTime)
                _uiState.update { it.copy(programmesForCategory = programmes) }
            } catch (e: Exception) {
                _uiState.update { it.copy(programmesForCategory = emptyList()) }
            }
        }
    }

    fun getChannel(name: String) {
        viewModelScope.launch {
            try {
                val channel = repository.getChannel(name)
                _uiState.update { it.copy(fetchedChannel = channel) }
            } catch (e: Exception) {
                _uiState.update { it.copy(fetchedChannel = null) }
            }
        }
    }

    fun getChannelByEpgId(epgChannelId: String) {
        viewModelScope.launch {
            try {
                val channel = repository.getChannelByEpgId(epgChannelId)
                _uiState.update { it.copy(fetchedChannel = channel) }
            } catch (e: Exception) {
                _uiState.update { it.copy(fetchedChannel = null) }
            }
        }
    }

    suspend fun getChannelByEpgIdDirect(epgChannelId: String): Channel? {
        return try {
            repository.getChannelByEpgId(epgChannelId)
        } catch (e: Exception) {
            null
        }
    }

    fun changeLoadingState(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    fun calculateEpgTime(startTime: Long?, endTime: Long?, currentTime: Long): Int {
        if (startTime == null || endTime == null || startTime >= endTime || currentTime < startTime) return 0
        val totalDuration = (endTime - startTime).toDouble()
        val timePassed = (currentTime - startTime).toDouble()
        return ((timePassed / totalDuration) * 100).coerceIn(0.0, 100.0).toInt()
    }

    fun setSelectedChannel(channel: Channel, position: String) {
        _uiState.update { it.copy(selectedChannel = channel, selectedPosition = position) }
    }

    fun setSelectedPosition(position: String) {
        _uiState.update { it.copy(selectedPosition = position) }
    }

    fun setSelectedPosition(position: Int) {
        _uiState.update { it.copy(selectedPosition = position.toString()) }
    }

    fun clearSelectedChannel() {
        _uiState.update { it.copy(selectedChannel = null) }
    }

    fun isFavorite(channel: Channel): Boolean {
        return _uiState.value.favorites.any { fav ->
            if (fav.id != null && channel.id != null) fav.id == channel.id
            else fav.name == channel.name && fav.categoryId == channel.categoryId
        }
    }

    fun isRecentlyViewed(channel: Channel): Boolean {
        return _uiState.value.recentlyViewedChannels.any { recent ->
            if (recent.id != null && channel.id != null) recent.id == channel.id
            else recent.name == channel.name && recent.categoryId == channel.categoryId
        }
    }

    fun saveLastClickedItem(itemId: String?, position: Int) {
        _uiState.update { it.copy(lastClickedItemId = itemId, lastClickedItemPosition = position) }
    }

    fun clearLastClickedItem() {
        _uiState.update { it.copy(lastClickedItemId = null, lastClickedItemPosition = -1) }
    }

    suspend fun getCredentialsByUserId(userId: Int): UserCredentials? {
        return try {
            accountManagerRepository.getCredentialsByUserId(userId)
        } catch (e: Exception) {
            null
        }
    }

    fun resetState() {
        _uiState.value = StreamUiState()
    }
}
