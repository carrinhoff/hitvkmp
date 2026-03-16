package pt.hitv.core.data.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.datastore.HitvPreferencesDataStore

/**
 * Single Source of Truth for the currently active user session.
 * ViewModels should observe [userIdFlow] to react to account switches.
 *
 * Writes go to both PreferencesHelper (sync callers) and DataStore (reactive callers).
 * Reads come from DataStore flow (reactive) or PreferencesHelper (sync).
 */
class UserSessionManager(
    private val preferencesHelper: PreferencesHelper,
    private val dataStore: HitvPreferencesDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Seed DataStore from Settings on first run after update.
        val spUserId = preferencesHelper.getUserId()
        if (spUserId != -1) {
            dataStore.seedFromExistingPreferences(
                userId = spUserId,
                username = preferencesHelper.getUsername(),
                hostUrl = preferencesHelper.getHostUrl()
            )
        }
    }

    /**
     * A flow that emits the current User ID from DataStore.
     */
    val userIdFlow: Flow<Int> = dataStore.userIdFlow

    /**
     * Updates the current session to a new user.
     * Dual-writes to both Settings (for sync callers) and DataStore (for reactive callers).
     */
    fun switchToUser(userId: Int) {
        preferencesHelper.setStoredIntTag("userId", userId)
        dataStore.updateUserId(userId)
    }

    /**
     * Updates credentials in both stores.
     */
    fun updateCredentials(username: String, password: String, hostUrl: String) {
        preferencesHelper.setStoredTag("username", username)
        preferencesHelper.setStoredTag("password", password)
        preferencesHelper.setStoredTag("hostUrl", hostUrl)
        dataStore.updateUsername(username)
        dataStore.updateHostUrl(hostUrl)
    }

    /**
     * Clears session data from both stores.
     */
    fun clearSession() {
        preferencesHelper.deleteStoredFile()
        dataStore.clear()
    }
}
