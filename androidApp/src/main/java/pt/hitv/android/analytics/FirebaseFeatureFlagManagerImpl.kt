package pt.hitv.android.analytics

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import pt.hitv.core.common.featureflags.FeatureFlag
import pt.hitv.core.common.featureflags.FeatureFlagManager

/**
 * Firebase Remote Config implementation of FeatureFlagManager
 * using GitLive Firebase KMP SDK.
 */
class FirebaseFeatureFlagManagerImpl : FeatureFlagManager {

    private val remoteConfig = Firebase.remoteConfig

    override fun getBoolean(flag: FeatureFlag): Boolean {
        return try {
            remoteConfig.getValue(flag.key).asBoolean()
        } catch (e: Exception) {
            flag.defaultValue as? Boolean ?: false
        }
    }

    override fun getString(flag: FeatureFlag): String {
        return try {
            remoteConfig.getValue(flag.key).asString()
        } catch (e: Exception) {
            flag.defaultValue as? String ?: ""
        }
    }

    override fun getLong(flag: FeatureFlag): Long {
        return try {
            remoteConfig.getValue(flag.key).asLong()
        } catch (e: Exception) {
            (flag.defaultValue as? Number)?.toLong() ?: 0L
        }
    }

    override fun getDouble(flag: FeatureFlag): Double {
        return try {
            remoteConfig.getValue(flag.key).asDouble()
        } catch (e: Exception) {
            (flag.defaultValue as? Number)?.toDouble() ?: 0.0
        }
    }

    override fun observeBoolean(flag: FeatureFlag): Flow<Boolean> {
        // Firebase Remote Config doesn't natively support Flow observation.
        // Return current value; updates happen after fetchAndActivate().
        return flowOf(getBoolean(flag))
    }

    override fun observeString(flag: FeatureFlag): Flow<String> {
        return flowOf(getString(flag))
    }

    override suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate()
        } catch (e: Exception) {
            false
        }
    }

    override fun setDefaults(defaults: Map<String, Any>) {
        // GitLive Firebase Remote Config handles defaults via Firebase console
        // or can be set via remoteConfig.setDefaults() if needed
    }
}
