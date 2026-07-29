package pt.hitv.core.data.paging

/**
 * Constants for filtering movies in paging queries.
 * These constants define special filter types for the Movie paging source.
 */
const val MOVIE_FILTER_FAVORITES = "Favorites"
const val MOVIE_FILTER_RECENTLY_VIEWED = "RecentlyViewed"
const val MOVIE_FILTER_LAST_ADDED = "LastAdded"
const val MOVIE_FILTER_TOP_RATED = "TopRated"
const val MOVIE_FILTER_CONTINUE_WATCHING = "ContinueWatching"
const val MOVIE_FILTER_4K = "4K"
const val MOVIE_FILTER_ALL = "All"
// Sort Constants
const val SORT_ADDED = "added"
const val SORT_NAME = "name"
const val SORT_RATING = "rating"
const val SORT_YEAR = "year"

/**
 * Constants for filtering TV shows in paging queries.
 *
 * **These deliberately hold the same string values as the `MOVIE_FILTER_*` set above**, and the
 * repositories branch on whichever name they happened to import. That aliasing is load-bearing:
 * `SeriesScreen` passes [FILTER_CONTINUE_WATCHING] while `TvShowRepositoryImpl` matches on
 * `MOVIE_FILTER_CONTINUE_WATCHING`, and it only works because the values are identical.
 *
 * It also hid a bug: the series repository had no Continue Watching branch at all, so "See All" on
 * that row fell through to a category lookup and opened an empty screen, while the movie side
 * worked. `PagingFilterConstantsTest` now pins the two sets together so drift between them fails
 * loudly instead of silently emptying a screen. Collapsing them into one set is the real fix and
 * touches every call site, so it is left as a follow-up.
 */
const val FILTER_FAVORITES = "Favorites"
const val FILTER_RECENTLY_VIEWED = "RecentlyViewed"
const val FILTER_LAST_ADDED = "LastAdded"
const val FILTER_TOP_RATED = "TopRated"
const val FILTER_CONTINUE_WATCHING = "ContinueWatching"
const val FILTER_ALL = "All"

/**
 * Constants for filtering channels in paging queries.
 * These constants define special filter types for the Channel paging source.
 */
const val CHANNEL_FILTER_FAVORITES = "Favorites"
const val CHANNEL_FILTER_RECENTLY_VIEWED = "RecentlyViewed"
const val CHANNEL_FILTER_LAST_ADDED = "LastAdded"
const val CHANNEL_FILTER_ALL = "All"
const val CHANNEL_FILTER_CATCH_UP = "CatchUp"
const val CHANNEL_FILTER_CUSTOM_GROUP_PREFIX = "custom_group_"
