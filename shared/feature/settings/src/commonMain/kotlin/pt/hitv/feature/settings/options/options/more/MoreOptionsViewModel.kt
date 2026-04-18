package pt.hitv.feature.settings.options.options.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.hitv.core.common.AppInfoProvider
import pt.hitv.core.common.LocaleController
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.designsystem.theme.ThemeManager
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.sync.BackgroundSyncManager
import pt.hitv.core.sync.TASK_CONTENT
import pt.hitv.core.sync.TASK_EPG

/**
 * Keys used by the More Options screen to read / write user preferences.
 */
object MoreOptionsPrefKeys {
    const val PLAYER_ENGINE = "player_engine"
    const val LIVE_BUFFER = "live_buffer"
}

data class MoreOptionsUiState(
    val channelPreviewEnabled: Boolean = true,
    val currentAccountInfo: Triple<String, String, String?>? = null,
    val currentLanguage: String = "",
    val lastFocusedItemId: String? = null,
    val shouldRestoreFocus: Boolean = false
)

class MoreOptionsViewModel(
    private val preferencesHelper: PreferencesHelper,
    private val accountManagerRepository: AccountManagerRepository,
    private val themeManager: ThemeManager,
    private val localeController: LocaleController,
    private val appInfoProvider: AppInfoProvider,
    private val backgroundSyncManager: BackgroundSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreOptionsUiState())
    val uiState: StateFlow<MoreOptionsUiState> = _uiState.asStateFlow()

    /** Currently-applied language tag (BCP-47). Empty string means system default. */
    val languageTag: StateFlow<String> = localeController.currentLocale

    /** True when the most recent applyLanguage needs an app restart (iOS). */
    val pendingRestart: StateFlow<Boolean> = localeController.pendingRestart

    private val _playerEngine = MutableStateFlow(
        preferencesHelper.getStoredTag(MoreOptionsPrefKeys.PLAYER_ENGINE).ifEmpty { DEFAULT_PLAYER_ENGINE }
    )
    val playerEngine: StateFlow<String> = _playerEngine.asStateFlow()

    private val _liveBufferSize = MutableStateFlow(
        preferencesHelper.getStoredTag(MoreOptionsPrefKeys.LIVE_BUFFER).ifEmpty { DEFAULT_LIVE_BUFFER }
    )
    val liveBufferSize: StateFlow<String> = _liveBufferSize.asStateFlow()

    /** Human-readable version, e.g. "1.0.0 (15)". */
    val appVersion: String =
        "${appInfoProvider.versionName} (${appInfoProvider.versionCode})"

    init {
        loadChannelPreviewSetting()
        loadCurrentAccountInfo()
        loadCurrentLanguage()
    }

    fun saveLastFocusedItem(itemId: String) {
        _uiState.update { it.copy(lastFocusedItemId = itemId) }
    }

    fun prepareFocusRestoration() {
        _uiState.update { it.copy(shouldRestoreFocus = true) }
    }

    fun consumeFocusRestoration() {
        _uiState.update { it.copy(shouldRestoreFocus = false) }
    }

    private fun loadChannelPreviewSetting() {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                val userCredentials = accountManagerRepository.getCredentialsByUserId(currentUserId)
                _uiState.update { it.copy(channelPreviewEnabled = userCredentials?.channelPreviewEnabled ?: true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(channelPreviewEnabled = true) }
            }
        }
    }

    fun updateChannelPreviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                accountManagerRepository.updateChannelPreviewEnabled(currentUserId, enabled)
                _uiState.update { it.copy(channelPreviewEnabled = enabled) }
            } catch (_: Exception) {}
        }
    }

    private fun loadCurrentAccountInfo() {
        viewModelScope.launch {
            try {
                val currentUserId = preferencesHelper.getUserId()
                val userCredentials = accountManagerRepository.getCredentialsByUserId(currentUserId)
                if (userCredentials != null) {
                    _uiState.update {
                        it.copy(
                            currentAccountInfo = Triple(
                                userCredentials.username,
                                userCredentials.hostname,
                                userCredentials.expirationDate
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun refreshCurrentAccountInfo() {
        loadCurrentAccountInfo()
        loadChannelPreviewSetting()
    }

    private fun loadCurrentLanguage() {
        _uiState.update { it.copy(currentLanguage = preferencesHelper.getAppLanguage()) }
    }

    /**
     * Apply the given BCP-47 language [tag]. Empty string resets to system default.
     * Persists to PreferencesHelper for UI echo and delegates to LocaleController
     * for the actual platform hot-swap / restart-pending flag.
     */
    fun applyLanguage(tag: String) {
        val normalized = tag.ifBlank { "" }
        preferencesHelper.setAppLanguage(normalized.ifEmpty { "system" })
        _uiState.update { it.copy(currentLanguage = normalized.ifEmpty { "system" }) }
        localeController.applyLocale(normalized)
    }

    /** Kept for backward compat with existing call sites. */
    fun updateAppLanguage(languageCode: String) {
        applyLanguage(languageCode)
    }

    fun setPlayerEngine(engine: String) {
        preferencesHelper.setStoredTag(MoreOptionsPrefKeys.PLAYER_ENGINE, engine)
        _playerEngine.value = engine
    }

    fun setLiveBufferSize(size: String) {
        preferencesHelper.setStoredTag(MoreOptionsPrefKeys.LIVE_BUFFER, size)
        _liveBufferSize.value = size
    }

    /**
     * Kick off one-shot runs for both background sync tasks. Used by the
     * "Refresh Data" entry on the More screen.
     */
    fun triggerRefreshData() {
        viewModelScope.launch {
            backgroundSyncManager.runOnce(TASK_CONTENT)
            backgroundSyncManager.runOnce(TASK_EPG)
        }
    }

    companion object {
        const val DEFAULT_PLAYER_ENGINE = "exoplayer"
        const val DEFAULT_LIVE_BUFFER = "medium"
    }
}
