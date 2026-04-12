package pt.hitv.feature.movies.detail

import androidx.compose.runtime.Composable
import pt.hitv.core.model.cast.Cast
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import pt.hitv.feature.movies.detail.mobile.LandscapeMovieInfo
import pt.hitv.feature.movies.detail.mobile.PortraitMovieInfo

/**
 * Movie Info Screen Router
 *
 * Routes to the appropriate layout based on orientation:
 * - Landscape/Tablet: Uses LandscapeMovieInfo (cinematic split layout)
 * - Portrait: Uses PortraitMovieInfo (vertical scrolling layout)
 */
@Composable
fun MovieInfoScreen(
    movieInfo: Info,
    movieData: MovieData,
    castList: List<Cast>,
    savedPosition: Long? = null,
    isFavorite: Boolean = false,
    isLandscape: Boolean = false,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onTrailerClick: () -> Unit
) {
    if (isLandscape) {
        LandscapeMovieInfo(
            movieInfo = movieInfo,
            movieData = movieData,
            castList = castList,
            savedPosition = savedPosition,
            onBackClick = onBackClick,
            onPlayClick = onPlayClick,
            onTrailerClick = onTrailerClick
        )
    } else {
        PortraitMovieInfo(
            movieInfo = movieInfo,
            movieData = movieData,
            castList = castList,
            savedPosition = savedPosition,
            onBackClick = onBackClick,
            onPlayClick = onPlayClick,
            onTrailerClick = onTrailerClick
        )
    }
}
