package pt.hitv.feature.movies.list

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
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.domain.usecases.GetMoviesPagerUseCase
import pt.hitv.core.domain.usecases.SearchMoviesUseCase
import pt.hitv.core.domain.usecases.ToggleFavoriteMovieUseCase
import pt.hitv.core.model.Category
import pt.hitv.core.model.Movie
import pt.hitv.core.data.paging.MOVIE_FILTER_ALL
import pt.hitv.core.data.paging.SORT_ADDED
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.manager.UserSessionManager

/**
 * UI State for Movie screen following NiA unidirectional data flow pattern.
 */
data class MovieUiState(
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
    val searchResultMovies: Map<String, List<Movie>> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val favorites: List<Movie> = emptyList(),
    val recentlyViewedMovies: List<Movie> = emptyList(),
    val lastAddedMovies: List<Movie> = emptyList(),
    val continueWatchingMovies: List<Movie> = emptyList(),
    val categoryMoviesMap: Map<String, List<Movie>> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val focusState: MovieFocusState = MovieFocusState(),
    val lastClickedItemId: String? = null,
    val lastClickedItemPosition: Int = -1,
    val isSidebarFocused: Boolean = false
)

data class MovieFocusState(
    val focusedMovie: Movie? = null,
    val pendingFocusIndex: Int = -1,
    val pendingFocusCategoryId: String? = null,
    val pendingFocusMovieId: String? = null,
    val activeFocusIndex: Int = -1,
    val activeFocusCategoryId: String? = null,
    val activeFocusMovieId: String? = null
)

/**
 * ViewModel for Movies screen.
 * Ported from Hilt to plain class for Koin injection.
 */
