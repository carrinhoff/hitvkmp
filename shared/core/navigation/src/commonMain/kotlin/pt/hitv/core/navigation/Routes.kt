package pt.hitv.core.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation argument data classes for Voyager screen navigation.
 * These replace the @Serializable route objects from Navigation Compose.
 */

@Serializable
data class MovieDetailArgs(val streamId: String?)

@Serializable
data class SeriesDetailArgs(val seriesId: String)

@Serializable
data class MovieCategoryDetailArgs(val categoryId: String, val categoryName: String)

@Serializable
data class SeriesCategoryDetailArgs(val categoryId: String, val categoryName: String)

@Serializable
data class ChannelPlayerArgs(
    val url: String,
    val name: String = "",
    val titleEpg: String? = null,
    val descEpg: String? = null,
    val logoUrl: String? = null,
    val licenseKey: String? = null
)

@Serializable
data class MoviePlayerArgs(val movieUrl: String, val movieTitle: String)

@Serializable
data class SeriesPlayerArgs(val episodeIndex: Int = 0)

@Serializable
data class YoutubePlayerArgs(val youtubeTrailer: String)

/**
 * Screen identifiers for Voyager navigation.
 * Each enum value corresponds to a distinct screen in the app.
 */
enum class HitvScreen {
    LOGIN,
    SWITCH_ACCOUNT,
    CHANNELS,
    MOVIES,
    SERIES,
    MOVIE_DETAIL,
    SERIES_DETAIL,
    MOVIE_CATEGORY,
    SERIES_CATEGORY,
    CHANNEL_PLAYER,
    MOVIE_PLAYER,
    SERIES_PLAYER,
    YOUTUBE_PLAYER,
    SETTINGS,
    MORE_OPTIONS,
    THEME_SETTINGS,
    PARENTAL_CONTROL,
    PARENTAL_PIN_SETUP,
    PARENTAL_CATEGORY_LOCK,
    MANAGE_CATEGORIES,
    BACKGROUND_SYNC_SETTINGS,
    TIPS_AND_FEATURES,
    ABOUT,
    FEEDBACK,
    LIVE_EPG,
    PREMIUM_SUBSCRIPTION
}
