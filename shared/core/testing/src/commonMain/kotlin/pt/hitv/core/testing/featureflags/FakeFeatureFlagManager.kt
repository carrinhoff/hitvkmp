package pt.hitv.core.testing.featureflags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import pt.hitv.core.common.featureflags.FeatureFlag
import pt.hitv.core.common.featureflags.FeatureFlagManager

/**
 * Fake implementation of [FeatureFlagManager] for testing.
 *
 * This implementation allows tests to control feature flag values.
 *
 * Usage:
 * ```
 * val fakeFeatureFlags = FakeFeatureFlagManager()
 *
 * // Set specific flags for testing
 * fakeFeatureFlags.setFlag(FeatureFlag.PREMIUM_ENABLED, false)
 * fakeFeatureFlags.setFlag(FeatureFlag.NEW_PLAYER_UI_ENABLED, true)
 *
 * // Inject into your ViewModel/class
 * viewModel = MyViewModel(featureFlags = fakeFeatureFlags)
 *
 * // Test behavior with flags
 * assertThat(viewModel.shouldShowPremium()).isFalse()
 * ```
 */
class FakeFeatureFlagManager : FeatureFlagManager {

    private val flagValues = mutableMapOf<String, Any>()
    private val _flagUpdates = MutableStateFlow(0L)

    init {
        FeatureFlag.entries.forEach { flag ->
            flagValues[flag.key] = flag.defaultValue
        }
    }

    /**
     * Set a feature flag value for testing.
     */
    fun setFlag(flag: FeatureFlag, value: Any) {
        flagValues[flag.key] = value
        _flagUpdates.value++
    }

    /**
     * Set multiple flags at once.
     */
    fun setFlags(flags: Map<FeatureFlag, Any>) {
        flags.forEach { (flag, value) ->
            flagValues[flag.key] = value
        }
        _flagUpdates.value++
    }

    /**
     * Reset all flags to their default values.
     */
    fun resetToDefaults() {
        FeatureFlag.entries.forEach { flag ->
            flagValues[flag.key] = flag.defaultValue
        }
        _flagUpdates.value++
    }

    override fun getBoolean(flag: FeatureFlag): Boolean {
        return flagValues[flag.key] as? Boolean ?: flag.defaultValue as? Boolean ?: false
    }

    override fun getString(flag: FeatureFlag): String {
        return flagValues[flag.key] as? String ?: flag.defaultValue as? String ?: ""
    }

    override fun getLong(flag: FeatureFlag): Long {
        return (flagValues[flag.key] as? Number)?.toLong()
            ?: (flag.defaultValue as? Number)?.toLong() ?: 0L
    }

    override fun getDouble(flag: FeatureFlag): Double {
        return (flagValues[flag.key] as? Number)?.toDouble()
            ?: (flag.defaultValue as? Number)?.toDouble() ?: 0.0
    }

    override fun observeBoolean(flag: FeatureFlag): Flow<Boolean> {
        return _flagUpdates.map { getBoolean(flag) }
    }

    override fun observeString(flag: FeatureFlag): Flow<String> {
        return _flagUpdates.map { getString(flag) }
    }

    override suspend fun fetchAndActivate(): Boolean {
        return true
    }

    override fun setDefaults(defaults: Map<String, Any>) {
        defaults.forEach { (key, value) ->
            if (!flagValues.containsKey(key)) {
                flagValues[key] = value
            }
        }
    }
}
