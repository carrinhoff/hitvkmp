package pt.hitv.feature.series.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import pt.hitv.core.common.Resources
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.common.analytics.ContentType
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.seriesInfo.SeriesInfo

class SeriesInfoViewModel(
    private val repository: TvShowRepository,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _seriesInfo = MutableStateFlow<SeriesInfo?>(null)
    val seriesInfo: StateFlow<SeriesInfo?> = _seriesInfo.asStateFlow()

    private var favoriteJob: Job? = null
    private var infoJob: Job? = null

    /**
     * Fetches series info: first from cache, then from network.
     * Updates _seriesInfo StateFlow so the UI reacts.
     */
    fun loadSeriesInfo(seriesId: String) {
        infoJob?.cancel()
        infoJob = viewModelScope.launch {
            // 1) Try cache first
            repository.fetchSeriesInfo(seriesId)
                .catch { /* ignore cache errors */ }
                .collect { cached ->
                    if (cached != null) {
                        _seriesInfo.value = cached
                    }
                }
        }

        // 2) Fetch from network (inserts into DB, then re-read)
        viewModelScope.launch {
            try {
                val startTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                val result = repository.getSeriesInfo(seriesId)
                val loadTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds() - startTime

                when (result) {
                    is Resources.Success -> {
                        analyticsHelper.logContentDetailLoaded(
                            contentType = ContentType.TV_SHOW,
                            contentId = seriesId,
                            loadTimeMs = loadTime,
                            dataSource = "network"
                        )
                        // Re-read from DB after network insert
                        repository.fetchSeriesInfo(seriesId)
                            .catch { /* ignore */ }
                            .collect { fresh ->
                                if (fresh != null) {
                                    _seriesInfo.value = fresh
                                }
                            }
                    }
                    is Resources.Error -> {
                        analyticsHelper.logContentDetailLoadFailed(
                            contentType = ContentType.TV_SHOW,
                            contentId = seriesId,
                            failureReason = result.message ?: "Unknown error"
                        )
                    }
                    else -> {}
                }
            } catch (_: Exception) {}
        }
    }

    fun checkFavoriteStatus(seriesId: Int) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            repository.getFavoritesTvShow().collect { favorites ->
                _isFavorite.value = favorites.any { it.seriesId == seriesId }
            }
        }
    }

    fun toggleFavorite(tvShow: TvShow) {
        viewModelScope.launch {
            val wasAdding = !_isFavorite.value
            repository.saveFavoriteTvShow(tvShow)
            analyticsHelper.logToggleFavorite(
                contentType = ContentType.TV_SHOW,
                contentId = tvShow.seriesId.toString(),
                contentName = tvShow.name,
                isAdding = wasAdding
            )
        }
    }

    fun saveRecentlyViewedSeries(tvShow: TvShow) {
        viewModelScope.launch {
            try {
                repository.saveRecentlyViewedTvShow(tvShow)
            } catch (_: Exception) {}
        }
    }
}
