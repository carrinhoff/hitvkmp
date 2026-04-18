package pt.hitv.feature.series.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.domain.usecases.GetSeriesPagerUseCase
import pt.hitv.core.domain.usecases.SearchSeriesUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteSeriesUseCase
import pt.hitv.core.model.Category
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.seriesInfo.Episode
import pt.hitv.core.model.seriesInfo.Season
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.SORT_ADDED
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.sync.SyncStateManager
import pt.hitv.core.data.manager.UserSessionManager

data class SeriesUiState(
    val isLoading: Boolean = false,
    val isInitialized: Boolean = false,
    val currentCategoryFilter: String? = MOVIE_FILTER_ALL,
    val currentSortOrder: String = SORT_ADDED,
    val isSortAscending: Boolean = false,
    val currentSearchQuery: String? = null,
    val selectedCategoryName: String? = null,
    val tvSearchQuery: String = "",
    val tvSearchActive: Boolean = false,
    val searchMatchedCategories: List<Category> = emptyList(),
    val searchResultSeries: Map<String, List<TvShow>> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val favorites: List<TvShow> = emptyList(),
    val recentlyViewedTvShows: List<TvShow> = emptyList(),
    val lastAddedTvShows: List<TvShow> = emptyList(),
    val continueWatchingSeries: List<TvShow> = emptyList(),
    val categorySeriesMap: Map<String, List<TvShow>> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val seasonEpisodeData: Pair<String?, Map<Season, List<Episode>>> = null to emptyMap(),
    val focusState: SeriesFocusState = SeriesFocusState(),
    val lastClickedItemId: String? = null,
    val lastClickedItemPosition: Int = -1,
    val isSidebarFocused: Boolean = false
)

data class SeriesFocusState(
    val focusedSeries: TvShow? = null,
    val pendingFocusIndex: Int = -1,
    val pendingFocusCategoryId: String? = null,
    val pendingFocusSeriesId: String? = null,
    val activeFocusIndex: Int = -1,
    val activeFocusCategoryId: String? = null,
    val activeFocusSeriesId: String? = null
)

