package pt.hitv.feature.movies.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.MovieCategoryDetailArgs
import pt.hitv.core.navigation.ScreenRegistry

fun registerMoviesScreens() {
    ScreenRegistry.register(HitvScreen.MOVIES) { MoviesVoyagerScreen() }
    registerMovieDetailScreen()
    ScreenRegistry.register(HitvScreen.MOVIE_CATEGORY) { args ->
        MovieCategoryDetailVoyagerScreen(args as MovieCategoryDetailArgs)
    }
}
