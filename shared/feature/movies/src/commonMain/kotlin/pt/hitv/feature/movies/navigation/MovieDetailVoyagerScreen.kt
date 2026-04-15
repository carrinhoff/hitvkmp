package pt.hitv.feature.movies.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.navigation.MovieDetailArgs
import pt.hitv.feature.movies.detail.MovieInfoContent
import pt.hitv.feature.movies.detail.MovieInfoViewModel
import pt.hitv.feature.player.platform.launchMoviePlayer

class MovieDetailVoyagerScreen(
    private val streamId: String?
) : Screen {
    constructor(args: MovieDetailArgs) : this(args.streamId)

    override val key = "MovieDetail_$streamId"

    @Composable
    override fun Content() {
        val viewModel: MovieInfoViewModel = koinInject()
        val preferencesHelper: PreferencesHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        MovieInfoContent(
            streamId = streamId,
            viewModel = viewModel,
            preferencesHelper = preferencesHelper,
            onNavigateBack = { navigator.pop() },
            onPlayMovie = { movieUrl, movieTitle ->
                launchMoviePlayer(movieUrl, movieTitle)
            },
            onPlayTrailer = { youtubeUrl ->
                try {
                    uriHandler.openUri("https://www.youtube.com/watch?v=$youtubeUrl")
                } catch (_: Exception) {}
            }
        )
    }
}
