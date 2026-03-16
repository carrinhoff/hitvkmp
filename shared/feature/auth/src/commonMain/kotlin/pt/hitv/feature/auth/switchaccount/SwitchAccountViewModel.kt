package pt.hitv.feature.auth.switchaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.model.UserCredentials
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.manager.UserSessionManager

/**
 * Events emitted when a user is deleted, to be handled by the UI layer.
 */
sealed interface DeleteUserEvent {
    data class SwitchedToUser(val user: UserCredentials) : DeleteUserEvent
    data object LastAccountDeleted : DeleteUserEvent
}

/**
 * Events emitted after an edit account attempt.
 */
sealed interface EditAccountEvent {
    data object Success : EditAccountEvent
    data class Error(val message: String) : EditAccountEvent
}

/**
 * ViewModel for Switch Account screen supporting both mobile and TV platforms.
 * Ported from Hilt to plain class for Koin injection.
 */
class SwitchAccountViewModel(
    private val repository: AccountManagerRepository,
    private val userSessionManager: UserSessionManager,
    private val preferencesHelper: PreferencesHelper
) : ViewModel() {

    val users: StateFlow<List<UserCredentials>> = repository.getAllCredentials()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentUserId: StateFlow<Int> = userSessionManager.userIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = preferencesHelper.getUserId()
        )

    private val _isDeletingUser = MutableStateFlow(false)
    val isDeletingUser: StateFlow<Boolean> = _isDeletingUser.asStateFlow()

    private val _deleteUserEvent = MutableSharedFlow<DeleteUserEvent>()
    val deleteUserEvent: SharedFlow<DeleteUserEvent> = _deleteUserEvent.asSharedFlow()

    private val _editAccountEvent = MutableSharedFlow<EditAccountEvent>()
    val editAccountEvent: SharedFlow<EditAccountEvent> = _editAccountEvent.asSharedFlow()

    private val _isEditingAccount = MutableStateFlow(false)
    val isEditingAccount: StateFlow<Boolean> = _isEditingAccount.asStateFlow()

    fun deleteUser(user: UserCredentials) {
        viewModelScope.launch {
            try {
                _isDeletingUser.value = true
                val isDeletingActiveUser = user.userId == currentUserId.value

                repository.deleteUserAndRelatedData(user.userId)

                if (isDeletingActiveUser) {
                    val remainingUsers = repository.getAllCredentials().first()
                    if (remainingUsers.isNotEmpty()) {
                        val nextUser = remainingUsers.first()
                        switchUser(nextUser)
                        _deleteUserEvent.emit(DeleteUserEvent.SwitchedToUser(nextUser))
                    } else {
                        userSessionManager.clearSession()
                        _deleteUserEvent.emit(DeleteUserEvent.LastAccountDeleted)
                    }
                }
            } catch (e: Exception) {
                // Error handled silently
            } finally {
                _isDeletingUser.value = false
            }
        }
    }

    fun editAccount(
        userId: Int,
        username: String,
        password: String,
        hostname: String,
        epgUrl: String?
    ) {
        viewModelScope.launch {
            try {
                _isEditingAccount.value = true
                repository.updateAccountCredentials(userId, username, password, hostname, epgUrl)

                if (userId == currentUserId.value) {
                    val fullCreds = repository.getCredentialsByUserId(userId)
                    preferencesHelper.setStoredTag("expirationDate", fullCreds?.expirationDate ?: "")
                    preferencesHelper.setStoredTag("output", fullCreds?.allowedOutputFormats?.firstOrNull() ?: "")
                    userSessionManager.updateCredentials(
                        username = username,
                        password = fullCreds?.password ?: "",
                        hostUrl = hostname
                    )
                }

                _editAccountEvent.emit(EditAccountEvent.Success)
            } catch (e: Exception) {
                _editAccountEvent.emit(EditAccountEvent.Error(e.message ?: "Failed to update account"))
            } finally {
                _isEditingAccount.value = false
            }
        }
    }

    fun switchUser(user: UserCredentials) {
        viewModelScope.launch {
            val fullCreds = repository.getCredentialsByUserId(user.userId)

            preferencesHelper.setStoredTag("expirationDate", user.expirationDate ?: "")
            preferencesHelper.setStoredTag("output", user.allowedOutputFormats?.firstOrNull() ?: "")

            userSessionManager.updateCredentials(
                username = user.username,
                password = fullCreds?.password ?: "",
                hostUrl = user.hostname
            )
            val newUserId = user.userId ?: -1
            userSessionManager.switchToUser(newUserId)
        }
    }
}