class MovieViewModel(
    private val userSessionManager: UserSessionManager,
    private val repository: MovieRepository,
    private val preferencesHelper: PreferencesHelper,
    private val getMoviesPagerUseCase: GetMoviesPagerUseCase,
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val toggleFavoriteMovieUseCase: ToggleFavoriteMovieUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieUiState())
    val uiState: StateFlow<MovieUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val userIdFlow = userSessionManager.userIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, preferencesHelper.getUserId())

    private val _refreshPagingEvent = MutableSharedFlow<Unit>()
    val refreshPagingEvent: SharedFlow<Unit> = _refreshPagingEvent.asSharedFlow()

    var listScrollIndex: Int = 0
    var listScrollOffset: Int = 0

    fun saveScrollPosition(index: Int, offset: Int) {
        listScrollIndex = index
        listScrollOffset = offset
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val moviesPagerFlow: Flow<PagingData<Movie>> = combine(
        userIdFlow,
        _uiState.map { it.currentCategoryFilter }.distinctUntilChanged(),
        _uiState.map { it.currentSearchQuery }.distinctUntilChanged(),
        _uiState.map { it.currentSortOrder }.distinctUntilChanged(),
        _uiState.map { it.isSortAscending }.distinctUntilChanged()
    ) { userId, category, query, sortOrder, isAscending ->
        PagingParams(userId, category, query, sortOrder, isAscending)
    }
        .flatMapLatest { params ->
            val actualCategory = if (params.category == "SEARCH") MOVIE_FILTER_ALL else params.category
            getMoviesPagerUseCase(actualCategory, params.query, params.sortOrder, params.isAscending)
        }.cachedIn(viewModelScope)

    private val categoryMoviesCache = mutableMapOf<String, List<Movie>>()

    private var categoryManuallySet = false

    init {
        viewModelScope.launch {
            userIdFlow.collect { userId ->
                if (userId != -1) {
                    clearCategoryCache()
                    categoryMoviesCache.clear()
                    _uiState.update {
                        it.copy(
                            categoryMoviesMap = emptyMap(),
                            searchMatchedCategories = emptyList(),
                            searchResultMovies = emptyMap()
                        )
                    }
                    loadDefaultCategory()
                    fetchLocalCategories()
                    getFavorites()
                    fetchRecentlyViewedMovies()
                }
            }
        }
    }

    private fun loadDefaultCategory() {
        viewModelScope.launch {
            try {
                val defaultCategoryId = repository.getDefaultMovieCategoryId()
                val newCategory = defaultCategoryId ?: MOVIE_FILTER_ALL
                if (!categoryManuallySet) {
                    _uiState.update { it.copy(currentCategoryFilter = newCategory) }
                }
            } catch (e: Exception) {
                if (!categoryManuallySet) {
                    _uiState.update { it.copy(currentCategoryFilter = MOVIE_FILTER_ALL) }
                }
            }
        }
    }

    fun setCategoryFilter(category: String?) {
        categoryManuallySet = true
        if (_uiState.value.currentCategoryFilter != category) {
            _uiState.update { it.copy(currentCategoryFilter = category, currentSearchQuery = null) }
            listScrollIndex = 0
            listScrollOffset = 0
        }
    }

    fun setSearchQuery(query: String?) {
        val trimmedQuery = query?.trim().takeIf { !it.isNullOrEmpty() }
        if (_uiState.value.currentSearchQuery != trimmedQuery) {
            _uiState.update { it.copy(currentSearchQuery = trimmedQuery) }
        }
    }

    fun setCategorySearchActive(active: Boolean) {
        if (!active) setSearchQuery(null)
    }

    fun updateSort(newSort: String) {
        _uiState.update { state ->
            if (state.currentSortOrder == newSort) {
                state.copy(isSortAscending = !state.isSortAscending)
            } else {
                state.copy(currentSortOrder = newSort, isSortAscending = false)
            }
        }
    }

    fun updateSearchQueryWithCategories(query: String, allCategories: List<Category>) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.update { it.copy(searchMatchedCategories = emptyList(), searchResultMovies = emptyMap()) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val categoriesByName = allCategories.filter { it.categoryName.contains(trimmedQuery, ignoreCase = true) }
                val processedCategoryIds = categoriesByName.map { it.categoryId.toString() }.toMutableSet()
                val matchingMovies = searchMoviesUseCase(trimmedQuery, 500)
                val moviesByCategory = matchingMovies.groupBy { it.categoryId ?: "" }
                val finalCategories = ArrayList(categoriesByName)
                moviesByCategory.keys.forEach { categoryId ->
                    if (!processedCategoryIds.contains(categoryId)) {
                        allCategories.find { it.categoryId.toString() == categoryId }?.let { category ->
                            finalCategories.add(category)
                            processedCategoryIds.add(categoryId)
                        }
                    }
                }
                _uiState.update { it.copy(searchMatchedCategories = finalCategories, searchResultMovies = moviesByCategory) }
            } catch (e: Exception) {
                _uiState.update { it.copy(searchMatchedCategories = emptyList(), searchResultMovies = emptyMap()) }
            }
        }
    }

    private var categoriesJob: Job? = null
    private var favoritesJob: Job? = null
    private var recentMoviesJob: Job? = null

    private fun fetchLocalCategories() {
        categoriesJob?.cancel()
        categoriesJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.getAllMovieCategories()
                .catch { _uiState.update { it.copy(categories = emptyList()) } }
                .collect { categories -> _uiState.update { it.copy(categories = categories, isLoading = false) } }
        }
    }

    fun getFavorites() {
        favoritesJob?.cancel()
        favoritesJob = viewModelScope.launch {
            repository.getFavoritesMovie().collect { favorites -> _uiState.update { it.copy(favorites = favorites) } }
        }
    }

    fun fetchRecentlyViewedMovies() {
        recentMoviesJob?.cancel()
        recentMoviesJob = viewModelScope.launch {
            repository.getRecentlyViewedMovies().collect { movies -> _uiState.update { it.copy(recentlyViewedMovies = movies) } }
        }
    }

    fun fetchLastAddedMovies() {
        viewModelScope.launch {
            try {
                val movies = repository.getLastAddedMovies(20)
                _uiState.update { it.copy(lastAddedMovies = movies) }
            } catch (e: Exception) { }
        }
    }

    fun fetchContinueWatchingMovies() {
        viewModelScope.launch {
            try {
                val movies = repository.getContinueWatchingMovies(20)
                _uiState.update { it.copy(continueWatchingMovies = movies) }
            } catch (e: Exception) { }
        }
    }

    suspend fun getTotalMovieCount(): Int {
        _uiState.value.categoryCounts[MOVIE_FILTER_ALL]?.let { return it }
        return withContext(Dispatchers.IO) {
            val count = repository.getTotalMovieCount()
            _uiState.update { it.copy(categoryCounts = it.categoryCounts + (MOVIE_FILTER_ALL to count)) }
            count
        }
    }

    suspend fun getCategoryMovieCount(categoryId: String): Int {
        _uiState.value.categoryCounts[categoryId]?.let { return it }
        return withContext(Dispatchers.IO) {
            val count = repository.getCategoryMovieCount(categoryId)
            _uiState.update { it.copy(categoryCounts = it.categoryCounts + (categoryId to count)) }
            count
        }
    }

    suspend fun getMoviesByCategory(categoryId: String, limit: Int = 20): List<Movie> {
        categoryMoviesCache[categoryId]?.let { return it }
        val movies = withContext(Dispatchers.IO) { repository.getMoviesByCategory(categoryId, limit) }
        categoryMoviesCache[categoryId] = movies
        _uiState.update { it.copy(categoryMoviesMap = it.categoryMoviesMap + (categoryId to movies)) }
        return movies
    }

    suspend fun searchMoviesInCategory(categoryId: String, searchQuery: String, limit: Int = 20): List<Movie> {
        return withContext(Dispatchers.IO) {
            val movies = searchMoviesUseCase(searchQuery, limit)
            if (categoryId == MOVIE_FILTER_ALL) movies else movies.filter { it.categoryId == categoryId }
        }
    }

    fun clearCategoryCache() {
        categoryMoviesCache.clear()
        _uiState.update { it.copy(categoryCounts = emptyMap()) }
    }

    fun saveFavoriteMovie(movie: Movie, isFavoritesFilterActive: Boolean) {
        val wasFavorite = movie.isFavorite
        viewModelScope.launch {
            try {
                toggleFavoriteMovieUseCase(movie)
                getFavorites()
                if (wasFavorite && isFavoritesFilterActive) { _refreshPagingEvent.emit(Unit) }
            } catch (e: Exception) { }
        }
    }

    fun saveRecentlyViewedMovie(movie: Movie, currentTimeMillis: Long) {
        viewModelScope.launch {
            try {
                repository.saveRecentlyViewedMovie(movie.copy(lastViewedTimestamp = currentTimeMillis))
                fetchRecentlyViewedMovies()
            } catch (e: Exception) { }
        }
    }

    fun saveLastClickedItem(itemId: String?, position: Int) { _uiState.update { it.copy(lastClickedItemId = itemId, lastClickedItemPosition = position) } }
    fun clearLastClickedItem() { _uiState.update { it.copy(lastClickedItemId = null, lastClickedItemPosition = -1) } }
    fun setTvSearchQuery(query: String) { _uiState.update { it.copy(tvSearchQuery = query, currentSearchQuery = query.ifEmpty { null }) } }
    fun setTvSearchActive(active: Boolean) { _uiState.update { it.copy(tvSearchActive = active, tvSearchQuery = if (active) it.tvSearchQuery else "") } }
    fun setInitialized() { if (!_uiState.value.isInitialized) _uiState.update { it.copy(isInitialized = true) } }
    fun updateSelectedCategoryName(categoryName: String?) { _uiState.update { it.copy(selectedCategoryName = categoryName) } }

    fun savePendingFocus(index: Int, categoryId: String?, movieId: String?) {
        _uiState.update { it.copy(focusState = it.focusState.copy(pendingFocusIndex = index, pendingFocusCategoryId = categoryId, pendingFocusMovieId = movieId)) }
    }

    fun activateFocus() {
        val fs = _uiState.value.focusState
        if (fs.pendingFocusIndex >= 0 || fs.pendingFocusMovieId != null) {
            _uiState.update { it.copy(focusState = it.focusState.copy(activeFocusIndex = fs.pendingFocusIndex, activeFocusCategoryId = fs.pendingFocusCategoryId, activeFocusMovieId = fs.pendingFocusMovieId)) }
        }
    }

    fun clearActiveFocus() { _uiState.update { it.copy(focusState = MovieFocusState()) } }
    fun setFocusedMovie(movie: Movie?) { _uiState.update { it.copy(focusState = it.focusState.copy(focusedMovie = movie)) } }
    fun setSidebarFocused(focused: Boolean) { _uiState.update { it.copy(isSidebarFocused = focused) } }

    fun resetState() {
        _uiState.value = MovieUiState()
        categoryManuallySet = false
    }
}

data class PagingParams(
    val userId: Int,
    val category: String?,
    val query: String?,
    val sortOrder: String,
    val isAscending: Boolean
)
