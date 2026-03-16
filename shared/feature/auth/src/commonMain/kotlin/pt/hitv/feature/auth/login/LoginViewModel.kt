package pt.hitv.feature.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.domain.repositories.TvShowRepository
import pt.hitv.core.model.LiveStream
import pt.hitv.core.model.LoginResponse
import pt.hitv.core.model.Movie
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.UserCredentials
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.data.manager.UserSessionManager

/**
 * UI State for Login screen following NiA unidirectional data flow pattern.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val isSigningIn: Boolean = false,
    val isSavingCredentials: Boolean = false,
    val isProcessingM3u: Boolean = false,
    val signInResult: SignInResult? = null,
    val savedUserId: Int? = null,
    val m3uResult: M3uResult? = null,
    val saveCredentialsError: String? = null,
    val channelsState: DataState<List<LiveStream>> = DataState.Idle(),
    val moviesState: DataState<List<Movie>> = DataState.Idle(),
    val tvShowsState: DataState<List<TvShow>> = DataState.Idle()
)

sealed interface SignInResult {
    data class SignInSuccess(val response: LoginResponse) : SignInResult
    data class SignInError(val message: String) : SignInResult
}

sealed interface M3uResult {
    data object M3uSuccess : M3uResult
    data class M3uError(val message: String) : M3uResult
}

sealed interface DataState<out T> {
    class Idle<T> : DataState<T>
    class InProgress<T> : DataState<T>
    data class Loaded<T>(val data: T) : DataState<T>
    data class Failed<T>(val message: String) : DataState<T>
}

/**
 * ViewModel for Login screen.
 * Ported from Hilt to plain class with constructor params for Koin injection.
 */
class LoginViewModel(
    private val repository: StreamRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val userRepository: AccountManagerRepository,
    private val preferencesHelper: PreferencesHelper,
    private val userSessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn(username: String, password: String, isFromM3u: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isSigningIn = true, signInResult = null) }

            val result = repository.signIn(username, password)

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isSigningIn = false,
                    signInResult = when (result) {
                        is Resources.Success -> SignInResult.SignInSuccess(result.data!!)
                        is Resources.Error -> SignInResult.SignInError(result.message ?: "Unknown error")
                        is Resources.Loading -> null
                    }
                )
            }
        }
    }

    fun changeLoadingState(value: Boolean) {
        _uiState.update { it.copy(isLoading = value) }
    }

    fun saveCredentials(userCredentials: UserCredentials) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingCredentials = true, savedUserId = null, saveCredentialsError = null) }

            try {
                val userId = userRepository.saveCredentials(userCredentials)
                userSessionManager.switchToUser(userId)
                _uiState.update { it.copy(isSavingCredentials = false, savedUserId = userId) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSavingCredentials = false,
                        isLoading = false,
                        saveCredentialsError = e.message ?: "Failed to save credentials"
                    )
                }
            }
        }
    }

    fun processDirectM3uUrl(playlistName: String, m3uUrl: String, epgUrl: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingM3u = true, isLoading = true, m3uResult = null) }

            try {
                // Validate URL format
                val isValid = withContext(Dispatchers.IO) {
                    try {
                        // Basic URL validation without java.net.URL
                        val hasProtocol = m3uUrl.startsWith("http://") || m3uUrl.startsWith("https://")
                        val hasHost = m3uUrl.contains(".")
                        hasProtocol && hasHost
                    } catch (e: Exception) {
                        false
                    }
                }

                if (!isValid) {
                    _uiState.update {
                        it.copy(isProcessingM3u = false, isLoading = false, m3uResult = M3uResult.M3uError("Invalid URL format"))
                    }
                    return@launch
                }

                val userCredentials = UserCredentials(
                    userId = 0,
                    username = playlistName,
                    password = "",
                    hostname = m3uUrl,
                    expirationDate = null,
                    epgUrl = epgUrl?.ifBlank { null }
                )
                val userId = userRepository.saveCredentials(userCredentials)

                val fetchResult = repository.fetchAndParseM3uUrl(userId, playlistName, m3uUrl)

                if (fetchResult is Resources.Error) {
                    userRepository.deleteUserAndRelatedData(userId)
                    _uiState.update {
                        it.copy(
                            isProcessingM3u = false,
                            isLoading = false,
                            m3uResult = M3uResult.M3uError(fetchResult.message ?: "Failed to download M3U playlist")
                        )
                    }
                    return@launch
                }

                preferencesHelper.setStoredTag("username", playlistName)
                preferencesHelper.setStoredTag("password", "")
                preferencesHelper.setStoredTag("hostUrl", m3uUrl)
                userSessionManager.switchToUser(userId)

                _uiState.update {
                    it.copy(isProcessingM3u = false, isLoading = false, m3uResult = M3uResult.M3uSuccess, savedUserId = userId)
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error"
                _uiState.update {
                    it.copy(isProcessingM3u = false, isLoading = false, m3uResult = M3uResult.M3uError(errorMessage))
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }

    private fun fetchChannelsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(channelsState = DataState.InProgress()) }
            val result = repository.fetchChannelsData()
            _uiState.update { state ->
                state.copy(
                    channelsState = when (result) {
                        is Resources.Success -> DataState.Loaded(result.data!!)
                        is Resources.Error -> DataState.Failed(result.message ?: "Unknown error")
                        is Resources.Loading -> DataState.InProgress()
                    }
                )
            }
        }
    }

    private fun fetchMoviesData() {
        viewModelScope.launch {
            _uiState.update { it.copy(moviesState = DataState.InProgress()) }
            val result = movieRepository.fetchMoviesData()
            _uiState.update { state ->
                state.copy(
                    moviesState = when (result) {
                        is Resources.Success -> DataState.Loaded(result.data!!)
                        is Resources.Error -> DataState.Failed(result.message ?: "Unknown error")
                        is Resources.Loading -> DataState.InProgress()
                    }
                )
            }
        }
    }

    private fun fetchTvShowsData() {
        viewModelScope.launch {
            _uiState.update { it.copy(tvShowsState = DataState.InProgress()) }
            val result = tvShowRepository.fetchTvShowsData()
            _uiState.update { state ->
                state.copy(
                    tvShowsState = when (result) {
                        is Resources.Success -> DataState.Loaded(result.data!!)
                        is Resources.Error -> DataState.Failed(result.message ?: "Unknown error")
                        is Resources.Loading -> DataState.InProgress()
                    }
                )
            }
        }
    }
}
