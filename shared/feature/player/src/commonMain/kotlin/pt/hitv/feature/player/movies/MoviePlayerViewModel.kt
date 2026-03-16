package pt.hitv.feature.player.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.MovieRepository

class MoviePlayerViewModel(
    private val movieRepository: MovieRepository
) : ViewModel() {

    fun savePlaybackPosition(streamId: Int, position: Long) {
        viewModelScope.launch {
            if (position > 0) movieRepository.updateMoviePlaybackPosition(streamId, position)
        }
    }

    suspend fun getPlaybackPosition(streamId: Int): Long? {
        return movieRepository.getMoviePlaybackPosition(streamId)
    }
}
