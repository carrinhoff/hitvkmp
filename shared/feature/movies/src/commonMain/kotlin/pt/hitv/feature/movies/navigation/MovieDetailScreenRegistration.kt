package pt.hitv.feature.movies.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.MovieDetailArgs
import pt.hitv.core.navigation.ScreenRegistry

fun registerMovieDetailScreen() {
    ScreenRegistry.register(HitvScreen.MOVIE_DETAIL) { args ->
        MovieDetailVoyagerScreen(args as MovieDetailArgs)
    }
}