class SeriesViewModel(
    private val userSessionManager: UserSessionManager,
    private val repository: TvShowRepository,
    private val preferencesHelper: PreferencesHelper,
    private val getSeriesPagerUseCase: GetSeriesPagerUseCase,
    private val searchSeriesUseCase: SearchSeriesUseCase,
    private val toggleFavoriteSeriesUseCase: ToggleFavoriteSeriesUseCase,
    private val syncStateManager: SyncStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesUiState())
    val uiState: StateFlow<SeriesUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userIdFlow = userSessionManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferencesHelper.getUserId())

    private val _refreshPagingEvent = MutableSharedFlow<Unit>()
    val refreshPagingEvent: SharedFlow<Unit> = _refreshPagingEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val tvShowsPagerFlow = combine(
        userIdFlow,
        _uiState.map { it.currentCategoryFilter }.distinctUntilChanged(),
        _uiState.map { it.currentSearchQuery }.distinctUntilChanged(),
        _uiState.map { it.currentSortOrder }.distinctUntilChanged(),
        _uiState.map { it.isSortAscending }.distinctUntilChanged()
    ) { userId, category, query, sortOrder, isAscending ->
        PagingParams(userId, category, query, sortOrder, isAscending)
    }.flatMapLatest { params ->
        val actualCategory = if (params.category == "SEARCH") MOVIE_FILTER_ALL else params.category
        getSeriesPagerUseCase(actualCategory, params.query, params.sortOrder, params.isAscending)
    }

    private val categorySeriesCache = mutableMapOf<String, List<TvShow>>()
    private var categoryManuallySet = false
    private var categoriesJob: Job? = null
    private var favoritesJob: Job? = null
    private var recentSeriesJob: Job? = null

    init {
        viewModelScope.launch {
            userIdFlow.collect { userId ->
                if (userId != -1) {
                    clearCategoryCache()
                    categorySeriesCache.clear()
                    _uiState.update {
                        it.copy(categorySeriesMap = emptyMap(), searchMatchedCategories = emptyList(), searchResultSeries = emptyMap())
                    }
                    loadDefaultCategory()
                    fetchTvShowCategories()
                    getFavorites()
                    fetchRecentlyViewedTvShows()
                }
            }
        }
        viewModelScope.launch {
            syncStateManager.syncVersion.drop(1).collect { refreshAfterSync() }
        }
    }

    fun refreshAfterSync() {
        val uid = userIdFlow.value
        if (uid == -1) return
        clearCategoryCache()
        categorySeriesCache.clear()
        _uiState.update {
            it.copy(categorySeriesMap = emptyMap(), searchMatchedCategories = emptyList(), searchResultSeries = emptyMap())
        }
        fetchTvShowCategories()
        getFavorites()
        fetchRecentlyViewedTvShows()
        viewModelScope.launch { _refreshPagingEvent.emit(Unit) }
    }

    private fun loadDefaultCategory() {
        viewModelScope.launch {
            try {
                val defaultCategoryId = repository.getDefaultSeriesCategoryId()
                val newCategory = defaultCategoryId ?: MOVIE_FILTER_ALL
                if (!categoryManuallySet) _uiState.update { it.copy(currentCategoryFilter = newCategory) }
            } catch (_: Exception) {
                if (!categoryManuallySet) _uiState.update { it.copy(currentCategoryFilter = MOVIE_FILTER_ALL) }
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        categoryManuallySet = true
        if (_uiState.value.currentCategoryFilter != category) {
            _uiState.update { it.copy(currentCategoryFilter = category, currentSearchQuery = null) }
        }
    }

    fun setSearchQuery(query: String?) {
        val trimmedQuery = query?.trim().takeIf { !it.isNullOrEmpty() }
        if (_uiState.value.currentSearchQuery != trimmedQuery) _uiState.update { it.copy(currentSearchQuery = trimmedQuery) }
    }

    fun updateSort(newSort: String) {
        _uiState.update { state ->
            if (state.currentSortOrder == newSort) state.copy(isSortAscending = !state.isSortAscending)
            else state.copy(currentSortOrder = newSort, isSortAscending = false)
        }
    }

    fun updateSearchQueryWithCategories(query: String, allCategories: List<Category>) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.update { it.copy(searchMatchedCategories = emptyList(), searchResultSeries = emptyMap()) }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesByName = allCategories.filter { it.categoryName.contains(trimmedQuery, ignoreCase = true) }
                val processedCategoryIds = categoriesByName.map { it.categoryId.toString() }.toMutableSet()
                val matchingSeries = searchSeriesUseCase(trimmedQuery, 500)
                val seriesByCategory = matchingSeries.groupBy { it.categoryId ?: "" }
                val finalCategories = ArrayList(categoriesByName)
                seriesByCategory.keys.forEach { categoryId ->
                    if (!processedCategoryIds.contains(categoryId)) {
                        allCategories.find { it.categoryId.toString() == categoryId }?.let { category ->
                            finalCategories.add(category); processedCategoryIds.add(categoryId)
                        }
                    }
                }
                _uiState.update { it.copy(searchMatchedCategories = finalCategories, searchResultSeries = seriesByCategory) }
            } catch (_: Exception) {
                _uiState.update { it.copy(searchMatchedCategories = emptyList(), searchResultSeries = emptyMap()) }
            }
        }
    }

    fun clearSeasonData() { _uiState.update { it.copy(seasonEpisodeData = null to emptyMap()) } }

    fun fetchSeasonsAndEpisodesForSeries(seriesId: String) {
        _uiState.update { it.copy(seasonEpisodeData = seriesId to emptyMap()) }
        viewModelScope.launch {
            repository.fetchSeasonsWithEpisodes(seriesId)
                .catch { if (_uiState.value.seasonEpisodeData.first == seriesId) _uiState.update { it.copy(seasonEpisodeData = null to emptyMap()) } }
                .collectLatest { seasonsMap ->
                    if (_uiState.value.seasonEpisodeData.first == seriesId) _uiState.update { it.copy(seasonEpisodeData = seriesId to seasonsMap) }
                }
        }
    }

    suspend fun getTotalSeriesCount(): Int {
        _uiState.value.categoryCounts[MOVIE_FILTER_ALL]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val count = repository.getTotalTvShowCount()
                _uiState.update { it.copy(categoryCounts = it.categoryCounts + (MOVIE_FILTER_ALL to count)) }; count
            } catch (_: Exception) { 0 }
        }
    }

    suspend fun getCategorySeriesCount(categoryId: String): Int {
        _uiState.value.categoryCounts[categoryId]?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val count = repository.getCategoryTvShowCount(categoryId)
                _uiState.update { it.copy(categoryCounts = it.categoryCounts + (categoryId to count)) }; count
            } catch (_: Exception) { 0 }
        }
    }

    private fun fetchTvShowCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllTvShowCategories(preferencesHelper.getUserId())
                    .catch { _uiState.update { it.copy(categories = emptyList()) } }
                    .collect { categories -> _uiState.update { it.copy(categories = categories, isLoading = false) } }
            } catch (_: Exception) { _uiState.update { it.copy(isLoading = false) } }
        }
    }

    fun getFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            repository.getFavoritesTvShow().catch { }.collect { favorites -> _uiState.update { it.copy(favorites = favorites) } }
        }
    }

    fun saveFavoriteTvShow(tvShow: TvShow, isFavoritesFilterActive: Boolean) {
        val wasFavorite = tvShow.isFavorite
        viewModelScope.launch {
            try { toggleFavoriteSeriesUseCase(tvShow); getFavorites(); if (wasFavorite && isFavoritesFilterActive) _refreshPagingEvent.emit(Unit) } catch (_: Exception) {}
        }
    }

    fun saveRecentlyViewedTvShow(tvShow: TvShow, currentTimeMillis: Long) {
        viewModelScope.launch {
            try { repository.saveRecentlyViewedTvShow(tvShow.copy(lastViewedTimestamp = currentTimeMillis)); fetchRecentlyViewedTvShows() } catch (_: Exception) {}
        }
    }

    fun fetchRecentlyViewedTvShows() {
        recentSeriesJob?.cancel()
        recentSeriesJob = viewModelScope.launch {
            repository.getRecentlyViewedTvShowsFlow().catch { }.collect { tvShows -> _uiState.update { it.copy(recentlyViewedTvShows = tvShows) } }
        }
    }

    fun fetchLastAddedTvShows() {
        viewModelScope.launch { try { _uiState.update { it.copy(lastAddedTvShows = repository.getLastAddedTvShows(20)) } } catch (_: Exception) {} }
    }

    fun fetchContinueWatchingSeries() {
        viewModelScope.launch { try { _uiState.update { it.copy(continueWatchingSeries = repository.getContinueWatchingSeries(20)) } } catch (_: Exception) {} }
    }

    suspend fun getSeriesByCategory(categoryId: String, limit: Int = 20): List<TvShow> {
        categorySeriesCache[categoryId]?.let { return it }
        val series = withContext(Dispatchers.IO) { try { repository.getSeriesByCategory(categoryId, limit) } catch (_: Exception) { emptyList() } }
        categorySeriesCache[categoryId] = series
        _uiState.update { it.copy(categorySeriesMap = it.categorySeriesMap + (categoryId to series)) }
        return series
    }

    fun clearCategoryCache() { categorySeriesCache.clear(); _uiState.update { it.copy(categoryCounts = emptyMap()) } }

    suspend fun searchSeriesInCategory(categoryId: String, searchQuery: String, limit: Int = 20): List<TvShow> {
        return withContext(Dispatchers.IO) {
            val series = repository.searchTvShowsWithFallback(searchQuery, limit)
            if (categoryId == MOVIE_FILTER_ALL) series else series.filter { it.categoryId == categoryId }
        }
    }

    fun saveLastClickedItem(itemId: String?, position: Int) { _uiState.update { it.copy(lastClickedItemId = itemId, lastClickedItemPosition = position) } }
    fun clearLastClickedItem() { _uiState.update { it.copy(lastClickedItemId = null, lastClickedItemPosition = -1) } }
    fun setTvSearchQuery(query: String) { _uiState.update { it.copy(tvSearchQuery = query, currentSearchQuery = query.ifEmpty { null }) } }
    fun setTvSearchActive(active: Boolean) { _uiState.update { it.copy(tvSearchActive = active, tvSearchQuery = if (active) it.tvSearchQuery else "") } }
    fun setInitialized() { if (!_uiState.value.isInitialized) _uiState.update { it.copy(isInitialized = true) } }
    fun updateSelectedCategoryName(categoryName: String?) { _uiState.update { it.copy(selectedCategoryName = categoryName) } }
    fun savePendingFocus(index: Int, categoryId: String?, seriesId: String?) { _uiState.update { it.copy(focusState = it.focusState.copy(pendingFocusIndex = index, pendingFocusCategoryId = categoryId, pendingFocusSeriesId = seriesId)) } }

    fun activateFocus() {
        val fs = _uiState.value.focusState
        if (fs.pendingFocusIndex >= 0 || fs.pendingFocusSeriesId != null) {
            _uiState.update { it.copy(focusState = it.focusState.copy(activeFocusIndex = fs.pendingFocusIndex, activeFocusCategoryId = fs.pendingFocusCategoryId, activeFocusSeriesId = fs.pendingFocusSeriesId)) }
        }
    }

    fun clearActiveFocus() { _uiState.update { it.copy(focusState = SeriesFocusState()) } }
    fun setFocusedSeries(series: TvShow?) { _uiState.update { it.copy(focusState = it.focusState.copy(focusedSeries = series)) } }
    fun setSidebarFocused(focused: Boolean) { _uiState.update { it.copy(isSidebarFocused = focused) } }
    fun resetState() { _uiState.value = SeriesUiState(); categoryManuallySet = false }
}

data class PagingParams(val userId: Int, val category: String?, val query: String?, val sortOrder: String, val isAscending: Boolean)
