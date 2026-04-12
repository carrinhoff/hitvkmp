package pt.hitv.feature.movies.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.navigation.navigateToMovieDetail
import pt.hitv.core.navigation.navigateToMovieCategoryDetail
import pt.hitv.feature.movies.list.MovieViewModel
import pt.hitv.feature.movies.ui.MoviesScreen

class MoviesVoyagerScreen : Screen {
    override val key = "Movies"

    @Composable
    override fun Content() {
        val viewModel: MovieViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        MoviesScreen(
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            onMovieClicked = { movie, _, clickType ->
                when (clickType) {
                    ClickType.CLICK -> navigator.navigateToMovieDetail(movie.streamId)
                    ClickType.LONG_CLICK -> viewModel.saveFavoriteMovie(movie, false)
                }
            },
            onNavigateToCategory = { categoryId, categoryName ->
                navigator.navigateToMovieCategoryDetail(categoryId, categoryName)
            }
        )
    }
}
