package pt.hitv.core.common.featureflags

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * No-op implementation of FeatureFlagManager for testing or when feature flags are disabled.
 * Returns default values for all feature flags.
 */
class NoOpFeatureFlagManager : FeatureFlagManager {

    override fun getBoolean(flag: FeatureFlag): Boolean {
        return flag.defaultValue as? Boolean ?: false
    }

    override fun getString(flag: FeatureFlag): String {
        return flag.defaultValue as? String ?: ""
    }

    override fun getLong(flag: FeatureFlag): Long {
        return (flag.defaultValue as? Number)?.toLong() ?: 0L
    }

    override fun getDouble(flag: FeatureFlag): Double {
        return (flag.defaultValue as? Number)?.toDouble() ?: 0.0
    }

    override fun observeBoolean(flag: FeatureFlag): Flow<Boolean> {
        return flowOf(getBoolean(flag))
    }

    override fun observeString(flag: FeatureFlag): Flow<String> {
        return flowOf(getString(flag))
    }

    override suspend fun fetchAndActivate(): Boolean {
        return true
    }

    override fun setDefaults(defaults: Map<String, Any>) {
        // No-op
    }
}
