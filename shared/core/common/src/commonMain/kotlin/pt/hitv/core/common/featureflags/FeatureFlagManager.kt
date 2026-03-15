package pt.hitv.core.common.featureflags

import kotlinx.coroutines.flow.Flow

/**
 * Interface for feature flag management that abstracts away the specific implementation.
 * Feature modules can use this interface without depending on Firebase Remote Config directly.
 */
interface FeatureFlagManager {

    fun getBoolean(flag: FeatureFlag): Boolean

    fun getString(flag: FeatureFlag): String

    fun getLong(flag: FeatureFlag): Long

    fun getDouble(flag: FeatureFlag): Double

    fun observeBoolean(flag: FeatureFlag): Flow<Boolean>

    fun observeString(flag: FeatureFlag): Flow<String>

    /**
     * Fetch and activate the latest feature flag values from the server.
     *
     * @return true if fetch and activate was successful, false otherwise
     */
    suspend fun fetchAndActivate(): Boolean

    /**
     * Set default values for feature flags.
     *
     * @param defaults Map of flag keys to default values
     */
    fun setDefaults(defaults: Map<String, Any>)
}

/**
 * Enum defining all available feature flags in the app.
 */
enum class FeatureFlag(
    val key: String,
    val defaultValue: Any
) {
    // Premium Features
    PREMIUM_ENABLED("premium_enabled", true),
    TRIAL_OFFER_ENABLED("trial_offer_enabled", true),
    TRIAL_DAYS("trial_days", 7L),

    // Player Features
    NEW_PLAYER_UI_ENABLED("new_player_ui_enabled", false),
    PICTURE_IN_PICTURE_ENABLED("pip_enabled", true),
    CHROMECAST_ENABLED("chromecast_enabled", true),

    // UI Features
    DARK_MODE_ONLY("dark_mode_only", false),
    SHOW_RATINGS("show_ratings", true),
    EPG_ENABLED("epg_enabled", true),

    // Content Features
    MOVIES_ENABLED("movies_enabled", true),
    SERIES_ENABLED("series_enabled", true),
    LIVE_TV_ENABLED("live_tv_enabled", true),

    // Performance Tuning
    IMAGE_CACHE_SIZE_MB("image_cache_size_mb", 100L),
    PAGING_PAGE_SIZE("paging_page_size", 30L),
    PREFETCH_DISTANCE("prefetch_distance", 10L),

    // Maintenance
    MAINTENANCE_MODE("maintenance_mode", false),
    MAINTENANCE_MESSAGE("maintenance_message", ""),
    MIN_APP_VERSION("min_app_version", "1.0.0"),

    // A/B Testing
    AB_TEST_VARIANT("ab_test_variant", "control")
}
