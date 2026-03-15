package pt.hitv.core.model.movieInfo

/**
 * Domain model for cached movie information.
 * Combines movie data and detailed info, replacing direct use of
 * the database relation class in UI/feature code.
 */
data class CachedMovieInfo(
    val movieData: MovieData,
    val info: Info
)
