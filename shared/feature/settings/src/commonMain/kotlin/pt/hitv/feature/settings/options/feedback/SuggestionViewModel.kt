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
    // Mirrors the original's SuggestionUiState (hitv/feature/settings/.../SuggestionViewModel.kt:18-19).
    // `isSubmitting` drives the submit button's disabled/"Submitting..." state and cannot be
    // derived from `submissionStatus`, which is null both before a submit and after the result
    // has been consumed by onSubmissionHandled().
    val isSubmitting: Boolean = false,
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
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                val success = submitFeedback(category.displayText, text, timestamp)
                _uiState.update { it.copy(isSubmitting = false, submissionStatus = success) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isSubmitting = false, submissionStatus = false) }
            }
        }
    }

    fun onSubmissionHandled() {
        _uiState.update { it.copy(submissionStatus = null) }
    }
}
