package pt.hitv.feature.player.series

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.seriesInfo.Episode

data class SeriesPlayerUiState(
    val currentPlaybackPosition: Long = 0L,
    val currentEpisodeIndex: Int = 0
)

class SeriesPlayerViewModel(
    private val repository: TvShowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeriesPlayerUiState())
    val uiState: StateFlow<SeriesPlayerUiState> = _uiState.asStateFlow()

    fun updatePlaybackPosition(position: Long, episodeIndex: Int) {
        _uiState.update { it.copy(currentPlaybackPosition = position, currentEpisodeIndex = episodeIndex) }
    }

    fun savePlaybackPosition(currentPosition: Long, episodes: Array<Episode>, currentIndex: Int) {
        viewModelScope.launch {
            if (currentIndex >= 0 && currentIndex < episodes.size) {
                val episode = episodes[currentIndex]
                if (currentPosition != 0L) {
                    repository.updatePlaybackPosition(episode.id, currentPosition)
                }
            }
        }
    }

    fun saveEpisodeDuration(duration: Double, episode: Episode) {
        viewModelScope.launch { repository.updateEpisodeDuration(episode.id, duration) }
    }
}
