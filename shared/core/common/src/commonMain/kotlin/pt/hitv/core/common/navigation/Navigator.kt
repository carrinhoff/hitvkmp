package pt.hitv.core.common.navigation

/**
 * Navigator interface for decoupling feature-to-feature navigation.
 *
 * This interface allows feature modules to navigate to other features
 * without direct dependencies. The implementation lives in the app module
 * where the actual navigation graph is defined.
 */
interface Navigator {

    // ==================== Player Navigation ====================

    fun navigateToChannelPlayer(channelId: Int, channelName: String)

    fun navigateToMoviePlayer(movieId: String, movieName: String, containerExtension: String?)

    fun navigateToSeriesPlayer(
        episodeId: String,
        episodeName: String,
        containerExtension: String?,
        seriesId: Int
    )

    // ==================== Detail Navigation ====================

    fun navigateToMovieDetail(movieId: String)

    fun navigateToSeriesDetail(seriesId: Int)

    fun navigateToChannelDetail(channelId: Int)

    // ==================== List/Browse Navigation ====================

    fun navigateToMovieCategory(categoryId: String, categoryName: String)

    fun navigateToSeriesCategory(categoryId: String, categoryName: String)

    fun navigateToChannelCategory(categoryId: String, categoryName: String)

    // ==================== Settings Navigation ====================

    fun navigateToSettings()

    fun navigateToPremium()

    fun navigateToParentalControl()

    fun navigateToManageCategories(contentType: String? = null)

    fun navigateToCustomGroups()

    // ==================== Auth Navigation ====================

    fun navigateToLogin(clearBackStack: Boolean = false)

    fun navigateToQrPairing()

    // ==================== Utility ====================

    fun navigateBack(): Boolean

    fun navigateToHome(clearBackStack: Boolean = false)
}

/**
 * No-op implementation of Navigator for use in previews and tests.
 */
object NoOpNavigator : Navigator {
    override fun navigateToChannelPlayer(channelId: Int, channelName: String) {}
    override fun navigateToMoviePlayer(movieId: String, movieName: String, containerExtension: String?) {}
    override fun navigateToSeriesPlayer(episodeId: String, episodeName: String, containerExtension: String?, seriesId: Int) {}
    override fun navigateToMovieDetail(movieId: String) {}
    override fun navigateToSeriesDetail(seriesId: Int) {}
    override fun navigateToChannelDetail(channelId: Int) {}
    override fun navigateToMovieCategory(categoryId: String, categoryName: String) {}
    override fun navigateToSeriesCategory(categoryId: String, categoryName: String) {}
    override fun navigateToChannelCategory(categoryId: String, categoryName: String) {}
    override fun navigateToSettings() {}
    override fun navigateToPremium() {}
    override fun navigateToParentalControl() {}
    override fun navigateToManageCategories(contentType: String?) {}
    override fun navigateToCustomGroups() {}
    override fun navigateToLogin(clearBackStack: Boolean) {}
    override fun navigateToQrPairing() {}
    override fun navigateBack(): Boolean = false
    override fun navigateToHome(clearBackStack: Boolean) {}
}
