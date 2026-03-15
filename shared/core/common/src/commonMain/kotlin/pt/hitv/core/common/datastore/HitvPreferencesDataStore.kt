package pt.hitv.core.common.datastore

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * KMP equivalent of the Android DataStore-based preferences.
 * Uses multiplatform-settings with FlowSettings for reactive observation.
 */
@OptIn(ExperimentalSettingsApi::class)
class HitvPreferencesDataStore(
    observableSettings: ObservableSettings
) {
    private val flowSettings: FlowSettings = observableSettings.toFlowSettings()
    private val settings: ObservableSettings = observableSettings

    private companion object {
        const val KEY_USER_ID = "userId"
        const val KEY_USERNAME = "username"
        const val KEY_HOST_URL = "hostUrl"
        const val KEY_PARENTAL_CONTROL_ENABLED = "parental_control_enabled"
    }

    val userIdFlow: Flow<Int> = flowSettings.getIntFlow(KEY_USER_ID, -1)
        .distinctUntilChanged()

    val usernameFlow: Flow<String> = flowSettings.getStringFlow(KEY_USERNAME, "")
        .distinctUntilChanged()

    val hostUrlFlow: Flow<String> = flowSettings.getStringFlow(KEY_HOST_URL, "")
        .distinctUntilChanged()

    val parentalControlEnabledFlow: Flow<Boolean> = flowSettings.getBooleanFlow(KEY_PARENTAL_CONTROL_ENABLED, false)
        .distinctUntilChanged()

    fun updateUserId(userId: Int) {
        settings.putInt(KEY_USER_ID, userId)
    }

    fun updateUsername(username: String) {
        settings.putString(KEY_USERNAME, username)
    }

    fun updateHostUrl(hostUrl: String) {
        settings.putString(KEY_HOST_URL, hostUrl)
    }

    fun updateParentalControlEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_PARENTAL_CONTROL_ENABLED, enabled)
    }

    /**
     * One-time migration: seed from existing preferences for existing users
     * updating the app. Only writes if no userId is set yet.
     */
    fun seedFromExistingPreferences(userId: Int, username: String, hostUrl: String) {
        val currentUserId = settings.getIntOrNull(KEY_USER_ID)
        if (currentUserId != null) return // Already seeded

        settings.putInt(KEY_USER_ID, userId)
        settings.putString(KEY_USERNAME, username)
        settings.putString(KEY_HOST_URL, hostUrl)
    }

    fun clear() {
        settings.clear()
    }
}
