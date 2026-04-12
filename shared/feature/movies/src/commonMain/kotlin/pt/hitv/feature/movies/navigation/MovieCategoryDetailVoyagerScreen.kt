package pt.hitv.feature.movies.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.navigation.MovieCategoryDetailArgs
import pt.hitv.core.navigation.navigateToMovieDetail
import pt.hitv.feature.movies.detail.category.MovieCategoryDetailScreen
import pt.hitv.feature.movies.list.MovieViewModel

class MovieCategoryDetailVoyagerScreen(
    private val args: MovieCategoryDetailArgs
) : Screen {
    override val key = "MovieCategoryDetail_${args.categoryId}"

    @Composable
    override fun Content() {
        val viewModel: MovieViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        MovieCategoryDetailScreen(
            initialCategoryId = args.categoryId,
            initialCategoryName = args.categoryName,
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            onMovieClicked = { movie, position, clickType ->
                when (clickType) {
                    ClickType.CLICK -> {
                        viewModel.saveLastClickedItem(movie.streamId, position)
                        navigator.navigateToMovieDetail(movie.streamId)
                    }
                    ClickType.LONG_CLICK -> {
                        viewModel.saveFavoriteMovie(movie, isFavoritesFilterActive = false)
                    }
                }
            },
            onBackPressed = { navigator.pop() }
        )
    }
}
