package pt.hitv.feature.player.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.seriesInfo.Episode

data class SeriesPlayerUiState(
    val episodes: List<Episode> = emptyList(),
    val isLoading: Boolean = true,
    val currentPlaybackPosition: Long = 0L,
    val currentEpisodeIndex: Int = 0
)

class SeriesPlayerViewModel(
    private val repository: TvShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesPlayerUiState())
    val uiState: StateFlow<SeriesPlayerUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadEpisodes(seriesId: String, seasonNumber: Int) {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            repository.fetchSeasonsWithEpisodes(seriesId)
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { seasonsMap ->
                    val seasonEpisodes = seasonsMap.entries
                        .find { it.key.seasonNumber == seasonNumber }
                        ?.value ?: emptyList()
                    _uiState.update { it.copy(episodes = seasonEpisodes, isLoading = false) }
                }
        }
    }

    fun updatePlaybackPosition(position: Long, episodeIndex: Int) {
        _uiState.update { it.copy(currentPlaybackPosition = position, currentEpisodeIndex = episodeIndex) }
    }

    fun savePlaybackPosition(position: Long, episodeId: String) {
        if (position == 0L) return
        viewModelScope.launch(NonCancellable) {
            repository.updatePlaybackPosition(episodeId, position)
        }
    }

    fun saveEpisodeDuration(duration: Double, episodeId: String) {
        viewModelScope.launch {
            repository.updateEpisodeDuration(episodeId, duration)
        }
    }
}
