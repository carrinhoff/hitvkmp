package pt.hitv.core.domain.repositories

import kotlinx.coroutines.flow.Flow
import pt.hitv.core.model.UserCredentials

interface AccountManagerRepository {
    suspend fun saveCredentials(userCredentials: UserCredentials): Int
    fun getAllCredentials(): Flow<List<UserCredentials>>
    suspend fun getCredentialsByUsername(username: String): UserCredentials?
    suspend fun getCredentialsByUserId(userId: Int): UserCredentials?
    suspend fun deleteUserAndRelatedData(userId: Int)
    suspend fun updateChannelPreviewEnabled(userId: Int, enabled: Boolean)
    suspend fun updateAccountCredentials(userId: Int, username: String, password: String, hostname: String, epgUrl: String?)
}
