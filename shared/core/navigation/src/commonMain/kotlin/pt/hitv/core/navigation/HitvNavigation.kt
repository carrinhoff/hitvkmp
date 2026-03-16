package pt.hitv.core.navigation

import cafe.adriel.voyager.navigator.Navigator

/**
 * Extension functions on Voyager Navigator for typed navigation.
 *
 * These provide a clean API for navigating between screens with proper arguments,
 * replacing the old Navigation Compose route-based navigation.
 *
 * Usage:
 * ```
 * navigator.navigateToMovieDetail(streamId = "123")
 * navigator.navigateToChannelPlayer(url = "http://...", name = "Channel 1")
 * ```
 *
 * Note: The actual Screen implementations are provided by feature modules.
 * This file defines the navigation contract; feature modules register their
 * screen factories via a ScreenRegistry pattern.
 */

/**
 * Registry for creating Voyager screens from navigation arguments.
 * Feature modules register their screen factories at app startup.
 */
object ScreenRegistry {
    private val factories = mutableMapOf<HitvScreen, (Any?) -> cafe.adriel.voyager.core.screen.Screen>()

    fun register(screen: HitvScreen, factory: (Any?) -> cafe.adriel.voyager.core.screen.Screen) {
        factories[screen] = factory
    }

    fun create(screen: HitvScreen, args: Any? = null): cafe.adriel.voyager.core.screen.Screen {
        return factories[screen]?.invoke(args)
            ?: error("No screen factory registered for $screen")
    }
}

// ===== Detail Navigation =====

fun Navigator.navigateToMovieDetail(streamId: String?) {
    push(ScreenRegistry.create(HitvScreen.MOVIE_DETAIL, MovieDetailArgs(streamId)))
}

fun Navigator.navigateToSeriesDetail(seriesId: String) {
    push(ScreenRegistry.create(HitvScreen.SERIES_DETAIL, SeriesDetailArgs(seriesId)))
}

fun Navigator.navigateToMovieCategoryDetail(categoryId: String, categoryName: String) {
    push(ScreenRegistry.create(HitvScreen.MOVIE_CATEGORY, MovieCategoryDetailArgs(categoryId, categoryName)))
}

fun Navigator.navigateToSeriesCategoryDetail(categoryId: String, categoryName: String) {
    push(ScreenRegistry.create(HitvScreen.SERIES_CATEGORY, SeriesCategoryDetailArgs(categoryId, categoryName)))
}

// ===== Player Navigation =====

fun Navigator.navigateToChannelPlayer(
    url: String,
    name: String = "",
    titleEpg: String? = null,
    descEpg: String? = null,
    logoUrl: String? = null,
    licenseKey: String? = null
) {
    push(ScreenRegistry.create(
        HitvScreen.CHANNEL_PLAYER,
        ChannelPlayerArgs(url, name, titleEpg, descEpg, logoUrl, licenseKey)
    ))
}

fun Navigator.navigateToMoviePlayer(movieUrl: String, movieTitle: String) {
    push(ScreenRegistry.create(HitvScreen.MOVIE_PLAYER, MoviePlayerArgs(movieUrl, movieTitle)))
}

fun Navigator.navigateToSeriesPlayer(episodeIndex: Int = 0) {
    push(ScreenRegistry.create(HitvScreen.SERIES_PLAYER, SeriesPlayerArgs(episodeIndex)))
}

fun Navigator.navigateToYoutubePlayer(youtubeTrailer: String) {
    push(ScreenRegistry.create(HitvScreen.YOUTUBE_PLAYER, YoutubePlayerArgs(youtubeTrailer)))
}

// ===== Content Navigation =====

fun Navigator.navigateToChannels() {
    push(ScreenRegistry.create(HitvScreen.CHANNELS))
}

fun Navigator.navigateToMovies() {
    push(ScreenRegistry.create(HitvScreen.MOVIES))
}

fun Navigator.navigateToSeries() {
    push(ScreenRegistry.create(HitvScreen.SERIES))
}

// ===== Auth Navigation =====

fun Navigator.navigateToLogin() {
    push(ScreenRegistry.create(HitvScreen.LOGIN))
}

fun Navigator.navigateToSwitchAccount() {
    push(ScreenRegistry.create(HitvScreen.SWITCH_ACCOUNT))
}

// ===== Settings Navigation =====

fun Navigator.navigateToSettings() {
    push(ScreenRegistry.create(HitvScreen.SETTINGS))
}

fun Navigator.navigateToMoreOptions() {
    push(ScreenRegistry.create(HitvScreen.MORE_OPTIONS))
}

fun Navigator.navigateToThemeSettings() {
    push(ScreenRegistry.create(HitvScreen.THEME_SETTINGS))
}

fun Navigator.navigateToParentalControl() {
    push(ScreenRegistry.create(HitvScreen.PARENTAL_CONTROL))
}

fun Navigator.navigateToManageCategories() {
    push(ScreenRegistry.create(HitvScreen.MANAGE_CATEGORIES))
}

fun Navigator.navigateToFeedback() {
    push(ScreenRegistry.create(HitvScreen.FEEDBACK))
}

// ===== Other Navigation =====

fun Navigator.navigateToLiveEpg() {
    push(ScreenRegistry.create(HitvScreen.LIVE_EPG))
}

fun Navigator.navigateToPremiumSubscription() {
    push(ScreenRegistry.create(HitvScreen.PREMIUM_SUBSCRIPTION))
}
