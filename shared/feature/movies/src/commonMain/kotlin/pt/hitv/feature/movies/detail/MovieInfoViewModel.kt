package pt.hitv.feature.movies.detail

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.common.Resources
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ContentType
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.model.Movie
import pt.hitv.core.model.cast.CastResponse
import pt.hitv.core.model.movieInfo.CachedMovieInfo
import pt.hitv.core.model.movieInfo.MovieInfoResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class MovieInfoUiState(
    val cachedMovieInfo: Resources<CachedMovieInfo?> = Resources.Loading(),
    val networkMovieInfo: Resources<MovieInfoResponse>? = null,
    val movieCast: Resources<CastResponse>? = null,
    val isFavorite: Boolean = false
)

class MovieInfoViewModel(
    private val repository: MovieRepository,
    private val analyticsHelper: AnalyticsHelper,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieInfoUiState())
    val uiState: StateFlow<MovieInfoUiState> = _uiState.asStateFlow()

    private var favoriteJob: Job? = null

    fun getLocalMovieInfo(streamId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cachedMovieInfo = Resources.Loading()) }
            _uiState.update { it.copy(cachedMovieInfo = repository.getMovieInfoCached(streamId)) }
        }
    }

    fun getMovieInfo(streamId: String) {
        val username = preferencesHelper.getUsername()
        val password = preferencesHelper.getPassword()

        if (username.isBlank() || password.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(networkMovieInfo = Resources.Loading()) }

            val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val result = repository.getMovieInfo(username, password, streamId)
            val loadTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime

            when (result) {
                is Resources.Success -> {
                    analyticsHelper.logContentDetailLoaded(
                        contentType = ContentType.MOVIE,
                        contentId = streamId,
                        loadTimeMs = loadTime,
                        dataSource = "network"
                    )
                }
                is Resources.Error -> {
                    analyticsHelper.logContentDetailLoadFailed(
                        contentType = ContentType.MOVIE,
                        contentId = streamId,
                        failureReason = result.message ?: "Unknown error"
                    )
                }
                else -> {}
            }

            _uiState.update { it.copy(networkMovieInfo = result) }
        }
    }

    fun insertMovieInfo(movieInfo: MovieInfoResponse) {
        viewModelScope.launch {
            try {
                repository.insertMovieInfo(movieInfo)
            } catch (_: Exception) {}
        }
    }

    fun getCast(tmdbId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(movieCast = Resources.Loading()) }
            _uiState.update { it.copy(movieCast = repository.getCast(tmdbId)) }
        }
    }

    suspend fun getPlaybackPosition(streamId: Int): Long? {
        return repository.getMoviePlaybackPosition(streamId)
    }

    fun checkFavoriteStatus(streamId: String) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            repository.getFavoritesMovie().collect { favorites ->
                _uiState.update { it.copy(isFavorite = favorites.any { fav -> fav.streamId == streamId }) }
            }
        }
    }

    fun toggleFavorite(movie: Movie) {
        viewModelScope.launch {
            val wasAdding = !_uiState.value.isFavorite
            repository.saveFavoriteMovie(movie)
            analyticsHelper.logToggleFavorite(
                contentType = ContentType.MOVIE,
                contentId = movie.streamId,
                contentName = movie.name,
                isAdding = wasAdding
            )
        }
    }

    fun saveRecentlyViewed(movie: Movie) {
        viewModelScope.launch {
            try {
                repository.saveRecentlyViewedMovie(movie)
            } catch (_: Exception) {}
        }
    }
}
