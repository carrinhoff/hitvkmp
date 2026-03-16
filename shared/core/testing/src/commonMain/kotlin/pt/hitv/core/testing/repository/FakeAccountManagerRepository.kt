package pt.hitv.core.testing.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.model.UserCredentials

/**
 * Fake implementation of [AccountManagerRepository] for testing.
 *
 * This implementation stores credentials in memory and provides control over
 * responses for testing different scenarios.
 *
 * Usage:
 * ```
 * val fakeRepo = FakeAccountManagerRepository()
 * val userId = fakeRepo.saveCredentials(credentials)
 *
 * // Retrieve credentials
 * val creds = fakeRepo.getCredentialsByUserId(userId)
 *
 * // For error scenarios:
 * fakeRepo.setShouldThrowOnSave(true)
 * ```
 */
class FakeAccountManagerRepository : AccountManagerRepository {

    private val _credentials = MutableStateFlow<List<UserCredentials>>(emptyList())
    private var nextUserId = 1
    private var shouldThrowOnSave = false
    private var saveErrorMessage = "Failed to save credentials"
    private var shouldThrowOnUpdate = false
    private var updateErrorMessage = "An account with this username and server URL already exists"

    // ==================== Test Control Methods ====================

    fun setCredentials(credentials: List<UserCredentials>) {
        _credentials.value = credentials
        nextUserId = (credentials.maxOfOrNull { it.userId } ?: 0) + 1
    }

    fun setShouldThrowOnSave(shouldThrow: Boolean, message: String = "Failed to save credentials") {
        shouldThrowOnSave = shouldThrow
        saveErrorMessage = message
    }

    fun setShouldThrowOnUpdate(
        shouldThrow: Boolean,
        message: String = "An account with this username and server URL already exists"
    ) {
        shouldThrowOnUpdate = shouldThrow
        updateErrorMessage = message
    }

    fun clear() {
        _credentials.value = emptyList()
        nextUserId = 1
        shouldThrowOnSave = false
        shouldThrowOnUpdate = false
    }

    fun getCredentialsList(): List<UserCredentials> = _credentials.value

    // ==================== AccountManagerRepository Implementation ====================

    override suspend fun saveCredentials(userCredentials: UserCredentials): Int {
        if (shouldThrowOnSave) {
            throw Exception(saveErrorMessage)
        }

        val existingIndex = _credentials.value.indexOfFirst {
            it.userId == userCredentials.userId && userCredentials.userId != 0
        }

        val userId: Int
        val updatedList = _credentials.value.toMutableList()

        if (existingIndex >= 0) {
            userId = userCredentials.userId
            updatedList[existingIndex] = userCredentials
        } else {
            userId = if (userCredentials.userId == 0) nextUserId++ else userCredentials.userId
            updatedList.add(userCredentials.copy(userId = userId))
        }

        _credentials.value = updatedList
        return userId
    }

    override fun getAllCredentials(): Flow<List<UserCredentials>> = _credentials

    override suspend fun getCredentialsByUsername(username: String): UserCredentials? {
        return _credentials.value.find { it.username == username }
    }

    override suspend fun getCredentialsByUserId(userId: Int): UserCredentials? {
        return _credentials.value.find { it.userId == userId }
    }

    override suspend fun deleteUserAndRelatedData(userId: Int) {
        _credentials.value = _credentials.value.filter { it.userId != userId }
    }

    override suspend fun updateChannelPreviewEnabled(userId: Int, enabled: Boolean) {
        val updatedList = _credentials.value.map { creds ->
            if (creds.userId == userId) {
                creds.copy(channelPreviewEnabled = enabled)
            } else {
                creds
            }
        }
        _credentials.value = updatedList
    }

    override suspend fun updateAccountCredentials(
        userId: Int,
        username: String,
        password: String,
        hostname: String,
        epgUrl: String?
    ) {
        if (shouldThrowOnUpdate) {
            throw IllegalStateException(updateErrorMessage)
        }

        val updatedList = _credentials.value.map { creds ->
            if (creds.userId == userId) {
                creds.copy(
                    username = username,
                    password = if (password.isNotEmpty()) password else creds.password,
                    hostname = hostname,
                    epgUrl = epgUrl
                )
            } else {
                creds
            }
        }
        _credentials.value = updatedList
    }
}
