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

    /**
     * Non-null when the network fetch failed and there was nothing cached to fall back on.
     *
     * Without this the screen had no way to distinguish "still loading" from "failed and never
     * coming": `SeriesInfoContent` shows `SeriesLoadingScreen` whenever `seriesInfo == null`, and a
     * failed fetch left it null forever, so the user sat on a spinner indefinitely with no
     * explanation and no retry. `Resources.Error` was logged to analytics and otherwise dropped.
     */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    private var favoriteJob: Job? = null

    /**
     * Fetches series info: first from cache, then from network with DB insert.
     * Suspending so the caller can sequence follow-up queries (episodes) that depend
     * on the network-inserted rows being present in the DB.
     */
    suspend fun loadSeriesInfo(seriesId: String) {
        _loadError.value = null
        try {
            repository.fetchSeriesInfo(seriesId)
                .catch { /* ignore cache errors */ }
                .collect { cached ->
                    if (cached != null) {
                        _seriesInfo.value = cached
                    }
                }

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
                    // Only an error if the cache gave us nothing — a stale-but-usable series is
                    // better than an error screen, which is what the cache-then-network order is
                    // for.
                    if (_seriesInfo.value == null) {
                        _loadError.value = result.message?.takeIf { it.isNotBlank() }
                            ?: "Could not load this series."
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            // Previously swallowed silently, which is how a thrown failure also became an
            // permanent spinner.
            if (_seriesInfo.value == null) {
                _loadError.value = e.message?.takeIf { it.isNotBlank() }
                    ?: "Could not load this series."
            }
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
