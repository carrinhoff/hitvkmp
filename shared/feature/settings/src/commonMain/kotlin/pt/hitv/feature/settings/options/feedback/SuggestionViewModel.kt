package pt.hitv.feature.settings.options.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import pt.hitv.core.model.enums.SuggestionCategory

data class SuggestionUiState(
    val submissionStatus: Boolean? = null
)

/**
 * ViewModel for suggestion/feedback screen.
 * Firebase Firestore submission is handled via a platform-abstracted feedback repository.
 */
class SuggestionViewModel(
    private val submitFeedback: suspend (category: String, text: String, timestamp: Long) -> Boolean = { _, _, _ -> false }
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuggestionUiState())
    val uiState: StateFlow<SuggestionUiState> = _uiState.asStateFlow()

    fun submitSuggestion(category: SuggestionCategory, text: String) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        viewModelScope.launch {
            try {
                val success = submitFeedback(category.displayText, text, timestamp)
                _uiState.update { it.copy(submissionStatus = success) }
            } catch (_: Exception) {
                _uiState.update { it.copy(submissionStatus = false) }
            }
        }
    }

    fun onSubmissionHandled() {
        _uiState.update { it.copy(submissionStatus = null) }
    }
}
